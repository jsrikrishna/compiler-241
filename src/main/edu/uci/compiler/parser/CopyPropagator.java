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
                if (isConstantInstruction(instruction)) {
                    Result constResult = generateConstantResult(instruction);
                    toBeDeletedInstruction.add(instruction);
                    if (!instructionResults.containsKey(instruction)) {
                        System.out.println("Should contain constant instruction result");
                        System.exit(44);
                    }
                    copies.put(instructionResults.get(instruction), constResult);
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
        if(operation == Operation.DIV && operand2.getValue() == 0) return false;
        return operand1.getKind() == Result.KIND.CONSTANT && operand2.getKind() == Result.KIND.CONSTANT;
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
