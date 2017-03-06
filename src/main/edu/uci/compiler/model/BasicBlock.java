package main.edu.uci.compiler.model;

import java.util.*;

/**
 * Created by srikrishna on 2/2/17.
 */
public class BasicBlock {
    public enum Type {
        BB_IF_CONDITION, BB_IF_THEN, BB_ELSE, BB_IF_THEN_JOIN, BB_IF_ELSE_JOIN,
        BB_WHILE_CONDITION_AND_JOIN, BB_WHILE_BODY, BB_WHILE_FALL_THROUGH,
        BB_RETURN, BB_NORMAL, BB_NONE,
        BB_FUNCTION,
        BB_MAIN;
    }

    int id;
    Type type;
    static Integer numBasicBlocks = 0;
    static LinkedList<BasicBlock> allBasicBlocks = new LinkedList<>();
    LinkedList<Instruction> instructions;
    List<BasicBlock> parent;
    List<BasicBlock> children;
    HashMap<String, Integer> localTracker; // Local SSA Tracker
    ArrayList<Function> functionsCalled; // functions called from basic blocks
    HashMap<Operation, Instruction> anchor;
    boolean isRootBasicBlock; // used in dominance relationships
    boolean isVisited;
    boolean isVisitedWhileLiveRangeAnalysis;

    public BasicBlock(Type type) {
        id = numBasicBlocks;
        this.type = type;
        instructions = new LinkedList<>();
        parent = new ArrayList<BasicBlock>();
        children = new ArrayList<BasicBlock>();
        anchor = null; // Will be set during common subexpression elimination
        localTracker = new HashMap<>();
        functionsCalled = new ArrayList<>();
        this.isRootBasicBlock = false;
        this.isVisitedWhileLiveRangeAnalysis = false;
        this.isVisited = false;
        allBasicBlocks.add(this);
        ++numBasicBlocks;
    }

    public void addInstructionToAnchor(Instruction instruction) {
        anchor.put(instruction.getOperation(), instruction);
    }

    public void setAnchor(HashMap<Operation, Instruction> anchor) {
        this.anchor = anchor;
    }

    public HashMap<Operation, Instruction> getAnchor() {
        return this.anchor;
    }

    public void addInstruction(Instruction instruction) {
        this.instructions.add(instruction);
    }

    public void addInstructionAtStart(Instruction instruction) {
        this.instructions.add(0, instruction);
    }

    public void setInstructions(LinkedList<Instruction> instructions) {
        this.instructions = instructions;
    }

    public LinkedList<Instruction> getInstructions() {
        return this.instructions;
    }

    public int getId() {
        return this.id;
    }

    public List<BasicBlock> getChildren() {
        return children;
    }

    public List<BasicBlock> getParent() {
        return parent;
    }

    public void setParent(List<BasicBlock> parent) {
        this.parent = parent;
    }

    public void setChildren(List<BasicBlock> children) {
        this.children = children;
    }

    public void addChildrenAndUpdateChildrenTracker(BasicBlock children) {
        this.children.add(children);
        if (children.getLocalTracker().isEmpty()) {
            children.setLocalTracker(this.getCopyOfVariableTracker());
        }
    }

    public void addParent(BasicBlock parent) {
        this.parent.add(parent);
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Type getType() {
        return this.type;
    }

    public Integer getSSAVersion(String identifier) {
        return localTracker.get(identifier);
    }

    public void updateSSAVersion(String identifier, Integer instructionId) {
        localTracker.put(identifier, instructionId);
    }

    public void setLocalTracker(HashMap<String, Integer> globalTracker) {
        this.localTracker = globalTracker;
    }

    public HashMap<String, Integer> getLocalTracker() {
        return this.localTracker;
    }

    public void addFunctionCalled(Function function) {
        this.functionsCalled.add(function);
    }

    public ArrayList<Function> getFunctionCalled() {
        return this.functionsCalled;
    }

    public boolean isVisited() {
        return this.isVisited;
    }

    public void setIsVisited() {
        this.isVisited = true;
    }

    public void setIsVisitedWhileLiveRangeAnalysis() {
        this.isVisitedWhileLiveRangeAnalysis = !this.isVisitedWhileLiveRangeAnalysis;
    }

    public boolean getIsVisitedWhileLiveRangeAnalysis() {
        return this.isVisitedWhileLiveRangeAnalysis;
    }

    public void setIsRootBasicBlock() {
        this.isRootBasicBlock = true;
    }

    public boolean isRootBasicBlock() {
        return this.isRootBasicBlock;
    }

    public HashMap<String, Integer> getCopyOfVariableTracker() {
        HashMap<String, Integer> copy = new HashMap<>();
        for (Map.Entry<String, Integer> entry : this.localTracker.entrySet()) {
            copy.put(entry.getKey(), entry.getValue());
        }
        return copy;
    }

    public LinkedList<BasicBlock> getListOfAllBasicBlocks() {
        return this.allBasicBlocks;
    }

    @Override
    public String toString() {
        return "Block -> (" + this.id + ", " + this.type + ")";
    }

}
