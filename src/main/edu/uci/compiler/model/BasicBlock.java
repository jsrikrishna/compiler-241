package main.edu.uci.compiler.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by srikrishna on 2/2/17.
 */
public class BasicBlock {
    public enum Type {
        BB_IF_CONDITION, BB_IF_THEN, BB_ELSE, BB_IF_JOIN, BB_IF_ELSE_JOIN, BB_WHILE, BB_WHILE_BODY, BB_WHILE_JOIN,
        BB_RETURN, BB_NORMAL, BB_NONE
    }
    int id;
    Type type;
    static Integer numBasicBlocks = 0;
    List<Instruction> instructions;
    List<BasicBlock> parent;
    List<BasicBlock> children;
    HashMap<String, Integer> localSSATracker;

    public BasicBlock(){
        id = numBasicBlocks;
        type = Type.BB_NONE;
        instructions = new ArrayList<Instruction>();
        parent = new ArrayList<BasicBlock>();
        children = new ArrayList<BasicBlock>();
        localSSATracker = new HashMap<>();
        ++numBasicBlocks;
    }

    public void addInstruction(Instruction instruction){
        instructions.add(instruction);
    }
    public void setInstructions(List<Instruction> instructions){
        this.instructions = instructions;
    }
    public int getId(){
        return this.id;
    }
    public List<BasicBlock> getChildren(){
        return children;
    }
    public List<BasicBlock> getParent(){
        return parent;
    }
    public void setParent(List<BasicBlock> parent){
        this.parent = parent;
    }
    public void setChildren(List<BasicBlock> children){
        this.children = children;
    }
    public void addChildren(BasicBlock children){
        this.children.add(children);
    }
    public void addParent(BasicBlock parent){
        this.parent.add(parent);
    }
    public void setType(Type type){
        this.type = type;
    }
    public Type getType(Type type){
        return this.type;
    }
    public Integer getSSAVersion(String identifier){
        return localSSATracker.get(identifier);
    }
    public void updateLocalSSAVersion(String identifier, Integer instructionId){
        localSSATracker.put(identifier, instructionId);
    }
    @Override
    public String toString(){
        return "Block -> (" + this.id + ", " + this.type + ")";

    }

}
