package main.edu.uci.compiler.parser;

import main.edu.uci.compiler.model.*;

import java.util.*;

/**
 * Created by srikrishna on 2/23/17.
 */
public class CopyPropagator {
    Set<DominatorBlock> allRootDominatorBlocks;
    InstructionGenerator ig;
    HashMap<Instruction, Result> instructionResults;

    public CopyPropagator(Set<DominatorBlock> allRootDominatorBlocks,
                          InstructionGenerator ig,
                          HashMap<Instruction, Result> instructionResults) {
        this.allRootDominatorBlocks = allRootDominatorBlocks;
        this.ig = ig;
        this.instructionResults = instructionResults;
    }

    public void propagateCopiesForProgram(boolean doBranchFolding) {
        for (DominatorBlock rootDomBlock : allRootDominatorBlocks) {
            propagateCopiesForDomTree(rootDomBlock, doBranchFolding);
        }
    }

    private void propagateCopiesForDomTree(DominatorBlock rootDomBlock, boolean doBranchFolding) {
        HashMap<Result, Result> copies = new HashMap<>();
        HashSet<Instruction> phiInstructions = new HashSet<>();
        propagateCopiesAcrossBasicBlocks(rootDomBlock, copies, phiInstructions, doBranchFolding);
        propagatePhiInstructions(phiInstructions, copies);
    }

    private void propagateCopiesAcrossBasicBlocks(DominatorBlock rootDomBlock,
                                                  HashMap<Result, Result> copies,
                                                  Set<Instruction> phiInstructions,
                                                  boolean doBranchFolding) {
        BasicBlock basicBlock = rootDomBlock.getMyBasicBlock();
        List<Instruction> instructions = basicBlock.getInstructions();
        propagateCopiesInBasicBlock(instructions, copies, phiInstructions, doBranchFolding);
        for (DominatorBlock childDomBlock : rootDomBlock.getChildren()) {
            propagateCopiesAcrossBasicBlocks(childDomBlock, copies, phiInstructions, doBranchFolding);
        }

    }

    private void propagateCopiesInBasicBlock(List<Instruction> instructions,
                                             HashMap<Result, Result> copies,
                                             Set<Instruction> phiInstructions,
                                             boolean doBranchFolding) {
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
                if (isConstantInstruction(instruction)) {
                    Result constResult = generateConstantResult(instruction);
                    toBeDeletedInstruction.add(instruction);
                    if (!instructionResults.containsKey(instruction)) {
                        System.out.println("Should contain constant instruction result");
                        System.exit(44);
                    }
                    copies.put(instructionResults.get(instruction), constResult);
                }
                if (doBranchFolding) {
                    doBranchFolding(instruction);
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

    private boolean isConstantInstruction(Instruction instruction) {
        Operation operation = instruction.getOperation();
        Result operand1 = instruction.getOperand1();
        Result operand2 = instruction.getOperand2();
        if (operand1 == null || operand2 == null) return false;
        if (!isMathOperator(operation)) return false;
        if (operation == Operation.DIV && operand2.getValue() == 0) return false;
        return operand1.getKind() == Result.KIND.CONSTANT && operand2.getKind() == Result.KIND.CONSTANT;
    }

    private boolean doBranchFolding(Instruction instruction) {
        Operation operation = instruction.getOperation();
        Result operand1 = instruction.getOperand1();
        Result operand2 = instruction.getOperand2();
        if (operation == Operation.CMP) {
            if (operand1 != null && operand2 != null
                    && operand1.getKind() == Result.KIND.CONSTANT && operand2.getKind() == Result.KIND.CONSTANT) {
                Instruction nextInstruction = instruction.getNextInstruction();
                if (nextInstruction != null) {
                    Operation nextInstructionOperation = nextInstruction.getOperation();
                    if (!leftToBeRemoved(nextInstructionOperation, operand1, operand2)) {
                        // If
                        Integer toBeRemovedBasicBlockId = nextInstruction.getOperand2().getBasicBlockId();
                        System.out.println("Removing " + toBeRemovedBasicBlockId);
                        instruction.getBasicBlock().removeChildrenWithId(toBeRemovedBasicBlockId);
                    } else {
                        // Else
                        Integer toBeRemovedBasicBlockId = nextInstruction.getOperand2().getBasicBlockId();
                        System.out.println("Not Removing " + toBeRemovedBasicBlockId);
                        instruction.getBasicBlock().removeChildrenWithoutId(toBeRemovedBasicBlockId);
                    }
//                System.out.println("Next " + instruction.getNextInstruction());

                }


            }
            return false;
        }
        return false;
    }

    private boolean leftToBeRemoved(Operation nextInstructionOperation, Result operand1, Result operand2) {
        int lhs = operand1.getValue();
        int rhs = operand2.getValue();
        if (nextInstructionOperation == Operation.BEQ && lhs == rhs) return true;
        if (nextInstructionOperation == Operation.BNE && lhs != rhs) return true;
        if (nextInstructionOperation == Operation.BLT && lhs < rhs) return true;
        if (nextInstructionOperation == Operation.BGE && lhs >= rhs) return true;
        if (nextInstructionOperation == Operation.BLE && lhs <= rhs) return true;
        if (nextInstructionOperation == Operation.BGT && lhs > rhs) return true;
        return false;
    }

    private Result generateConstantResult(Instruction constantInstruction) {
        return ig.computeConstantResult(constantInstruction);
    }

    private boolean isMathOperator(Operation operation) {
        return operation == Operation.ADD
                || operation == Operation.SUB
                || operation == Operation.MUL
                || operation == Operation.DIV;
    }


}
