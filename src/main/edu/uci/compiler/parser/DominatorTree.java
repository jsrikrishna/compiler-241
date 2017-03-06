package main.edu.uci.compiler.parser;

import main.edu.uci.compiler.cfg.ControlFlowGraph;
import main.edu.uci.compiler.model.BasicBlock;
import main.edu.uci.compiler.model.DominatorBlock;
import main.edu.uci.compiler.model.Function;

import java.util.*;

/**
 * Created by srikrishna on 2/22/17.
 * Implements the naive algorithm to find the nodes dominated by a given basic block
 */
public class DominatorTree {
    private BasicBlock mainStartBasicBlock;
    private Set<BasicBlock> endBasicBlocks;
    private Set<BasicBlock> allRootBasicBlocks;
    private Set<DominatorBlock> allRootDominatorBlocks;
    private HashMap<String, Function> functions;
    private HashMap<BasicBlock, DominatorBlock> allDominatorBlocks;
    private HashSet<Map<BasicBlock, Set<BasicBlock>>> allDomRelationsInProgram;
    private HashMap<BasicBlock, BasicBlock> allDomParents;

    public DominatorTree(Set<DominatorBlock> allRootDominatorBlocks,
                         Set<BasicBlock> endBasicBlocks,
                         HashMap<BasicBlock, BasicBlock> allDomParents) {
        mainStartBasicBlock = null;
        functions = null;
        this.endBasicBlocks = endBasicBlocks;
        allRootBasicBlocks = new HashSet<>();
        this.allRootDominatorBlocks = allRootDominatorBlocks;
        this.allDomParents = allDomParents;
        allDomRelationsInProgram = new HashSet<>();
        allDominatorBlocks = new HashMap<>();
    }

    public void updateDomTree(BasicBlock mainStartBasicBlock, HashMap<String, Function> functions) {
        this.mainStartBasicBlock = mainStartBasicBlock;
        this.functions = functions;
    }

    private void findAllReachableBlocksExceptFromV(BasicBlock currentBlock,
                                                   BasicBlock v,
                                                   Set<BasicBlock> visitedBlocks) {

        if (!visitedBlocks.contains(currentBlock) && v.getId() != currentBlock.getId()) {
            visitedBlocks.add(currentBlock);
            for (BasicBlock children : currentBlock.getChildren())
                findAllReachableBlocksExceptFromV(children, v, visitedBlocks);
        }
    }

    private Set<BasicBlock> blocksDominatedByV(Set<BasicBlock> allBasicBlocks,
                                               BasicBlock root,
                                               BasicBlock v) {
        HashSet<BasicBlock> visitedBlocks = new HashSet<>();
        findAllReachableBlocksExceptFromV(root, v, visitedBlocks);
        HashSet<BasicBlock> allBasicBlocksSet = new HashSet<>(allBasicBlocks);
        allBasicBlocksSet.remove(v);
        allBasicBlocksSet.removeAll(visitedBlocks);
        return allBasicBlocksSet;
    }

    private Map<BasicBlock, Set<BasicBlock>> generateDomRelations(Set<BasicBlock> allBasicBlocks,
                                                                  BasicBlock root) {
        Map<BasicBlock, Set<BasicBlock>> dominatorRelationships = new HashMap<>();
        for (BasicBlock currentBlock : allBasicBlocks) {
            Set<BasicBlock> blocksDominatedByCurrentBlock = blocksDominatedByV(allBasicBlocks, root, currentBlock);
            dominatorRelationships.put(currentBlock, blocksDominatedByCurrentBlock);
            for(BasicBlock basicBlock: blocksDominatedByCurrentBlock){
                allDomParents.put(basicBlock, currentBlock);
            }
        }
        return dominatorRelationships;
    }

    private Set<BasicBlock> basicBlockBFS(BasicBlock startBasicBlock) {
        Set<BasicBlock> listOfAllBasicBlocks = new HashSet<>();
        Queue<BasicBlock> frontier = new LinkedList<>();
        frontier.add(startBasicBlock);
        while (!frontier.isEmpty()) {
            BasicBlock currentBasicBlock = frontier.poll();
            listOfAllBasicBlocks.add(currentBasicBlock);
            List<BasicBlock> children = currentBasicBlock.getChildren();
            if (children.isEmpty() || children == null) {
                endBasicBlocks.add(currentBasicBlock);
            }
            for (BasicBlock child : children) {
                if (!listOfAllBasicBlocks.contains(child)) frontier.add(child);
            }
        }
        return listOfAllBasicBlocks;
    }

    private void generateDomRelationsForProgram(BasicBlock mainStartBasicBlock,
                                                Set<Function> functions) {
        allRootBasicBlocks.add(mainStartBasicBlock);
        mainStartBasicBlock.setIsRootBasicBlock();
        Set<BasicBlock> mainBasicBlocks = basicBlockBFS(mainStartBasicBlock);
        Map<BasicBlock, Set<BasicBlock>> domRelations = generateDomRelations(mainBasicBlocks, mainStartBasicBlock);
        allDomParents.put(mainStartBasicBlock, null);
        generateTransitiveDomDependency(domRelations);
        allDomRelationsInProgram.add(domRelations);
        for (Function function : functions) {
            BasicBlock funcBasicBlock = function.getFuncBasicBlock();
            funcBasicBlock.setIsRootBasicBlock();
            allRootBasicBlocks.add(funcBasicBlock);
            allDomParents.put(funcBasicBlock, null);
            Set<BasicBlock> funcBasicBlocks = basicBlockBFS(funcBasicBlock);
            domRelations = generateDomRelations(funcBasicBlocks, funcBasicBlock);
            generateTransitiveDomDependency(domRelations);
            allDomRelationsInProgram.add(domRelations);
        }
    }

    /*
    Get Immediate Dominance Relationships
    Ex: what we have is 1 dom (2,3,4), [but actually 1 dom (2) and 2 dom (3,4)]
    So what we need is 1 dom (2) and 2 dom (3,4).
     */
    private void generateTransitiveDomDependency(Map<BasicBlock, Set<BasicBlock>> domRelationships) {
        for (Map.Entry<BasicBlock, Set<BasicBlock>> entry : domRelationships.entrySet()) {
            Set<BasicBlock> dominanceChilds = entry.getValue();
            Set<BasicBlock> childrenOfChildren = new HashSet<>();
            for (BasicBlock child : dominanceChilds) {
                childrenOfChildren.addAll(domRelationships.get(child));
            }
            dominanceChilds.removeAll(childrenOfChildren);
        }

    }

    private void printBlockDomRelations(Map<BasicBlock, Set<BasicBlock>> domRelations) {
        for (Map.Entry<BasicBlock, Set<BasicBlock>> entry : domRelations.entrySet()) {
            BasicBlock basicBlock = entry.getKey();
            Set<BasicBlock> blocksDominated = entry.getValue();
            System.out.print("[" + basicBlock.getId() + "]: {");
            for (BasicBlock blockDominated : blocksDominated) {
                System.out.print("[" + blockDominated.getId() + "]");

            }
            System.out.print("}\n");
        }
    }

    private BasicBlock getRootBasicBlock(Map<BasicBlock, Set<BasicBlock>> domRelations) {
        for (Map.Entry<BasicBlock, Set<BasicBlock>> entry : domRelations.entrySet()) {
            if (allRootBasicBlocks.contains(entry.getKey())) return entry.getKey();
        }
        System.err.println("Root Not Found");
        return null;
    }

    private DominatorBlock generateDomTreeForRoot(BasicBlock rootBasicBlock,
                                                  Map<BasicBlock, Set<BasicBlock>> domRelations) {
        DominatorBlock rootDominatorBlock = new DominatorBlock(rootBasicBlock);
        DominatorBlock dominatorBlock = rootDominatorBlock;

        Queue<DominatorBlock> frontier = new LinkedList<>();
        // There wont be any cycles as it is a tree, so not keeping visited set of dom blocks
        frontier.add(dominatorBlock);

        while (!frontier.isEmpty()) {
            dominatorBlock = frontier.poll();
            allDominatorBlocks.put(dominatorBlock.getMyBasicBlock(), dominatorBlock);
            Set<BasicBlock> domChildrens = domRelations.get(dominatorBlock.getMyBasicBlock());

            for (BasicBlock domChildren : domChildrens) {
                DominatorBlock domBlockChildren = new DominatorBlock(domChildren);

                // Establish dom-block relationships
                dominatorBlock.addChildren(domBlockChildren);
//                domBlockChildren.addParent(dominatorBlock);
                domBlockChildren.setParent(dominatorBlock);

                frontier.add(domBlockChildren);
            }
        }
        return rootDominatorBlock;
    }

    private void generateDomTreeForProgram() {
        for (Map<BasicBlock, Set<BasicBlock>> domRelations : allDomRelationsInProgram) {
            BasicBlock rootBasicBlock = getRootBasicBlock(domRelations);
            if (rootBasicBlock != null) {
                DominatorBlock rootDomBlock = generateDomTreeForRoot(rootBasicBlock, domRelations);
                rootDomBlock.setParent(null);
                allRootDominatorBlocks.add(rootDomBlock);

            } else {
                System.err.println("Root Basic Block not found");
                System.exit(100);
            }
        }
    }

    public void generateDomRelationsForProgram() {
        Set<Function> functionsSet = new HashSet<>();
        for (Map.Entry<String, Function> entry : functions.entrySet()) functionsSet.add(entry.getValue());
        generateDomRelationsForProgram(mainStartBasicBlock, functionsSet);
        generateDomTreeForProgram();
    }

    public void printDomForProgram() {
        for (Map<BasicBlock, Set<BasicBlock>> domRelations : allDomRelationsInProgram) {
            printBlockDomRelations(domRelations);
        }
    }

    public void printDomVCGForProgram(String fileName) {
        List<String> domDigraph = new ArrayList<>();
        domDigraph.add("digraph{");
        for (Map<BasicBlock, Set<BasicBlock>> domRelations : allDomRelationsInProgram) {
            for (Map.Entry<BasicBlock, Set<BasicBlock>> entry : domRelations.entrySet()) {
                Integer parentBasicBlockId = entry.getKey().getId();
                for (BasicBlock basicBlock : entry.getValue()) {
                    domDigraph.add("BasicBlock" + parentBasicBlockId + " -> BasicBlock" + basicBlock.getId());
                }
            }
        }
        domDigraph.add("}");
        ControlFlowGraph.generateFlow(fileName, domDigraph, "DOM");
    }
}
