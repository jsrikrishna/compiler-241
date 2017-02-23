package main.edu.uci.compiler.model;

import sun.jvm.hotspot.debugger.bsd.amd64.BsdAMD64CFrame;

import java.util.*;

/**
 * Created by srikrishna on 2/22/17.
 * Implements the naive algorithm to find the nodes dominated by a given basic block
 */
public class DominatorTree {
    private BasicBlock mainStartBasicBlock;
    private HashMap<String, Function> functions;
    private DominatorNode root;
    private HashSet<Map<BasicBlock, Set<BasicBlock>>> allDominatorsInProgram;


    public DominatorTree(BasicBlock mainStartBasicBlock, HashMap<String, Function> functions) {
        this.mainStartBasicBlock = mainStartBasicBlock;
        this.functions = functions;
        allDominatorsInProgram = new HashSet<>();
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
            for (BasicBlock children : currentBasicBlock.getChildren()) {
                if (!listOfAllBasicBlocks.contains(children)) frontier.add(children);
            }
        }
        return listOfAllBasicBlocks;
    }

    private void generateDomRelationsForProgram(BasicBlock mainStartBasicBlock,
                                               Set<Function> functions) {
        Set<BasicBlock> mainBasicBlocks = basicBlockBFS(mainStartBasicBlock);
        Map<BasicBlock, Set<BasicBlock>> dominanceRelationship = generateDomRelations(mainBasicBlocks, mainStartBasicBlock);
        allDominatorsInProgram.add(dominanceRelationship);
        for (Function function : functions) {
            BasicBlock funcBasicBlock = function.getFuncBasicBlock();
            Set<BasicBlock> funcBasicBlocks = basicBlockBFS(funcBasicBlock);
            Map<BasicBlock, Set<BasicBlock>> relationships = generateDomRelations(funcBasicBlocks, funcBasicBlock);
            allDominatorsInProgram.add(relationships);
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

    public void printDomForProgram(){
        Set<Function> functionsSet = new HashSet<>();
        for(Map.Entry<String, Function> entry : functions.entrySet()) functionsSet.add(entry.getValue());
        generateDomRelationsForProgram(mainStartBasicBlock, functionsSet);
        for(Map<BasicBlock, Set<BasicBlock>> domRelations : allDominatorsInProgram){
            printBlockDomRelations(domRelations);
        }
    }

}
