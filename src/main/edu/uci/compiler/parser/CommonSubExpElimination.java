package main.edu.uci.compiler.parser;

import main.edu.uci.compiler.model.*;

import static main.edu.uci.compiler.model.Operation.*;

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

    private void generateAnchorRelationsForProgram() {
        for (DominatorBlock rootDomBlock : allRootDominatorBlocks) {
            generateAnchorRelationsForDomTree(rootDomBlock);
        }
    }

    private void generateAnchorRelationsForDomTree(DominatorBlock rootDomBlock) {
        generateAnchorRelationsAcrossBasicBlocks(rootDomBlock);
//        printInstrcutionAndAnchorInstruction(rootDomBlock);
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
            if (canAnchorLinkIgnored(operation)) continue;
            if (operation == LOAD || operation == ADDA) {
                Result arrayVariable = instruction.getArrayVariable();
                if (anchor.containsKey(KILL)
                        && !anchor.containsKey(LOAD)
                        && anchor.get(KILL).getArrayVariable().equals(arrayVariable)) {

                    if(operation == ADDA
                            && anchor.get(KILL).getForwardAnchorInstruction() == null){
                        instruction.setAnchorInstruction(anchor.get(KILL));
                        anchor.get(KILL).setForwardAnchorInstruction(instruction);
                    }

                    else if(operation == LOAD
                            && anchor.get(KILL).getForwardAnchorInstruction() != null
                            && anchor.get(KILL).getForwardAnchorInstruction().getOperation() == ADDA){
                        instruction.setAnchorInstruction(anchor.get(KILL));
                    }

                    else {
                        setAnchorInstruction(domBlock, anchor, instruction, operation);
                    }
                }
            }
            if (instruction.getAnchorInstruction() == null) {
                setAnchorInstruction(domBlock, anchor, instruction, operation);
            }
            anchor.put(operation, instruction);
        }
        basicBlock.setAnchor(anchor);
//        System.out.println("Basic Block " + basicBlock.getId());
//        System.out.println(basicBlock.getAnchor());
//        System.out.println();
    }

    private void setAnchorInstruction(DominatorBlock domBlock,
                                      HashMap<Operation, Instruction> anchor,
                                      Instruction instruction,
                                      Operation operation) {
        if (anchor.containsKey(operation)) {
            instruction.setAnchorInstruction(anchor.get(operation));
        } else {
            Instruction parentAnchorInstruction = findInstructionInParents(domBlock, operation);
            if (parentAnchorInstruction != null) {
                instruction.setAnchorInstruction(parentAnchorInstruction);
            }
        }
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

    private void doCSEForDomTree(DominatorBlock rootDomBlock) {
        HashMap<Instruction, Result> toBeRemovedInstruction = new HashMap<>();
        ArrayList<Instruction> killInstructions = new ArrayList<>();
        doCSEAcrossDomBlocks(rootDomBlock, toBeRemovedInstruction, killInstructions);
        printInstructionsToBeRemoved(toBeRemovedInstruction);
        checkInstructionAndReplace(rootDomBlock, toBeRemovedInstruction);
        for (Instruction kill : killInstructions) {
            kill.getBasicBlock().removeInstruction(kill);
            allInstructions.remove(kill.getInstructionId());
        }
    }

    private void doCSEAcrossDomBlocks(DominatorBlock dominatorBlock,
                                      HashMap<Instruction, Result> toBeRemovedInstruction,
                                      ArrayList<Instruction> killInstructions) {
        doCSEInDomBlock(dominatorBlock, toBeRemovedInstruction, killInstructions);
        for (DominatorBlock childDomBlock : dominatorBlock.getChildren()) {
            doCSEAcrossDomBlocks(childDomBlock, toBeRemovedInstruction, killInstructions);
        }

    }

    private void doCSEInDomBlock(DominatorBlock domBlock,
                                 HashMap<Instruction, Result> toBeRemovedInstruction,
                                 ArrayList<Instruction> killInstructions) {
        BasicBlock basicBlock = domBlock.getMyBasicBlock();
        LinkedList<Instruction> instructions = basicBlock.getInstructions();
        for (Instruction instruction : instructions) {
            if (instruction.getOperation() == KILL) killInstructions.add(instruction);
            checkForDuplicateInstruction(instruction, toBeRemovedInstruction);
        }
    }

    private void checkForDuplicateInstruction(Instruction toBeCheckedInstruction,
                                              HashMap<Instruction, Result> toBeRemovedInstruction) {
        if (needNotDoCSE(toBeCheckedInstruction)) return;
        Instruction anchorInstruction = toBeCheckedInstruction.getAnchorInstruction();
        while (anchorInstruction != null) {
            if (toBeCheckedInstruction.equals(anchorInstruction)) {
                Result canBeReplacedWithResult;
                canBeReplacedWithResult = instructionResults.get(anchorInstruction);

                if (toBeRemovedInstruction.containsKey(anchorInstruction)) {
                    canBeReplacedWithResult = toBeRemovedInstruction.get(anchorInstruction);
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
                    Instruction anchorInstruction = checkForRemovabilityInstruction.getAnchorInstruction();
                    Result canBeReplacedWithResult = null;
                    while (anchorInstruction != null) {
                        if (checkForRemovabilityInstruction.equals(anchorInstruction)) {
                            canBeReplacedWithResult = instructionResults.get(anchorInstruction);
                            if (toBeRemovedInstruction.containsKey(anchorInstruction)) {
                                canBeReplacedWithResult = toBeRemovedInstruction.get(anchorInstruction);
                            }
                            break;
                        }
                        anchorInstruction = anchorInstruction.getAnchorInstruction();
                    }
                    instruction.setOperand1(canBeReplacedWithResult);
                }
            }
            if (isInstructionResult(operand2)) {
                Instruction checkForRemovabilityInstruction = allInstructions.get(operand2.getInstructionId());
                if (toBeRemovedInstruction.containsKey(checkForRemovabilityInstruction)) {
                    Instruction anchorInstruction = checkForRemovabilityInstruction.getAnchorInstruction();
                    Result canBeReplacedWithResult = null;
                    while (anchorInstruction != null) {
                        if (checkForRemovabilityInstruction.equals(anchorInstruction)) {
                            canBeReplacedWithResult = instructionResults.get(anchorInstruction);
                            if (toBeRemovedInstruction.containsKey(anchorInstruction)) {
                                canBeReplacedWithResult = toBeRemovedInstruction.get(anchorInstruction);
                            }
                            break;
                        }
                        anchorInstruction = anchorInstruction.getAnchorInstruction();
                    }
                    instruction.setOperand2(canBeReplacedWithResult);
                }
            }
            Instruction anchorInstruction = instruction.getAnchorInstruction();
            if(instruction.equals(anchorInstruction)){
                toBeRemovedInstruction.put(instruction, instructionResults.get(instruction));
                iterator.remove();
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
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }

    private boolean needNotDoCSE(Instruction instruction) {
        Operation op = instruction.getOperation();
        return (op == Operation.WRITE || op == Operation.WRITENL || op == Operation.READ);
    }

    private boolean canAnchorLinkIgnored(Operation operation) {
        return operation == Operation.STORE;
    }
}
