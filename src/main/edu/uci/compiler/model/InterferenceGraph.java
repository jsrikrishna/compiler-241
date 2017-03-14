package main.edu.uci.compiler.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by srikrishna on 3/5/17.
 */
public class InterferenceGraph {
    private HashMap<Integer, HashSet<Integer>> adjacencyList;
    private HashMap<Integer, Result> liveRangeNumberToResult;
    private Set<Instruction> phiInstructions;
    private Set<Instruction> allInstructions;

    public InterferenceGraph(HashMap<Integer, HashSet<Integer>> adjacencyList,
                             HashMap<Integer, Result> liveRangeNumberToResult,
                             Set<Instruction> phiInstructions,
                             Set<Instruction> allInstructions) {
        this.adjacencyList = adjacencyList;
        this.liveRangeNumberToResult = liveRangeNumberToResult;
        this.phiInstructions = phiInstructions;
        this.allInstructions = allInstructions;
    }

    public HashMap<Integer, Result> getLiveRangeNumberToResult() {
        return this.liveRangeNumberToResult;
    }

    public HashMap<Integer, HashSet<Integer>> getAdjacencyList() {
        return this.adjacencyList;
    }

    public Set<Instruction> getPhiInstructions() {
        return this.phiInstructions;
    }

    public Set<Instruction> getAllInstructions() {
        return this.allInstructions;
    }

    public HashSet<Integer> getNeighbors(Integer node) {
        return adjacencyList.get(node);
    }

    public void removeNode(Integer node) {
        HashSet<Integer> neighbors = getNeighbors(node);
        if (neighbors != null) {
            for (Integer neighbor : neighbors) {
                if (adjacencyList.containsKey(neighbor)) {
                    adjacencyList.get(neighbor).remove(node);
                }
            }
        }
        adjacencyList.remove(node);
    }

    public boolean isEmpty() {
        return adjacencyList.isEmpty();
    }

    public void addNodeBack(Integer node, HashSet<Integer> neighbors) {
        if(neighbors != null){
            for (Integer neighbor : neighbors) {
                if (adjacencyList.containsKey(neighbor)) {
                    adjacencyList.get(neighbor).add(node);
                }
            }
            adjacencyList.put(node, neighbors);
        }

    }
}
