package main.edu.uci.compiler.model;

/**
 * Created by raghugudipati on 2/28/17.
 */




import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;




public class LTDominatorTree {
    /**
     * Construct a DominatorTree from a root.
     *
     * @param root the root of the graph.
     */
    public LTDominatorTree(BasicBlock root) {
        Collection<BasicBlock> roots = new ArrayList<BasicBlock>();
        roots.add(root);
        this.dfs(roots);
        this.computeDominators();
//        this.printLTDomTree();
    }

    /**
     * Construct a DominatorTree from a collection of "roots."
     *
     * @param roots the collection of roots; one of these is
     *              the true root of the flowgraph, the others are exception
     *              handlers that would otherwise be unreachable.
     */
    public LTDominatorTree(Collection<? extends BasicBlock> roots) {
        this.dfs(roots);
        this.computeDominators();
    }

    /**
     * Semidominator numbers by block.
     */
    private Map<BasicBlock, Integer> semi = new HashMap<BasicBlock, Integer>();

    /**
     * Parents by block.
     */
    private Map<BasicBlock, BasicBlock> parent = new HashMap<BasicBlock, BasicBlock>();

    /**
     * Predecessors by block.
     */
    private Multimap<BasicBlock> pred = new Multimap<BasicBlock>();

    /**
     * Blocks in DFS order; used to look up a block from its semidominator
     * numbering.
     */
    private ArrayList<BasicBlock> vertex = new ArrayList<BasicBlock>();

    /**
     * Blocks by semidominator block.
     */
    private Multimap<BasicBlock> bucket = new Multimap<BasicBlock>();

    /**
     * idominator map, built iteratively.
     */
    private Map<BasicBlock, BasicBlock> idom = new HashMap<BasicBlock, BasicBlock>();

    /**
     * Dominance frontiers of this dominator tree, built on demand.
     */
    private Multimap<BasicBlock> dominanceFrontiers = null;

    /**
     * Dominator tree, built on demand from the idominator map.
     */
    private Multimap<BasicBlock> dominatorTree = null;

    /**
     * Auxiliary data structure used by the O(m log n) eval/link implementation:
     * ancestor relationships in the forest (the processed tree as it's built
     * back up).
     */
    private Map<BasicBlock, BasicBlock> ancestor = new HashMap<BasicBlock, BasicBlock>();

    /**
     * Auxiliary data structure used by the O(m log n) eval/link implementation:
     * node with least semidominator seen during traversal of a path from node
     * to subtree root in the forest.
     */
    private Map<BasicBlock, BasicBlock> label = new HashMap<BasicBlock, BasicBlock>();

    /**
     * A topological traversal of the dominator tree, built on demand.
     */
    private LinkedList<BasicBlock> topologicalTraversalImpl = null;

    /**
     * Create and/or fetch the map of immediate dominators.
     *
     * @return the map from each block to its immediate dominator
     * (if it has one).
     */
    public Map<BasicBlock, BasicBlock> getIdoms() {
        return this.idom;
    }

    /**
     * Compute and/or fetch the dominator tree as a Multimap.
     *
     * @return the dominator tree.
     */
    public Multimap<BasicBlock> getDominatorTree() {
        if (this.dominatorTree == null) {
            this.dominatorTree = new Multimap<BasicBlock>();

            for (BasicBlock node : this.idom.keySet())
                dominatorTree.get(this.idom.get(node)).add(node);
        }

        return this.dominatorTree;
    }




    /**
     * Depth-first search the graph and initialize data structures.
     *
     * @param roots the root(s) of the flowgraph.  One of these is
     *              the start block, the others are exception handlers.
     */
    private void dfs(Collection<? extends BasicBlock> roots) {
        Iterator<BasicBlock> it = new DepthFirstPreorderIterator(roots);

        while (it.hasNext()) {
            BasicBlock node = it.next();

            if (!semi.containsKey(node)) {
                vertex.add(node);

                //  Initial assumption: the node's semidominator is itself.
                semi.put(node, semi.size());
                label.put(node, node);

                for (BasicBlock child : node.getChildren()) {
                    pred.get(child).add(node);
                    if (!semi.containsKey(child)) {
                        parent.put(child, node);
                    }
                }
            }
        }
    }

    /**
     * Steps 2, 3, and 4 of Lengauer-Tarjan.
     */
    private void computeDominators() {
        int lastSemiNumber = semi.size() - 1;

        for (int i = lastSemiNumber; i > 0; i--) {
            BasicBlock w = vertex.get(i);
            BasicBlock p = this.parent.get(w);

            //  step 2: compute semidominators
            //  for each v in pred(w)...
            int semidominator = semi.get(w);
            for (BasicBlock v : pred.get(w))
                semidominator = Math.min(semidominator, semi.get(eval(v)));

            semi.put(w, semidominator);
            bucket.get(vertex.get(semidominator)).add(w);

            //  Link w into the forest via its parent, p
            link(p, w);

            //  step 3: implicitly compute idominators
            //  for each v in bucket(parent(w)) ...
            for (BasicBlock v : bucket.get(p)) {
                BasicBlock u = eval(v);

                if (semi.get(u) < semi.get(v))
                    idom.put(v, u);
                else
                    idom.put(v, p);
            }

            bucket.get(p).clear();
        }

        // step 4: explicitly compute idominators
        for (int i = 1; i <= lastSemiNumber; i++) {
            BasicBlock w = vertex.get(i);

            if (idom.get(w) != vertex.get((semi.get(w))))
                idom.put(w, idom.get(idom.get(w)));
        }
    }

    /**
     * Extract the node with the least-numbered semidominator in the (processed)
     * ancestors of the given node.
     *
     * @param v - the node of interest.
     * @return "If v is the root of a tree in the forest, return v. Otherwise,
     * let r be the root of the tree which contains v. Return any vertex u != r
     * of miniumum semi(u) on the path r-*v."
     */
    private BasicBlock eval(BasicBlock v) {
        //  This version of Lengauer-Tarjan implements
        //  eval(v) as a path-compression procedure.
        compress(v);
        return label.get(v);
    }

    /**
     * Traverse ancestor pointers back to a subtree root, then propagate the
     * least semidominator seen along this path through the "label" map.
     */
    private void compress(BasicBlock v) {
        Stack<BasicBlock> worklist = new Stack<BasicBlock>();
        worklist.add(v);

        BasicBlock a = this.ancestor.get(v);

        //  Traverse back to the subtree root.
        while (this.ancestor.containsKey(a)) {
            worklist.push(a);
            a = this.ancestor.get(a);
        }

        //  Propagate semidominator information forward.
        BasicBlock ancestor = worklist.pop();
        int leastSemi = semi.get(label.get(ancestor));

        while (!worklist.empty()) {
            BasicBlock descendent = worklist.pop();
            int currentSemi = semi.get(label.get(descendent));

            if (currentSemi > leastSemi)
                label.put(descendent, label.get(ancestor));
            else
                leastSemi = currentSemi;

            //  Prepare to process the next iteration.
            ancestor = descendent;
        }
    }

    /**
     * Simple version of link(parent,child) simply links the child into the
     * parent's forest, with no attempt to balance the subtrees or otherwise
     * optimize searching.
     */
    private void link(BasicBlock parent, BasicBlock child) {
        this.ancestor.put(child, parent);
    }



    public void printLTDomTree(){
        Multimap<BasicBlock> Domtree = this.getDominatorTree();
        HashMap<BasicBlock,Set<BasicBlock>> dominatorSet = new HashMap<BasicBlock,Set<BasicBlock>>() ;
        for(Map.Entry<BasicBlock,Set<BasicBlock>> entry : Domtree.entrySet()){
            BasicBlock block = entry.getKey();


            Set<BasicBlock> blocksDominated = entry.getValue();
            System.out.print("[" + block.getId() + "]: {");
            for (BasicBlock blockDominated : blocksDominated) {
                System.out.print("[" + blockDominated.getId() + "]");

            }
            System.out.print("}\n");
        }
    }



}
