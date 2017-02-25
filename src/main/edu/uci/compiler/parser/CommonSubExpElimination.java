package main.edu.uci.compiler.parser;

import main.edu.uci.compiler.model.BasicBlock;
import main.edu.uci.compiler.model.DominatorBlock;
import main.edu.uci.compiler.model.Instruction;
import main.edu.uci.compiler.model.Operation;

import java.util.HashMap;
import java.util.Set;

public class CommonSubExpElimination {
    private Set<DominatorBlock> allRootDominatorBlocks;

    public CommonSubExpElimination(Set<DominatorBlock> allRootDominatorBlocks) {
        this.allRootDominatorBlocks = allRootDominatorBlocks;
    }

    public void generateAnchorRelationsForProgram() {
        System.out.println("All Root Dom Blocks " + allRootDominatorBlocks.size());
        for (DominatorBlock rootDomBlock : allRootDominatorBlocks) {
            generateAnchorRelationsForDomTree(rootDomBlock);
        }

    }

    public void generateAnchorRelationsForDomTree(DominatorBlock rootDomBlock) {
        generateAnchorRelationsAcrossBasicBlocks(rootDomBlock);
        printInstrcutionAndAnchorInstruction(rootDomBlock);

    }

    public void generateAnchorRelationsAcrossBasicBlocks(DominatorBlock rootDomBlock) {
        generateAnchorRelationsInBasicBlock(rootDomBlock);
        for (DominatorBlock childDomBlock : rootDomBlock.getChildren()) {
            generateAnchorRelationsAcrossBasicBlocks(childDomBlock);
        }
    }

    public void generateAnchorRelationsInBasicBlock(DominatorBlock domBlock) {
        BasicBlock basicBlock = domBlock.getMyBasicBlock();
        HashMap<Operation, Instruction> anchor = new HashMap<>();
        for (Instruction instruction : basicBlock.getInstructions()) {
            Operation operation = instruction.getOperation();
            if (anchor.containsKey(operation)) {
                instruction.setAnchorInstruction(anchor.get(operation));
            } else {
                Instruction parentAnchorInstruction = findInstructionInParents(domBlock, operation);
                if(parentAnchorInstruction != null){
                    instruction.setAnchorInstruction(parentAnchorInstruction);
                }
            }
            anchor.put(operation, instruction);
        }
        basicBlock.setAnchor(anchor);
        System.out.println("Basic Block " + basicBlock.getId());
        System.out.println(basicBlock.getAnchor());
        System.out.println();
    }

    public Instruction findInstructionInParents(DominatorBlock domBlock, Operation operation){
        DominatorBlock parentDomBlock = domBlock.getParent();
        while (parentDomBlock != null){
            HashMap<Operation, Instruction> parentAnchor = parentDomBlock.getMyBasicBlock().getAnchor();
            if(parentAnchor.containsKey(operation)){
                return parentAnchor.get(operation);
            }
            parentDomBlock = parentDomBlock.getParent();
        }
        return null;
    }

    /*
    This method is for testing anchor relationships
     */
    public void printInstrcutionAndAnchorInstruction(DominatorBlock rootDomBlock){
        BasicBlock basicBlock = rootDomBlock.getMyBasicBlock();
        for(Instruction instruction : basicBlock.getInstructions()){
            Integer instId;
            if(instruction.getAnchorInstruction() == null) instId = null;
            else instId = instruction.getAnchorInstruction().getInstructionId();
            System.out.println(instruction.getInstructionId() + " -> " + instId);
        }
        for(DominatorBlock childDomBlock : rootDomBlock.getChildren()){
            printInstrcutionAndAnchorInstruction(childDomBlock);
        }

    }
}
