package main.edu.uci.compiler.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by srikrishna on 2/2/17.
 */
public class BasicBlock {
    public enum Type {
        BB_IF_CONDITION, BB_IF_THEN, BB_ELSE, BB_IF_THEN_JOIN, BB_IF_ELSE_JOIN,
        BB_WHILE_CONDITION, BB_WHILE_BODY, BB_WHILE_JOIN,
        BB_RETURN, BB_NORMAL, BB_NONE,
        BB_FUNCTION,
        BB_MAIN;
    }

    int id;
    Type type;
    static Integer numBasicBlocks = 0;
    LinkedList<Instruction> instructions;
    List<BasicBlock> parent;
    List<BasicBlock> children;
    HashMap<String, Integer> localTracker; // Local SSA Tracker
    ArrayList<Function> functionsCalled;
    boolean isVisited;

    public BasicBlock(Type type) {
        id = numBasicBlocks;
        this.type = type;
        instructions = new LinkedList<>();
        parent = new ArrayList<BasicBlock>();
        children = new ArrayList<BasicBlock>();
        localTracker = new HashMap<>();
        functionsCalled = new ArrayList<>();
        this.isVisited = false;
        ++numBasicBlocks;
    }

    public void addInstruction(Instruction instruction) {
        this.instructions.add(instruction);
    }

    public void setInstructions(LinkedList<Instruction> instructions) {
        this.instructions = instructions;
    }

    public List<Instruction> getInstructions() {
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

    public void addChildren(BasicBlock children) {
        this.children.add(children);
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

    @Override
    public String toString() {
        return "Block -> (" + this.id + ", " + this.type + ")";
    }

}
