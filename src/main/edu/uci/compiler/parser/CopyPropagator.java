package main.edu.uci.compiler.parser;

import main.edu.uci.compiler.model.*;

import java.util.*;

/**
 * Created by srikrishna on 2/23/17.
 */
public class CopyPropagator {
    Set<DominatorBlock> allRootDominatorBlocks;

    public CopyPropagator(Set<DominatorBlock> allRootDominatorBlocks) {
        this.allRootDominatorBlocks = allRootDominatorBlocks;
    }

    public void propagateCopiesForProgram() {
        for (DominatorBlock rootDomBlock : allRootDominatorBlocks) {
            propagateCopiesForDomTree(rootDomBlock);
        }
    }

    private void propagateCopiesForDomTree(DominatorBlock rootDomBlock) {
        HashMap<Result, Result> copies = new HashMap<>();
        HashSet<Instruction> phiInstructions = new HashSet<>();
        propagateCopiesAcrossBasicBlocks(rootDomBlock, copies, phiInstructions);
        propagatePhiInstructions(phiInstructions, copies);
    }

    private void propagateCopiesAcrossBasicBlocks(DominatorBlock rootDomBlock,
                                                  HashMap<Result, Result> copies,
                                                  Set<Instruction> phiInstructions) {
        BasicBlock basicBlock = rootDomBlock.getMyBasicBlock();
        List<Instruction> instructions = basicBlock.getInstructions();
        propagateCopiesInBasicBlock(instructions, copies, phiInstructions);
        for (DominatorBlock childDomBlock : rootDomBlock.getChildren()) {
            propagateCopiesAcrossBasicBlocks(childDomBlock, copies, phiInstructions);
        }

    }

    private void propagateCopiesInBasicBlock(List<Instruction> instructions,
                                             HashMap<Result, Result> copies,
                                             Set<Instruction> phiInstructions) {
        Set<Instruction> toBeDeletedInstruction = new HashSet<>();
        for (Instruction instruction : instructions) {

            Operation operation = instruction.getOperation();
            Result operand1 = instruction.getOperand1();
            Result operand2 = instruction.getOperand2();
            Result operand3 = instruction.getOperand3();

            if (operation == Operation.MOVE) {
                toBeDeletedInstruction.add(instruction);
                Result finalCopy = getFinalCopy(operand1, copies);
                copies.put(operand2, finalCopy);
            } else {
                if (operation == Operation.PHI) {
                    phiInstructions.add(instruction);
                    continue;
                }
                if (operand1 != null && copies.containsKey(operand1)) {
                    instruction.setOperand1(copies.get(operand1));
                }
                if (operand2 != null && copies.containsKey(operand2)) {
                    instruction.setOperand2(copies.get(operand2));
                }
            }
        }
        instructions.removeAll(new LinkedList<>(toBeDeletedInstruction));
    }

    private void propagatePhiInstructions(Set<Instruction> phiInstructions,
                                          HashMap<Result, Result> copies) {
        for (Instruction instruction : phiInstructions) {

            Result operand1 = instruction.getOperand1();
            Result operand2 = instruction.getOperand2();

            if (operand1 != null && copies.containsKey(operand1)) {
                instruction.setOperand1(copies.get(operand1));
            }
            if (operand2 != null && copies.containsKey(operand2)) {
                instruction.setOperand2(copies.get(operand2));
            }
        }
    }

    private Result getFinalCopy(Result operand, HashMap<Result, Result> copies) {
        if (!copies.containsKey(operand)) return operand;
        return getFinalCopy(copies.get(operand), copies);
    }


}
