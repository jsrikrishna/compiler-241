package main.edu.uci.compiler.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by srikrishna on 3/5/17.
 */
public class InterferenceGraph {
    private HashSet<Instruction> instructions;
    private HashSet<Instruction> phiInstructions;
    private HashMap<Result, HashSet<Result>> adjacencyList;


    public void setInstructions(HashSet<Instruction> instructions){
        this.instructions = instructions;
    }
    public HashSet<Instruction> getInstructions(){
        return this.instructions;
    }
    public void setPhiInstructions(HashSet<Instruction> phiInstructions){
        this.phiInstructions = phiInstructions;
    }
    public HashSet<Instruction> getPhiInstructions(){
        return this.phiInstructions;
    }
    public void setAdjacencyList(HashMap<Result, HashSet<Result>> adjacencyList){
        this.adjacencyList = adjacencyList;
    }
    public HashMap<Result, HashSet<Result>> getAdjacencyList(){
        return this.adjacencyList;
    }
}
