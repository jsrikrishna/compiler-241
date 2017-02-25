package main.edu.uci.compiler.parser;

import main.edu.uci.compiler.model.*;

import java.util.*;

public class CommonSubExpElimination {
    private Set<DominatorBlock> allRootDominatorBlocks;
    private HashMap<Instruction, Result> instructionResults;
    private HashMap<Integer, Instruction> allInstructions;

    public CommonSubExpElimination(Set<DominatorBlock> allRootDominatorBlocks,
                                   HashMap<Instruction, Result> instructionResults,
                                   HashMap<Integer, Instruction> allInstructions) {

        this.instructionResults = instructionResults;
        this.allRootDominatorBlocks = allRootDominatorBlocks;
        this.allInstructions = allInstructions;
    }

    public void generateAnchorRelationsForProgram() {
        System.out.println("All Root Dom Blocks " + allRootDominatorBlocks.size());
        for (DominatorBlock rootDomBlock : allRootDominatorBlocks) {
            generateAnchorRelationsForDomTree(rootDomBlock);
        }
    }

    private void generateAnchorRelationsForDomTree(DominatorBlock rootDomBlock) {
        generateAnchorRelationsAcrossBasicBlocks(rootDomBlock);
        printInstrcutionAndAnchorInstruction(rootDomBlock);
    }

    private void generateAnchorRelationsAcrossBasicBlocks(DominatorBlock rootDomBlock) {
        generateAnchorRelationsInBasicBlock(rootDomBlock);
        for (DominatorBlock childDomBlock : rootDomBlock.getChildren()) {
            generateAnchorRelationsAcrossBasicBlocks(childDomBlock);
        }
    }

    private void generateAnchorRelationsInBasicBlock(DominatorBlock domBlock) {
        BasicBlock basicBlock = domBlock.getMyBasicBlock();
        HashMap<Operation, Instruction> anchor = new HashMap<>();
        for (Instruction instruction : basicBlock.getInstructions()) {
            Operation operation = instruction.getOperation();
            if (anchor.containsKey(operation)) {
                instruction.setAnchorInstruction(anchor.get(operation));
            } else {
                Instruction parentAnchorInstruction = findInstructionInParents(domBlock, operation);
                if (parentAnchorInstruction != null) {
                    instruction.setAnchorInstruction(parentAnchorInstruction);
                }
            }
            anchor.put(operation, instruction);
        }
        basicBlock.setAnchor(anchor);
//        System.out.println("Basic Block " + basicBlock.getId());
//        System.out.println(basicBlock.getAnchor());
//        System.out.println();
    }

    private Instruction findInstructionInParents(DominatorBlock domBlock, Operation operation) {
        DominatorBlock parentDomBlock = domBlock.getParent();
        while (parentDomBlock != null) {
            HashMap<Operation, Instruction> parentAnchor = parentDomBlock.getMyBasicBlock().getAnchor();
            if (parentAnchor.containsKey(operation)) {
                return parentAnchor.get(operation);
            }
            parentDomBlock = parentDomBlock.getParent();
        }
        return null;
    }

    public void doCSEForProgram() {
        generateAnchorRelationsForProgram();
        for (DominatorBlock rootDomBlock : allRootDominatorBlocks) {
            doCSEForDomTree(rootDomBlock);
        }
    }

    public void doCSEForDomTree(DominatorBlock rootDomBlock) {
        HashMap<Instruction, Result> toBeRemovedInstruction = new HashMap<>();
        doCSEAcrossDomBlocks(rootDomBlock, toBeRemovedInstruction);
        printInstructionsToBeRemoved(toBeRemovedInstruction);
        checkInstructionAndReplace(rootDomBlock, toBeRemovedInstruction);

    }

    public void doCSEAcrossDomBlocks(DominatorBlock dominatorBlock,
                                     HashMap<Instruction, Result> toBeRemovedInstruction) {
        doCSEInDomBlock(dominatorBlock, toBeRemovedInstruction);
        for (DominatorBlock childDomBlock : dominatorBlock.getChildren()) {
            doCSEAcrossDomBlocks(childDomBlock, toBeRemovedInstruction);
        }

    }

    public void doCSEInDomBlock(DominatorBlock domBlock, HashMap<Instruction, Result> toBeRemovedInstruction) {
        BasicBlock basicBlock = domBlock.getMyBasicBlock();
        LinkedList<Instruction> instructions = basicBlock.getInstructions();
        for (Instruction instruction : instructions) {
            checkForDuplicateInstruction(instruction, toBeRemovedInstruction);
        }
    }

    private void checkForDuplicateInstruction(Instruction toBeCheckedInstruction,
                                              HashMap<Instruction, Result> toBeRemovedInstruction) {

        Instruction anchorInstruction = toBeCheckedInstruction.getAnchorInstruction();
        while (anchorInstruction != null) {
            if (toBeCheckedInstruction.equals(anchorInstruction)) {

                Result canBeReplacedWithResult;
                canBeReplacedWithResult = instructionResults.get(anchorInstruction);

                if (toBeRemovedInstruction.containsKey(anchorInstruction)) {
                    canBeReplacedWithResult = toBeRemovedInstruction.get(anchorInstruction);
                }

                if (canBeReplacedWithResult == null) {
                    System.err.println("this cannot be possible " + toBeCheckedInstruction);
                    System.exit(100);
                }

                toBeRemovedInstruction.put(toBeCheckedInstruction, canBeReplacedWithResult);
                return;
            }
            anchorInstruction = anchorInstruction.getAnchorInstruction();
        }
    }

    private void checkInstructionAndReplace(DominatorBlock rootDomBlock,
                                            HashMap<Instruction, Result> toBeRemovedInstruction) {

        LinkedList<Instruction> instructions = rootDomBlock.getMyBasicBlock().getInstructions();
        Iterator<Instruction> iterator = instructions.iterator();
        while (iterator.hasNext()) {
            Instruction instruction = iterator.next();
            if (toBeRemovedInstruction.containsKey(instruction)) {
                iterator.remove();
                continue;
            }
            Result operand1 = instruction.getOperand1();
            Result operand2 = instruction.getOperand2();
            if (isInstructionResult(operand1)) {
                Instruction checkForRemovabilityInstruction = allInstructions.get(operand1.getInstructionId());
                if (toBeRemovedInstruction.containsKey(checkForRemovabilityInstruction)) {
                    instruction.setOperand1(toBeRemovedInstruction.get(checkForRemovabilityInstruction));
                }
            }
            if (isInstructionResult(operand2)) {
                Instruction checkForRemovabilityInstruction = allInstructions.get(operand2.getInstructionId());
                if (toBeRemovedInstruction.containsKey(checkForRemovabilityInstruction)) {
                    instruction.setOperand2(toBeRemovedInstruction.get(checkForRemovabilityInstruction));
                }
            }
        }
        for (DominatorBlock childDomBlock : rootDomBlock.getChildren()) {
            checkInstructionAndReplace(childDomBlock, toBeRemovedInstruction);
        }
    }

    private boolean isInstructionResult(Result operand) {
        return operand != null && operand.getKind().equals(Result.KIND.INSTRUCTION);
    }


    /*
    This method is for printing/testing anchor relationships
     */
    public void printInstrcutionAndAnchorInstruction(DominatorBlock rootDomBlock) {
        BasicBlock basicBlock = rootDomBlock.getMyBasicBlock();
        for (Instruction instruction : basicBlock.getInstructions()) {
            Integer instId;
            if (instruction.getAnchorInstruction() == null) instId = null;
            else instId = instruction.getAnchorInstruction().getInstructionId();
            System.out.println(instruction.getInstructionId() + " -> " + instId);
        }
        for (DominatorBlock childDomBlock : rootDomBlock.getChildren()) {
            printInstrcutionAndAnchorInstruction(childDomBlock);
        }
    }

    public void printInstructionsToBeRemoved(HashMap<Instruction, Result> toBeRemovedInstruction) {
        System.out.println("Instructions to be removed " + toBeRemovedInstruction.size());
        for (Map.Entry<Instruction, Result> entry : toBeRemovedInstruction.entrySet()) {
            System.out.println(entry.getKey().getInstructionId()
                    + ": " + entry.getKey() + " -> " + entry.getValue());
        }
    }
}
