package main.edu.uci.compiler.parser;

import main.edu.uci.compiler.cfg.ControlFlowGraph;
import main.edu.uci.compiler.model.*;

import static main.edu.uci.compiler.model.Operation.*;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.*;

/**
 * Created by srikrishna on 3/5/17.
 */
public class LiveRangeAnalysis {
    private Set<BasicBlock> endBasicBlocks;
    private InterferenceGraph interferenceGraph;
    private Set<Result> liveRangeSet;
    private HashMap<Integer, HashSet<Integer>> adjacencyList;
    private HashMap<BasicBlock, BasicBlock> allDomParents;

    public LiveRangeAnalysis(Set<BasicBlock> endBasicBlocks, HashMap<BasicBlock, BasicBlock> allDomParents) {
        this.liveRangeSet = new HashSet<>();
        this.endBasicBlocks = endBasicBlocks;
        this.interferenceGraph = new InterferenceGraph();
        this.allDomParents = allDomParents;
        adjacencyList = new HashMap<>();
        this.interferenceGraph.setAdjacencyList(adjacencyList);
    }

    public void generateInterferenceGraphForProgram() {
        for (BasicBlock endBasicBlock : endBasicBlocks) {
            Set<Integer> liveRanges = new HashSet<>();
            generateInterferenceGraph(endBasicBlock, liveRanges);
        }
    }

    public void generateInterferenceGraph(BasicBlock endBasicBlock, Set<Integer> liveRangesSet) {
        Set<BasicBlock> visited = new HashSet<>();


    }

    public void generateLiveRangesForBasicBlock(BasicBlock basicBlock, Set<Integer> liveRangeSet) {
        for (Instruction instruction : basicBlock.getInstructions()) {
            Operation op = instruction.getOperation();

            if (isCondBranchInstruction(op)) {
                Result result = instruction.getOperand1();
                if (!isInstructionResult(result)) {
                    System.out.println("First Operand of COND.BRANCH Result should beof type Instruction");
                    System.exit(43);
                }
                liveRangeSet.add(result.getInstructionId());
                continue;
            }

            if (isBranchInstruction(op) || isEndInstruction(op)) continue;

            Integer instructionId = instruction.getInstructionId();
            if (liveRangeSet.contains(instructionId)) {

                if (inWhileIsPhi(basicBlock, instruction) && !isVisited(basicBlock)) {
                    basicBlock.setIsVisitedWhileLiveRangeAnalysis();
                } else {
                    liveRangeSet.remove(instructionId);
                }
            }

            if (!adjacencyList.containsKey(instructionId)) {
                adjacencyList.put(instructionId, new HashSet<>());
            }
            for (Integer liveInstructionId : liveRangeSet) {
                adjacencyList.get(instructionId).add(liveInstructionId);
            }

            addResultToLiveRange(instruction.getOperand1(), liveRangeSet);
            addResultToLiveRange(instruction.getOperand2(), liveRangeSet);
        }

    }

    private void addResultToLiveRange(Result operand, Set<Integer> liveRangeSet) {
        if (operand != null) {
            if (isInstructionResult(operand)) {
                liveRangeSet.add(operand.getInstructionId());
                return;
            }
            if (isVariableResult(operand)) {
                liveRangeSet.add(operand.getSsaVersion());
                return;
            }
        }
    }

    private boolean isCondBranchInstruction(Operation op) {
        return (op == BNE || op == BEQ || op == BLE || op == BLT || op == BGE || op == BGT);
    }

    private boolean isBranchInstruction(Operation op) {
        return op == BRA;
    }

    private boolean isEndInstruction(Operation op) {
        return op == END;
    }

    private boolean isInstructionResult(Result result) {
        return result.getKind() == Result.KIND.INSTRUCTION;
    }

    private boolean isVariableResult(Result result) {
        return result.getKind() == Result.KIND.VARIABLE;
    }

    private boolean isWhileHeaderBlock(BasicBlock basicBlock) {
        return basicBlock.getType() == BasicBlock.Type.BB_WHILE_CONDITION_AND_JOIN;
    }

    private boolean isPhiInstruction(Instruction instruction) {
        return instruction.getOperation() == PHI;
    }

    private boolean isVisited(BasicBlock basicBlock) {
        return basicBlock.getIsVisitedWhileLiveRangeAnalysis();
    }

    private boolean inWhileIsPhi(BasicBlock basicBlock, Instruction instruction) {
        return isWhileHeaderBlock(basicBlock) && isPhiInstruction(instruction);
    }


}
