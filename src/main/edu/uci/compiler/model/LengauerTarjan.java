package main.edu.uci.compiler.model;

/**
 * Created by raghugudipati on 2/23/17.
 */
import com.sun.tools.doclets.formats.html.SourceToHTMLConverter;
import org.javatuples.Pair;
import com.google.common.collect.Lists;

import java.util.*;

public class LengauerTarjan {

    private HashMap<Integer,Set<Integer>> domTreeLinks = new HashMap<Integer,Set<Integer>>();
    private Set<Integer> dominatorSet = new HashSet<Integer>();

    public int depthFirstSearch(BasicBlock parent,BasicBlock root, HashMap<BasicBlock,Integer> dfnums, HashMap<Integer, BasicBlock> vertex, HashMap<BasicBlock,BasicBlock> parentList, int dfnum){
        parentList.put(root,parent);
        vertex.put(dfnum,root);
        dfnums.put(root,dfnum);
        dfnum++;


        Stack<Pair<BasicBlock,BasicBlock>> nodeStack = new Stack<Pair<BasicBlock, BasicBlock>>();
        for(BasicBlock child : Lists.reverse(root.getChildren())){
            nodeStack.push(new Pair<BasicBlock,BasicBlock>(child,root));
        }
        while(!nodeStack.empty()){
            Pair<BasicBlock,BasicBlock> currentPointer = nodeStack.pop();
            BasicBlock currentBlock = currentPointer.getValue0();
            BasicBlock parentBlock = currentPointer.getValue1();
            if(dfnums.get(currentBlock) == -1){
                dfnums.put(currentBlock,dfnum);
                vertex.put(dfnum,currentBlock);
                parentList.put(currentBlock,parentBlock);

                dfnum++;

                for(BasicBlock child : Lists.reverse(currentBlock.getChildren())){
                    nodeStack.push(new Pair<BasicBlock,BasicBlock>(child,currentBlock));
                }
            }
        }

        return dfnum -1 ;
    }
    public BasicBlock getAncestorWithLowestSemi(BasicBlock basicBlock ,HashMap<BasicBlock, Integer> dfnum, HashMap<BasicBlock, BasicBlock> semi,  HashMap<BasicBlock, BasicBlock> ancestor, HashMap<BasicBlock, BasicBlock> best){

        BasicBlock a = ancestor.get(basicBlock);
        if(ancestor.get(a)!=null){
            BasicBlock b = getAncestorWithLowestSemi(a,dfnum,semi,ancestor,best);
            System.out.println("b is " + b.getId());
            ancestor.put(basicBlock,ancestor.get(a));

            if (dfnum.get(semi.get(b)) < dfnum.get(semi.get(best.get(basicBlock)))){
                best.put(basicBlock,b);
            }
        }
        return best.get(basicBlock);
    }
    public void link(BasicBlock parent, BasicBlock node , HashMap<BasicBlock, BasicBlock> ancestor, HashMap<BasicBlock, BasicBlock> best){
        System.out.println("in Link node is : " + node.getId() + "Parent is " + parent.getId());
        ancestor.put(node,parent);
        best.put(node,node);
    }
    public HashMap<Integer,Set<Integer>> computeDominators(BasicBlock rootBlock , LinkedList<BasicBlock> allNodes){
        allNodes = rootBlock.getListOfAllBasicBlocks();


        HashMap<BasicBlock,Set<BasicBlock>> bucket = new HashMap<BasicBlock,Set<BasicBlock>>();
        HashMap<BasicBlock,BasicBlock> idom = new HashMap<BasicBlock,BasicBlock>();
        HashMap<BasicBlock,BasicBlock> best = new HashMap<BasicBlock,BasicBlock>();
        HashMap<BasicBlock,BasicBlock> semi = new HashMap<BasicBlock,BasicBlock>();
        HashMap<BasicBlock,BasicBlock> ancestor = new HashMap<BasicBlock,BasicBlock>();
        HashMap<BasicBlock,BasicBlock> samedom = new HashMap<BasicBlock,BasicBlock>();
        HashMap<Integer,BasicBlock> vertex = new HashMap<Integer,BasicBlock>();
        HashMap<BasicBlock,BasicBlock> parents = new HashMap<BasicBlock,BasicBlock>();
        HashMap<BasicBlock,Integer> dfnum = new HashMap<BasicBlock,Integer>();

        for( BasicBlock node : allNodes){
            bucket.put(node, new HashSet<BasicBlock>());
            dfnum.put(node, -1);
            semi.put(node, null);
            ancestor.put(node, null);
            idom.put(node, null);
            samedom.put(node, null);
        }
        int n = depthFirstSearch(null , rootBlock, dfnum,vertex,parents,0);

        System.out.println("n is : " + n);

        for (Map.Entry<Integer,BasicBlock> entry : vertex.entrySet()){
            System.out.println("[" + entry.getKey() + "]" + ": " + "{" + entry.getValue() + "}");
        }

        for(int i = n; i >= 1; i--){
            System.out.println("Iteration : " + i);
            BasicBlock node = vertex.get(i);
            BasicBlock parent = parents.get(node);
            BasicBlock s = parent;
            System.out.println("node is " + node.getId());
            System.out.println("parent is " + parent.getId());
            //System.out.println();

            for (BasicBlock v : node.getParent()){
                System.out.println(" v is " + v.getId());
                BasicBlock temp = null;
                if(dfnum.get(v) <= dfnum.get(node)){
                    temp = v;
                } else {
                    temp = semi.get(getAncestorWithLowestSemi(v,dfnum,semi,ancestor,best));
                }
                if(dfnum.get(temp) < dfnum.get(s)){
                    s = temp;
                }
            }
            semi.put(node,s);
            bucket.get(s).add(node);
            link(parent , node , ancestor , best);

            for( BasicBlock v : bucket.get(parent)){
                BasicBlock y = getAncestorWithLowestSemi(v ,dfnum,semi,ancestor,best);
                if(semi.get(y) == semi.get(v)){
                    idom.put(v,parent);
                } else{
                    samedom.put(v,y);
                }
            }
            bucket.get(parent).clear();
        }
        for(int i = 1; i<=n;i++){
            BasicBlock node = vertex.get(i);
            if(samedom.get(node) != null){
                idom.put(node , idom.get(samedom.get(node)));
            }
        }









        dominatorSet.clear();


        for(Map.Entry<BasicBlock,BasicBlock> node : idom.entrySet()){
            BasicBlock dominatorNode = node.getValue();
            BasicBlock dominatedNode = node.getKey();
            System.out.println("Dominator Node is " + dominatedNode);
            System.out.println();
            if(dominatorNode != null) {
                if (!domTreeLinks.containsKey(dominatorNode.getId())) {
                    Set<Integer> dominatedSet = new HashSet<Integer>();
                    dominatedSet.add(dominatedNode.getId());
                    domTreeLinks.put(dominatorNode.getId(),dominatedSet);
                }
                else{
                    dominatorSet = domTreeLinks.get(dominatorNode.getId());
                    dominatorSet.add(dominatedNode.getId());
                    domTreeLinks.put(dominatorNode.getId(),dominatorSet);
                }
            }else{
                int domId = 0;
                if(!domTreeLinks.containsKey(domId)) {
                    Set<Integer> dominatedSet = new HashSet<Integer>();
                    dominatedSet.add(dominatedNode.getId());
                    domTreeLinks.put(domId,dominatedSet);
                }
                else{
                    dominatorSet = domTreeLinks.get(domId);
                    dominatorSet.add(dominatedNode.getId());
                    domTreeLinks.put(domId,dominatorSet);
                }
            }
        }
        System.out.println();
        for(Map.Entry<Integer,Set<Integer>> entry : domTreeLinks.entrySet()){
            int block = entry.getKey();
            Set<Integer> blocksDominated = entry.getValue();
            System.out.print("[" + block + "]: {");
            for (Integer blockDominated : blocksDominated) {
                System.out.print("[" + blockDominated + "]");

            }
            System.out.print("}\n");
        }


        return domTreeLinks;

    }
    public void printTreeLinks(BasicBlock rootBlock , LinkedList<BasicBlock> allNodes){

        HashMap<Integer,Set<Integer>> finalTreeLinks = new HashMap<Integer,Set<Integer>>();

            finalTreeLinks = computeDominators(rootBlock,allNodes);


        System.out.println();
        for(Map.Entry<Integer,Set<Integer>> entry : finalTreeLinks.entrySet()){
            int block = entry.getKey();
            Set<Integer> blocksDominated = entry.getValue();
            System.out.print("[" + block + "]: {");
            for (Integer blockDominated : blocksDominated) {
                System.out.print("[" + blockDominated + "]");

            }
            System.out.print("}\n");
        }

    }


}
