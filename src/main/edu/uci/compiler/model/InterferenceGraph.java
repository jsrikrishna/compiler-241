package main.edu.uci.compiler.model;

import java.util.*;

/**
 * Created by srikrishna on 3/5/17.
 */
public class InterferenceGraph {
    private HashMap<Integer, HashSet<Integer>> adjacencyList;
    private HashMap<Integer, Result> liveRangeNumberToResult;
    private LinkedList<Instruction> phiInstructions;
    private Set<Instruction> allInstructions;
    private HashMap<Integer, String> colors = new HashMap<>();

    public InterferenceGraph(HashMap<Integer, HashSet<Integer>> adjacencyList,
                             HashMap<Integer, Result> liveRangeNumberToResult,
                             LinkedList<Instruction> phiInstructions,
                             Set<Instruction> allInstructions) {
        this.adjacencyList = adjacencyList;
        this.liveRangeNumberToResult = liveRangeNumberToResult;
        this.phiInstructions = phiInstructions;
        this.allInstructions = allInstructions;
        colors.put(1, "red");
        colors.put(2, "green");
        colors.put(3, "blue");
        colors.put(4, "cyan");
        colors.put(5, "magenta");
        colors.put(6, "yellow");
        colors.put(7, "sienna");
        colors.put(8, "lavender");
        colors.put(9, "lightgray");
        colors.put(10, "orange");
        colors.put(11, "hotpink");
        colors.put(12, "maroon1");

    }



    public HashMap<Integer, Result> getLiveRangeNumberToResult() {
        return this.liveRangeNumberToResult;
    }

    public HashMap<Integer, HashSet<Integer>> getAdjacencyList() {
        return this.adjacencyList;
    }

    public LinkedList<Instruction> getPhiInstructions() {
        return this.phiInstructions;
    }

    public void reversePhiInstructions() {
        Collections.reverse(this.phiInstructions);
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
        if (neighbors != null) {
            for (Integer neighbor : neighbors) {
                if (adjacencyList.containsKey(neighbor)) {
                    adjacencyList.get(neighbor).add(node);
                }
            }
            adjacencyList.put(node, neighbors);
        }
    }

    public List<String> writeAdjList() {
        List<String> adjListDigraph = new ArrayList<>();
        adjListDigraph.add("strict graph{");
        for (Map.Entry<Integer, HashSet<Integer>> entry : this.adjacencyList.entrySet()) {
            Integer key = entry.getKey();
            HashSet<Integer> values = entry.getValue();
            for (Integer instructionId : values) {
                adjListDigraph.add(instructionId + " -- " + key);
            }
            if (values == null || values.isEmpty()) {
                System.err.println(key + " has no edges");
//                adjListDigraph.add(key.toString());
            }
        }
        adjListDigraph.add("}");
        return adjListDigraph;
    }

    public List<String> writeAdjListWithCluster(HashMap<Integer, List<Integer>> clusterResults,
                                                HashMap<Result, Integer> registerForResults) {
        List<String> adjListDigraph = new ArrayList<>();
        List<String> edges = new ArrayList<>();
        adjListDigraph.add("strict graph{");
        for (Map.Entry<Integer, HashSet<Integer>> entry : this.adjacencyList.entrySet()) {
            Integer key = entry.getKey();
            HashSet<Integer> values = entry.getValue();
            for (Integer instructionId : values) {
                String cluster1 = instructionId + " [label=\"" + instructionId;
                if(clusterResults.containsKey(instructionId)){
                    for(Integer node: clusterResults.get(instructionId)){
                        cluster1 += ", " + node;
                    }

                }
                String color = colors.get(registerForResults.get(liveRangeNumberToResult.get(instructionId)));
                cluster1 += "\",color="+color+"];";
                String cluster2 = key + " [label=\"" + key;
                if(clusterResults.containsKey(key)){
                    for(Integer node: clusterResults.get(key)){
                        cluster2 += ", " + node;
                    }
                }
                String color2 = colors.get(registerForResults.get(liveRangeNumberToResult.get(key)));
                cluster2 += "\",color="+color2+"];";
                adjListDigraph.add(cluster1);
                adjListDigraph.add(cluster2);
                edges.add(instructionId + "--" + key + ";");
            }
            if (values == null || values.isEmpty()) {
                System.err.println(key + " has no edges");
//                adjListDigraph.add(key.toString());
            }
        }
        adjListDigraph.addAll(edges);
        adjListDigraph.add("}");
        return adjListDigraph;
    }
}
