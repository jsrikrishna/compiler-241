package main.edu.uci.compiler.model;

import sun.jvm.hotspot.debugger.bsd.amd64.BsdAMD64CFrame;

import java.util.*;

/**
 * Created by srikrishna on 2/22/17.
 * Implements the naive algorithm to find the nodes dominated by a given basic block
 */
public class DominatorTree {
    private List<BasicBlock> allBasicBlocks;
    private BasicBlock startBasicBlock;
    private DominatorNode root;
    private Map<BasicBlock, Set<BasicBlock>> dominatorRelationships;

    public DominatorTree(List<BasicBlock> allBasicBlocks, BasicBlock startBasicBlock) {
        this.startBasicBlock = startBasicBlock;
        this.allBasicBlocks = allBasicBlocks;
        dominatorRelationships = new HashMap<>();
    }

    public void findAllReachableBlocksExceptFromV(BasicBlock currentBlock,
                                                  BasicBlock v,
                                                  Set<BasicBlock> visitedBlocks) {

        if (!visitedBlocks.contains(currentBlock) && v.getId() != currentBlock.getId()) {
            visitedBlocks.add(currentBlock);
            for (BasicBlock children : currentBlock.getChildren())
                findAllReachableBlocksExceptFromV(children, v, visitedBlocks);
        }
    }

    public Set<BasicBlock> blocksDominatedByV(List<BasicBlock> allBasicBlocks,
                                              BasicBlock root,
                                              BasicBlock v) {
        HashSet<BasicBlock> visitedBlocks = new HashSet<>();
        findAllReachableBlocksExceptFromV(root, v, visitedBlocks);
        HashSet<BasicBlock> allBasicBlocksSet = new HashSet<>(allBasicBlocks);
        allBasicBlocksSet.remove(v);
        allBasicBlocksSet.removeAll(visitedBlocks);
        return allBasicBlocksSet;
    }

    public void generateDominationRelationships(List<BasicBlock> allBasicBlocks, BasicBlock root) {
        for (BasicBlock currentBlock : allBasicBlocks) {
            Set<BasicBlock> blocksDominatedByCurrentBlock = blocksDominatedByV(allBasicBlocks, root, currentBlock);
            dominatorRelationships.put(currentBlock, blocksDominatedByCurrentBlock);
        }
    }

    public void printDominatorRelationships() {
        generateDominationRelationships(allBasicBlocks, startBasicBlock);
        System.out.println("size is " + dominatorRelationships.size());
        for (Map.Entry<BasicBlock, Set<BasicBlock>> entry : dominatorRelationships.entrySet()) {
            BasicBlock basicBlock = entry.getKey();
            Set<BasicBlock> blocksDominated = entry.getValue();
            System.out.print("[" + basicBlock.getId() + "]: {");
            for (BasicBlock blockDominated : blocksDominated) {
                System.out.print("[" + blockDominated.getId() + "]");

            }
            System.out.print("}\n");
        }
    }

}
