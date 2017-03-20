package main.edu.uci.compiler.parser;

import main.edu.uci.compiler.model.*;

import static main.edu.uci.compiler.model.Operation.*;

import java.util.*;

/**
 * Created by srikrishna on 3/5/17.
 */
public class LiveRangeAnalysis {
    private Set<BasicBlock> endBasicBlocks;
    private InterferenceGraph interferenceGraph;
    private HashMap<Integer, HashSet<Integer>> adjacencyList;
    private HashMap<BasicBlock, BasicBlock> allDomParents;
    private HashMap<Integer, Result> liveRangeNumberToResult;
    private Set<Instruction> allInstructions;
    private LinkedList<Instruction> phiInstructions;
    private HashMap<Instruction, Result> instructionResults;

    public LiveRangeAnalysis(Set<BasicBlock> endBasicBlocks,
                             HashMap<BasicBlock, BasicBlock> allDomParents,
                             LinkedList<Instruction> phiInstructions,
                             HashMap<Instruction, Result> instructionResults) {

        this.endBasicBlocks = endBasicBlocks;
        this.allDomParents = allDomParents;
        this.adjacencyList = new HashMap<>();
        this.liveRangeNumberToResult = new HashMap<>();
        this.phiInstructions = phiInstructions;
        this.allInstructions = new HashSet<>();
        this.instructionResults = instructionResults;
        this.interferenceGraph =
                new InterferenceGraph(adjacencyList, liveRangeNumberToResult, phiInstructions, allInstructions);
    }

    public InterferenceGraph getInterferenceGraph() {
        return this.interferenceGraph;
    }

    public void generateInterferenceGraphForProgram() {
        for (BasicBlock endBasicBlock : endBasicBlocks) {
            Set<Integer> liveRanges = new HashSet<>();
            generateInterferenceGraph(endBasicBlock, liveRanges, null);
        }
    }

    private Set<Integer> generateInterferenceGraph(BasicBlock endBasicBlock,
                                                   Set<Integer> liveRangeSet,
                                                   BasicBlock domParent) {

        Set<BasicBlock> visited = new HashSet<>();
        Queue<BasicBlock> frontier = new LinkedList<>();
        frontier.add(endBasicBlock);
        while (!frontier.isEmpty()) {
            BasicBlock currentBasicBlock = frontier.poll();
            if (isIfJoin(currentBasicBlock)) {
                LinkedList<Instruction> phiInstructions = liveRangesForABlock(currentBasicBlock, liveRangeSet);
                this.phiInstructions.addAll(phiInstructions);
                BasicBlock joinDOMParent = allDomParents.get(currentBasicBlock);
                BasicBlock rightParent = currentBasicBlock.getRightParent();
                BasicBlock leftParent = currentBasicBlock.getLeftParent();

                Set<Integer> rightCopy = makeCopy(liveRangeSet);
                Set<Integer> leftCopy = makeCopy(liveRangeSet);
                if (rightParent != null) {
                    for (Instruction phi : phiInstructions) {
                        if (rightCopy.contains(phi.getInstructionId())) {
                            rightCopy.remove(phi.getInstructionId());
                        }
                        for (Integer liveInstructionId : rightCopy) {
                            adjacencyList.get(phi.getInstructionId()).add(liveInstructionId);
                            if (adjacencyList.containsKey(liveInstructionId)) {
                                adjacencyList.get(liveInstructionId).add(phi.getInstructionId());
                            } else {
                                HashSet<Integer> edges = new HashSet<>(Arrays.asList(phi.getInstructionId()));
                                adjacencyList.put(liveInstructionId, edges);
                            }

                        }
                        addResultToLiveRange(phi.getOperand2(), rightCopy);
                    }
                }
                if (rightParent != null && rightParent != joinDOMParent) {
                    rightCopy = generateInterferenceGraph(rightParent, rightCopy, joinDOMParent);
                }
                if (leftParent != null) {
                    for (Instruction phi : phiInstructions) {
                        if (leftCopy.contains(phi.getInstructionId())) {
                            leftCopy.remove(phi.getInstructionId());
                        }
                        for (Integer liveInstructionId : leftCopy) {
                            adjacencyList.get(phi.getInstructionId()).add(liveInstructionId);
                            if (adjacencyList.containsKey(liveInstructionId)) {
                                adjacencyList.get(liveInstructionId).add(phi.getInstructionId());
                            } else {
                                HashSet<Integer> edges = new HashSet<>(Arrays.asList(phi.getInstructionId()));
                                adjacencyList.put(liveInstructionId, edges);
                            }
                        }
                        addResultToLiveRange(phi.getOperand1(), leftCopy);

                    }
                }
                if (leftParent != null && leftParent != joinDOMParent) {
                    leftCopy = generateInterferenceGraph(leftParent, leftCopy, joinDOMParent);
                }

                liveRangeSet = add2Sets(rightCopy, leftCopy);
                frontier.add(joinDOMParent);
                visited.add(currentBasicBlock);
                visited.add(rightParent);
                visited.add(leftParent);
                continue;
            }
            if (isWhileHeaderBlock(currentBasicBlock)) {
                HashSet<Integer> liveRangeSetBeforeWhileHeader = makeCopy(liveRangeSet);
                LinkedList<Instruction> phiInstructions = liveRangesForABlock(currentBasicBlock, liveRangeSet);
                this.phiInstructions.addAll(phiInstructions);
                // Don't remove and add edge to [phi and live range] for first time traversal of phi instructions
                for (Instruction phi : phiInstructions) {
                    addResultToLiveRange(phi.getOperand2(), liveRangeSet);
                }

                BasicBlock dominatingBlock = allDomParents.get(currentBasicBlock);
                List<BasicBlock> parentBlocks = currentBasicBlock.getParents();
                BasicBlock whileHeaderParentApartFromDominator = getWhileBody(parentBlocks, dominatingBlock);
                Set<Integer> whileBodyCopy = makeCopy(liveRangeSet);

                // 1st pass, add only 2nd parameters
                whileBodyCopy = generateInterferenceGraph(
                        whileHeaderParentApartFromDominator,
                        whileBodyCopy,
                        currentBasicBlock);

                // Need to go around while body also twice
                whileBodyCopy = generateInterferenceGraph(
                        whileHeaderParentApartFromDominator,
                        whileBodyCopy,
                        currentBasicBlock);

                liveRangeSet = add2Sets(liveRangeSetBeforeWhileHeader, whileBodyCopy);

                LinkedList<Instruction> phiInstructions2ndPass = liveRangesForABlock(currentBasicBlock, liveRangeSet);
                // Now to remove phi instructions from live range if present and add edges between phi instructions and
                // live ranges
                for (Instruction phi : phiInstructions2ndPass) {
                    if (liveRangeSet.contains(phi.getInstructionId())) {
                        liveRangeSet.remove(phi.getInstructionId());
                    }
                    for (Integer liveInstructionId : liveRangeSet) {
                        adjacencyList.get(phi.getInstructionId()).add(liveInstructionId);
                        if (adjacencyList.containsKey(liveInstructionId)) {
                            adjacencyList.get(liveInstructionId).add(phi.getInstructionId());
                        } else {
                            HashSet<Integer> edges = new HashSet<>(Arrays.asList(phi.getInstructionId()));
                            adjacencyList.put(liveInstructionId, edges);
                        }
                    }
                    addResultToLiveRange(phi.getOperand1(), liveRangeSet);
                }
                frontier.add(dominatingBlock);
                visited.add(currentBasicBlock);
                continue;
            }
            // Ignore the returned phiInstructions as there wont be any
            liveRangesForABlock(currentBasicBlock, liveRangeSet);
            List<BasicBlock> parentBlocks = currentBasicBlock.getParents();
            for (BasicBlock parent : parentBlocks) {
                if (parent != domParent && !visited.contains(parent)) frontier.add(parent);
            }
        }
        return liveRangeSet;
    }

    private LinkedList<Instruction> liveRangesForABlock(BasicBlock basicBlock,
                                                        Set<Integer> liveRangeSet) {
        LinkedList<Instruction> phiInstructions = new LinkedList<>();
        allInstructions.addAll(basicBlock.getInstructions());
        basicBlock.reverseInstructions();
        HashSet<Instruction> deadCodeInstructions = new HashSet<>();
        for (Instruction instruction : basicBlock.getInstructions()) {
            Operation op = instruction.getOperation();
            Integer instructionId = instruction.getInstructionId();

            if (isCondBranchInstruction(op)) {
                Result result = instruction.getOperand1();
                if (!isInstructionResult(result)) {
                    System.err.println("First Operand of COND.BRANCH Result should be of type Instruction");
                    System.exit(43);
                }
                liveRangeSet.add(result.getInstructionId());
                liveRangeNumberToResult.put(result.getInstructionId(), result);
                continue;
            }

            if (isBranchInstruction(op) || isEndInstruction(op)) continue;

//            if (canBeDeadCodeEliminated(op) && !liveRangeSet.contains(instructionId)) {
//                System.out.println("Dead Code Elimination for " + instruction);
//                deadCodeInstructions.add(instruction);
//                continue;
//            }

            if (isWhileHeaderBlock(basicBlock) && !isVisited(basicBlock)) {
                basicBlock.setIsVisitedWhileLiveRangeAnalysis();
            }
            if (isPhiInstruction(instruction)) {
                phiInstructions.add(instruction);
            }
            if (canBeInLiveRangeGraph(op)) {
                if (!adjacencyList.containsKey(instructionId)) {
                    adjacencyList.put(instructionId, new HashSet<>());
                }
                if (!instructionResults.containsKey(instruction)) {
                    System.err.println("this cannot happen sir for instruction id " + instructionId);
                } else {
                    liveRangeNumberToResult.put(instructionId, instructionResults.get(instruction));
                }
            }
            if (!isPhiInstruction(instruction)) {
                if (liveRangeSet.contains(instructionId)) {
                    liveRangeSet.remove(instructionId);
                }
                for (Integer liveInstructionId : liveRangeSet) {
                    if (adjacencyList.containsKey(instructionId)) {
                        adjacencyList.get(instructionId).add(liveInstructionId);
                        if (adjacencyList.containsKey(liveInstructionId)) {
                            adjacencyList.get(liveInstructionId).add(instructionId);
                        } else {
                            HashSet<Integer> edges = new HashSet<>(Arrays.asList(instructionId));
                            adjacencyList.put(liveInstructionId, edges);
                        }
                    }
                }
                addResultToLiveRange(instruction.getOperand1(), liveRangeSet);
                addResultToLiveRange(instruction.getOperand2(), liveRangeSet);
            }
        }
        basicBlock.getInstructions().removeAll(deadCodeInstructions);
        basicBlock.reverseInstructions();
        return phiInstructions;

    }

    private void addResultToLiveRange(Result operand, Set<Integer> liveRangeSet) {
        if (operand == null) return;
        if (isInstructionResult(operand)) {
            liveRangeSet.add(operand.getInstructionId());
            liveRangeNumberToResult.put(operand.getInstructionId(), operand);
        } else if (isVariableResult(operand)) {
            liveRangeSet.add(operand.getSsaVersion());
            liveRangeNumberToResult.put(operand.getSsaVersion(), operand);
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

    private boolean isIfJoin(BasicBlock basicBlock) {
        return basicBlock.getType() == BasicBlock.Type.BB_IF_THEN_JOIN
                || basicBlock.getType() == BasicBlock.Type.BB_IF_ELSE_JOIN;
    }

    private boolean isElseBlock(BasicBlock basicBlock) {
        return basicBlock.getType() == BasicBlock.Type.BB_ELSE;
    }

    private boolean isIfConditionBlock(BasicBlock basicBlock) {
        return basicBlock.getType() == BasicBlock.Type.BB_IF_CONDITION;
    }

    private boolean isIfThenBlock(BasicBlock basicBlock) {
        return basicBlock.getType() == BasicBlock.Type.BB_IF_THEN;
    }

    private boolean isPhiInstruction(Instruction instruction) {
        return instruction.getOperation() == PHI;
    }

    private boolean isVisited(BasicBlock basicBlock) {
        return basicBlock.getIsVisitedWhileLiveRangeAnalysis();
    }

    private HashSet<Integer> makeCopy(Set<Integer> liveRangeSet) {
        HashSet<Integer> copy = new HashSet<>();
        for (Integer instructionId : liveRangeSet) copy.add(instructionId);
        return copy;
    }

    private HashSet<Integer> add2Sets(Set<Integer> set1, Set<Integer> set2) {
        HashSet<Integer> res = new HashSet<>();
        for (Integer instructionId : set1) res.add(instructionId);
        for (Integer instructionId : set2) res.add(instructionId);
        return res;
    }


    public BasicBlock getWhileBody(List<BasicBlock> basicBlocks, BasicBlock dominatingBlock) {
//        System.out.println("Parents for WHILE-HEADER " + basicBlocks.size());
        for (BasicBlock basicBlock : basicBlocks) {
            if (dominatingBlock != basicBlock) return basicBlock;
        }
        return null;
    }

    private boolean canBeInLiveRangeGraph(Operation operation) {
        return !(operation == END || operation == BRA || operation == WRITENL || operation == WRITE);
    }

    private boolean canBeDeadCodeEliminated(Operation operation) {
        return !(operation == RET || operation == END || operation == WRITENL || operation == WRITE);
    }


}
