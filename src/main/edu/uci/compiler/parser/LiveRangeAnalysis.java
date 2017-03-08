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

    public LiveRangeAnalysis(Set<BasicBlock> endBasicBlocks, HashMap<BasicBlock, BasicBlock> allDomParents) {
        this.endBasicBlocks = endBasicBlocks;
        this.interferenceGraph = new InterferenceGraph();
        this.allDomParents = allDomParents;
        adjacencyList = new HashMap<>();
        this.interferenceGraph.setAdjacencyList(adjacencyList);
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
                LinkedList<Instruction> phiInstructions =
                        liveRangesForABlock(currentBasicBlock, liveRangeSet);

                BasicBlock dominatingBlock = allDomParents.get(currentBasicBlock);
                List<BasicBlock> parentBlocks = currentBasicBlock.getParents();

                BasicBlock ifElseBlock = getIfElse(parentBlocks, dominatingBlock);
                BasicBlock ifThenBlock = getIfThenBlock(parentBlocks);

                HashSet<Integer> ifElseCopy = makeCopy(liveRangeSet);
                if (ifElseBlock != null) {
                    generateForIfRelatedBlocks(ifElseCopy, phiInstructions, dominatingBlock, ifElseBlock);
                } else {
                    for (Instruction phi : phiInstructions) {
                        if (ifElseCopy.contains(phi.getInstructionId())) {
                            ifElseCopy.remove(phi.getInstructionId());
                        }
                        addResultToLiveRange(phi.getOperand2(), ifElseCopy);
                    }
                }

                HashSet<Integer> ifThenCopy = makeCopy(liveRangeSet);
                if (ifThenBlock != null && ifThenBlock != dominatingBlock) {
                    generateForIfRelatedBlocks(ifThenCopy, phiInstructions, dominatingBlock, ifThenBlock);
                }

                liveRangeSet = add2Sets(ifElseCopy, ifThenCopy);
                frontier.add(dominatingBlock);
                visited.add(currentBasicBlock);
                continue;
            }
            if (isWhileHeaderBlock(currentBasicBlock)) {
                HashSet<Integer> liveRangeSetBeforeWhileHeader = makeCopy(liveRangeSet);
                LinkedList<Instruction> phiInstructions = liveRangesForABlock(currentBasicBlock, liveRangeSet);
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

                liveRangeSet = add2Sets(liveRangeSetBeforeWhileHeader, whileBodyCopy);

                LinkedList<Instruction> phiInstructions2ndPass = liveRangesForABlock(currentBasicBlock, liveRangeSet);
                for (Instruction phi : phiInstructions2ndPass) {
                    addResultToLiveRange(phi.getOperand1(), liveRangeSet);
                    if (liveRangeSet.contains(phi.getInstructionId())) {
                        liveRangeSet.remove(phi.getInstructionId());
                    }
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

    private void generateForIfRelatedBlocks(Set<Integer> copy,
                                            LinkedList<Instruction> phiInstructions,
                                            BasicBlock dominatingBlock,
                                            BasicBlock ifTypeBlock) {
        for (Instruction phi : phiInstructions) {
            if (copy.contains(phi.getInstructionId())) {
                copy.remove(phi.getInstructionId());
            }
            if (isElseBlock(ifTypeBlock)) {
                addResultToLiveRange(phi.getOperand2(), copy);
            }
            if (isIfThenBlock(ifTypeBlock)) {
                addResultToLiveRange(phi.getOperand1(), copy);
            }

        }
        generateInterferenceGraph(ifTypeBlock, copy, dominatingBlock);
    }

    private LinkedList<Instruction> liveRangesForABlock(BasicBlock basicBlock,
                                                        Set<Integer> liveRangeSet) {
        LinkedList<Instruction> phiInstructions = new LinkedList<>();
        basicBlock.reverseInstructions();
        for (Instruction instruction : basicBlock.getInstructions()) {
            Operation op = instruction.getOperation();

            if (isCondBranchInstruction(op)) {
                Result result = instruction.getOperand1();
                if (!isInstructionResult(result)) {
                    System.err.println("First Operand of COND.BRANCH Result should be of type Instruction");
                    System.exit(43);
                }
                liveRangeSet.add(result.getInstructionId());
                continue;
            }

            if (isBranchInstruction(op) || isEndInstruction(op)) continue;

            Integer instructionId = instruction.getInstructionId();

            if (isWhileHeaderBlock(basicBlock) && !isVisited(basicBlock)) {
                basicBlock.setIsVisitedWhileLiveRangeAnalysis();
            }

            if (isPhiInstruction(instruction)) {
                phiInstructions.add(instruction);
                continue;
            }

            if (liveRangeSet.contains(instructionId)) {
                liveRangeSet.remove(instructionId);
            }
            if (canBeInLiveRangeGraph(op)) {
                adjacencyList.put(instructionId, new HashSet<>());
            }

            for (Integer liveInstructionId : liveRangeSet) {
                if (adjacencyList.containsKey(instructionId)) {
                    adjacencyList.get(instructionId).add(liveInstructionId);
                }
            }

            addResultToLiveRange(instruction.getOperand1(), liveRangeSet);
            addResultToLiveRange(instruction.getOperand2(), liveRangeSet);
        }
        basicBlock.reverseInstructions();
        return phiInstructions;

    }

    private void addResultToLiveRange(Result operand, Set<Integer> liveRangeSet) {
        if (operand == null) return;
        if (isInstructionResult(operand)) {
            liveRangeSet.add(operand.getInstructionId());
            return;
        }
        if (isVariableResult(operand)) {
            liveRangeSet.add(operand.getSsaVersion());
            return;
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

    private BasicBlock getIfElse(List<BasicBlock> basicBlocks, BasicBlock dominatingBlock) {
//        System.out.println("Parents for IF-JOIN " + basicBlocks.size());
        for (BasicBlock basicBlock : basicBlocks) {
            if (isElseBlock(basicBlock) && basicBlock != dominatingBlock) return basicBlock;
        }
        return null;
    }

    private BasicBlock getIfThenBlock(List<BasicBlock> basicBlocks) {
//        System.out.println("Parents for IF-JOIN " + basicBlocks.size());
        for (BasicBlock basicBlock : basicBlocks) {
            if (isIfThenBlock(basicBlock)) return basicBlock;
        }
        return null;
    }

    private BasicBlock getWhileBody(List<BasicBlock> basicBlocks, BasicBlock dominatingBlock) {
//        System.out.println("Parents for WHILE-HEADER " + basicBlocks.size());
        for (BasicBlock basicBlock : basicBlocks) {
            if (dominatingBlock != basicBlock) return basicBlock;
        }
        return null;
    }

    public List<String> writeAdjList() {
        List<String> adjListDigraph = new ArrayList<>();
        adjListDigraph.add("digraph{");
        for (Map.Entry<Integer, HashSet<Integer>> entry : this.adjacencyList.entrySet()) {
            Integer key = entry.getKey();
            HashSet<Integer> values = entry.getValue();
            for (Integer instructionId : values) {
                adjListDigraph.add(instructionId + " -> " + key);
            }
            if (values == null || values.isEmpty()) {
                System.err.println(key + " has no edges");
//                adjListDigraph.add(key.toString());
            }
        }
        adjListDigraph.add("}");
        return adjListDigraph;
    }

    private boolean canBeInLiveRangeGraph(Operation operation) {
        return !(operation == END
                || operation == PHI
                || operation == BRA
                || operation == WRITE
                || operation == WRITENL);
    }


}
