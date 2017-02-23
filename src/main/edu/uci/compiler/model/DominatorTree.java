package main.edu.uci.compiler.model;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.*;

/**
 * Created by srikrishna on 2/22/17.
 * Implements the naive algorithm to find the nodes dominated by a given basic block
 */
public class DominatorTree {
    private BasicBlock mainStartBasicBlock;
    private HashMap<String, Function> functions;
    private Set<BasicBlock> allRootBasicBlocks;
    private HashSet<Map<BasicBlock, Set<BasicBlock>>> allDominatorsInProgram;
    private Set<DominatorBlock> allRootDominatorBlocks;


    public DominatorTree(BasicBlock mainStartBasicBlock, HashMap<String, Function> functions) {
        this.mainStartBasicBlock = mainStartBasicBlock;
        this.functions = functions;
        allRootBasicBlocks = new HashSet<>();
        allRootDominatorBlocks = new HashSet<>();
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
        allRootBasicBlocks.add(mainStartBasicBlock);
        Set<BasicBlock> mainBasicBlocks = basicBlockBFS(mainStartBasicBlock);
        Map<BasicBlock, Set<BasicBlock>> domRelations = generateDomRelations(mainBasicBlocks, mainStartBasicBlock);
        generateTransitiveDomDependency(domRelations);
        allDominatorsInProgram.add(domRelations);
        for (Function function : functions) {
            BasicBlock funcBasicBlock = function.getFuncBasicBlock();
            allRootBasicBlocks.add(funcBasicBlock);
            Set<BasicBlock> funcBasicBlocks = basicBlockBFS(funcBasicBlock);
            domRelations = generateDomRelations(funcBasicBlocks, funcBasicBlock);
            generateTransitiveDomDependency(domRelations);
            allDominatorsInProgram.add(domRelations);
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
            Set<BasicBlock> domChildrens = domRelations.get(dominatorBlock.getMyBasicBlock());
            for (BasicBlock domChildren : domChildrens) {
                frontier.add(new DominatorBlock(domChildren));
            }
        }
        return rootDominatorBlock;
    }

    private void generateDomTreeForProgram() {
        for (Map<BasicBlock, Set<BasicBlock>> domRelations : allDominatorsInProgram) {
            BasicBlock rootBasicBlock = getRootBasicBlock(domRelations);
            if (rootBasicBlock != null) {
                allRootDominatorBlocks.add(generateDomTreeForRoot(rootBasicBlock, domRelations));

            } else {
                System.err.println("Root Basic Block not found");
                System.exit(100);
            }
        }
    }

    private void generateDomRelationsForProgram(){
        Set<Function> functionsSet = new HashSet<>();
        for (Map.Entry<String, Function> entry : functions.entrySet()) functionsSet.add(entry.getValue());
        generateDomRelationsForProgram(mainStartBasicBlock, functionsSet);
        generateDomTreeForProgram();
    }

    public void printDomForProgram() {
        for (Map<BasicBlock, Set<BasicBlock>> domRelations : allDominatorsInProgram) {
            printBlockDomRelations(domRelations);
        }
    }

    public void generateDomVCGForProgram(String fileName) {
        generateDomRelationsForProgram();

        List<String> domDigraph = new ArrayList<>();
        domDigraph.add("digraph{");
        for (Map<BasicBlock, Set<BasicBlock>> domRelations : allDominatorsInProgram) {
            for (Map.Entry<BasicBlock, Set<BasicBlock>> entry : domRelations.entrySet()) {
                Integer parentBasicBlockId = entry.getKey().getId();
                for (BasicBlock basicBlock : entry.getValue()) {
                    domDigraph.add("BasicBlock" + parentBasicBlockId + " -> BasicBlock" + basicBlock.getId());
                }
            }
        }
        domDigraph.add("}");
        generateDomVcgImage(fileName, domDigraph);
    }

    private void generateDomVcgImage(String fileName, List<String> domDigraph) {
        Writer writer = null;
        try {
            String newFileName = fileName.substring(0, fileName.length() - 4) + "DOM.dot";
            String domFileName = fileName.substring(0, fileName.length() - 4) + "DOM.png";
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newFileName), "utf-8"));
            for (String str : domDigraph) {
                writer.write(str + "\n");
            }
            Runtime.getRuntime().exec("dot -Tpng " + newFileName + " -o " + domFileName);
        } catch (Exception ex) {
            System.err.print("Error occured while writing DOM Data to file");
        } finally {
            try {
                writer.close();
            } catch (Exception ex) {
                System.err.println("Error while closing writer and exiting");
            }
        }
    }


}
