package main.edu.uci.compiler.parser;

import main.edu.uci.compiler.cfg.ControlFlowGraph;
import main.edu.uci.compiler.model.Instruction;
import main.edu.uci.compiler.model.InterferenceGraph;
import main.edu.uci.compiler.model.Result;

import static main.edu.uci.compiler.model.Result.KIND.*;

import java.util.*;

/**
 * Created by srikrishna on 3/12/17.
 */
public class RegisterAllocator {
    private InterferenceGraph interferenceGraph;
    private static final int RegisterCount = 8; // Architecture Dependent
    private static final int MaxRegisters = 65536;
    public HashMap<Integer, Integer> colors; // colors map to register numbers
    private HashMap<Integer, Integer> registerForResults;
    private HashMap<Integer, HashSet<Integer>> adjacencyList;
    private HashMap<Integer, List<Integer>> clusterResults;
    private ControlFlowGraph cfg;

    public RegisterAllocator(InterferenceGraph interferenceGraph,
                             ControlFlowGraph cfg,
                             HashMap<Integer, Integer> registerForResults) {
        this.interferenceGraph = interferenceGraph;
        this.cfg = cfg;
        this.adjacencyList = interferenceGraph.getAdjacencyList();
        colors = new HashMap<>();
        this.registerForResults = registerForResults;
        clusterResults = new HashMap<>();
    }

    private void setClusterResults(HashMap<Integer, List<Integer>> clusterResults) {
        this.clusterResults = clusterResults;
    }

    private HashMap<Integer, List<Integer>> getClusterResults() {
        return this.clusterResults;
    }

    public void allocateRegister(String fileName) {
//        preProcessAdjacencyList();
        clusterPhiInstructions();
        colorInterferenceGraph();
        generateColoredGraph(fileName);
        int maxRegisterNumber = 0;
        for (Map.Entry<Integer, Integer> entry : registerForResults.entrySet()) {
            if (entry.getValue() > maxRegisterNumber) {
                maxRegisterNumber = entry.getValue();
            }
        }
        System.out.println("Total Number of Registers needed are " + maxRegisterNumber);
    }

    private void generateColoredGraph(String fileName) {
        List<String> adjListStrictGraph =
                interferenceGraph.writeAdjListWithCluster(getClusterResults(), registerForResults);
        cfg.generateFlow(fileName, adjListStrictGraph, "color");
    }

    private void preProcessAdjacencyList() {
        Iterator it = adjacencyList.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, HashSet<Integer>> pair = (Map.Entry) it.next();
            if (pair.getValue().size() == 0) it.remove();
        }
    }

    private void colorInterferenceGraph() {
        if (interferenceGraph.isEmpty()) return;
        Integer node = getNode();
        HashSet<Integer> neighbors = interferenceGraph.getNeighbors(node);
        interferenceGraph.removeNode(node);
        if (!interferenceGraph.isEmpty()) {
            colorInterferenceGraph();
        }
        interferenceGraph.addNodeBack(node, neighbors);
        chooseColor(node, neighbors);
    }

    private Integer getNode() {
        int minNeighbors = RegisterCount + 1;
        for (Map.Entry<Integer, HashSet<Integer>> entry : adjacencyList.entrySet()) {
            if (minNeighbors > entry.getValue().size()) {
                Integer node = entry.getKey();
                minNeighbors = entry.getValue().size();
                return node;
            }
        }
        minNeighbors = adjacencyList.size() + 1;
        for (Map.Entry<Integer, HashSet<Integer>> entry : adjacencyList.entrySet()) {
            int size = Integer.MIN_VALUE;
            if (minNeighbors > size) {
                Integer node = entry.getKey();
                minNeighbors = size;
                return node;
            }
        }
        System.out.println("Hello i am returning null");
        return null;
    }

    private void chooseColor(Integer node, HashSet<Integer> neighbors) {
        Set<Integer> colorsUsed = new HashSet<>();
        colorsUsed.add(0);
        for (Integer neighborNode : neighbors) {
            Integer regNo = registerForResults.get(neighborNode);
            colorsUsed.add(regNo);
        }
        for (Integer color = 0; color < MaxRegisters; color++) {
            if (!colorsUsed.contains(color)) {
                registerForResults.put(node, color);
                List<Integer> cluster = clusterResults.get(node);
                if (cluster != null && cluster.size() > 0) {
                    for (Integer clusterNodes : cluster) {
                        registerForResults.put(clusterNodes, color);
                    }
                }
                break;
            }
        }
    }

    private void clusterPhiInstructions() {
        // Need to reverse,
        // because phi instructions are added in reverse in LiveRangeAnalysis, as it performed in reverse order
        interferenceGraph.reversePhiInstructions();
        LinkedList<Instruction> phiInstructions = interferenceGraph.getPhiInstructions();
        for (Instruction phi : phiInstructions) {
            Result phiResult = phi.getOperand3();
            Result leftResult = phi.getOperand1();
            Result rightResult = phi.getOperand2();

            List<Integer> cluster = new LinkedList<>();
            Integer lrNumberForPhi = getLiveRangeNumber(phiResult);
            cluster.add(lrNumberForPhi);
            checkAndAddToCluster(leftResult, cluster);
            checkAndAddToCluster(rightResult, cluster);
            cluster.remove(lrNumberForPhi);
            if (!cluster.isEmpty()) {
                if (clusterResults.containsKey(lrNumberForPhi)) {
                    clusterResults.get(lrNumberForPhi).addAll(cluster);
                } else {
                    clusterResults.put(lrNumberForPhi, cluster);

                }
            }
        }
        System.out.println("Cluster Instructions Before Union/Find " + clusterResults);
        updateAdjacencyList();
        System.out.println("Cluster Instructions After Union/Find " + clusterResults);
        System.out.println();
    }

    private void checkAndAddToCluster(Result phiOperandResult, List<Integer> cluster) {
        if (phiOperandResult.getKind() != Result.KIND.CONSTANT) {
            Integer lrNumber = getLiveRangeNumber(phiOperandResult);
            HashSet<Integer> neighbors = adjacencyList.get(lrNumber);
            boolean doesInterfere = false;
            if (neighbors == null) {
                cluster.add(lrNumber);
                return;
            }
            for (Integer clusterNode : cluster) {

                if (neighbors.contains(clusterNode)) {
                    doesInterfere = true;
                    break;
                }
            }
            if (!doesInterfere) {
                cluster.add(lrNumber);
            }
        }
    }

    private Integer getLiveRangeNumber(Result result) {
        if (result.getKind() == Result.KIND.INSTRUCTION) {
            return result.getInstructionId();
        }
        if (result.getKind() == Result.KIND.VARIABLE) {
            return result.getSsaVersion();
        }
        return null;
    }

    private void updateAdjacencyList() {
        unionFind();
        HashMap<Integer, List<Integer>> newClusterResult = getClusterResults();
        for (Map.Entry<Integer, List<Integer>> entry : newClusterResult.entrySet()) {
            List<Integer> clusteredResults = entry.getValue();
            for (Integer clusterResult : clusteredResults) {
                HashSet<Integer> neighbors = adjacencyList.get(clusterResult);
                if (neighbors != null) {
                    if (adjacencyList.containsKey(entry.getKey())) {
                        adjacencyList.get(entry.getKey()).addAll(neighbors);
                    } else {
                        adjacencyList.put(entry.getKey(), new HashSet<>(neighbors));
                    }
                    for (Integer neighbor : neighbors) {
                        adjacencyList.get(neighbor).add(entry.getKey());
                    }
                }
                adjacencyList.remove(clusterResult);
                for (Map.Entry<Integer, HashSet<Integer>> adjEntry : adjacencyList.entrySet()) {
                    adjEntry.getValue().remove(clusterResult);
                }
            }
        }
    }

    private void unionFind() {
        Map<Integer, List<Integer>> union = new HashMap<>();
        Map<Integer, Integer> find = new HashMap<>();
        int groupId = 1; // Represent set id

        for (Map.Entry<Integer, List<Integer>> entry : clusterResults.entrySet()) {

            Integer node = entry.getKey();
            List<Integer> clusteredNodes = entry.getValue();

            if (clusteredNodes.size() == 1) {
                Integer clusteredNode = clusteredNodes.get(0);
                Integer setId1 = find.get(node);
                Integer setId2 = find.get(clusteredNode);
                if (setId1 == null && setId2 == null) {
                    List<Integer> newGroup = new LinkedList<>(Arrays.asList(node, clusteredNode));
                    union.put(groupId, newGroup);
                    find.put(node, groupId);
                    find.put(clusteredNode, groupId);
                    ++groupId;
                    continue;
                }
                if (setId1 == null) {
                    union.get(setId2).add(node);
                    find.put(node, setId2);
                    continue;
                }
                if (setId2 == null) {
                    union.get(setId1).add(clusteredNode);
                    find.put(clusteredNode, setId1);
                    continue;
                }
                if (!(setId1.intValue() == setId2.intValue())) {
                    merge(setId1, setId1, groupId, union, find);
                    ++groupId;
                }
            } else {
                Integer clusteredNode1 = clusteredNodes.get(0);
                Integer clusteredNode2 = clusteredNodes.get(1);
                Integer set1Id = find.get(node);
                Integer set2Id = find.get(clusteredNode1);
                Integer set3Id = find.get(clusteredNode2);
                if (set1Id == null && set2Id == null && set3Id == null) {
                    List<Integer> newGroup = new LinkedList<>(Arrays.asList(node, clusteredNode1, clusteredNode2));
                    union.put(groupId, newGroup);
                    for (Integer newNode : newGroup) {
                        find.put(newNode, groupId);
                    }
                    ++groupId;
                    continue;
                }
                if (set1Id == null && set2Id == null) {
                    find.put(node, set3Id);
                    find.put(clusteredNode1, set3Id);
                    List<Integer> newGroup = new LinkedList<>(Arrays.asList(node, clusteredNode1));
                    union.get(set3Id).addAll(newGroup);
                    ++groupId;
                    continue;
                }
                if (set1Id == null && set3Id == null) {
                    find.put(node, set2Id);
                    find.put(clusteredNode2, set2Id);
                    List<Integer> newGroup = new LinkedList<>(Arrays.asList(node, clusteredNode2));
                    union.get(set2Id).addAll(newGroup);
                    ++groupId;
                    continue;
                }
                if (set1Id == null) {
                    union.get(set2Id).add(node);
                    find.put(node, set2Id);
                    if (!(set2Id.intValue() == set3Id.intValue())) {
                        merge(set2Id, set3Id, groupId, union, find);
                        ++groupId;
                    }
                    continue;
                }
                if (set2Id == null && set3Id == null) {
                    find.put(clusteredNode1, set1Id);
                    find.put(clusteredNode2, set1Id);
                    List<Integer> newGroup = new LinkedList<>(Arrays.asList(clusteredNode1, clusteredNode2));
                    union.get(set1Id).addAll(newGroup);
                    ++groupId;
                    continue;
                }
                if (set2Id == null) {
                    union.get(set1Id).add(clusteredNode1);
                    find.put(clusteredNode1, set1Id);
                    if (!(set1Id.intValue() == set3Id.intValue())) {
                        merge(set1Id, set3Id, groupId, union, find);
                        ++groupId;
                    }
                    continue;
                }
                if (set3Id == null) {
                    union.get(set1Id).add(clusteredNode2);
                    find.put(clusteredNode2, set1Id);
                    if (!(set1Id.intValue() == set2Id.intValue())) {
                        merge(set1Id, set2Id, groupId, union, find);
                        ++groupId;
                    }
                    continue;
                }
                if (!(set1Id.intValue() == set2Id.intValue())) {
                    merge(set1Id, set2Id, groupId, union, find);
                    ++groupId;
                }
                Integer updatedGroupId = find.get(node);
                if (!(updatedGroupId.intValue() == set3Id.intValue())) {
                    merge(updatedGroupId, set3Id, groupId, union, find);
                    ++groupId;
                }
            }
        }
        HashMap<Integer, List<Integer>> updatedClusterResults = new HashMap<>();
        for (Map.Entry<Integer, List<Integer>> entry : union.entrySet()) {
            List<Integer> clusteredNodes = entry.getValue();
            Integer parentNode = clusteredNodes.get(0);
            clusteredNodes.remove(0);
            updatedClusterResults.put(parentNode, clusteredNodes);
        }
        setClusterResults(updatedClusterResults);
    }

    private void merge(Integer group1,
                       Integer group2,
                       Integer groupId,
                       Map<Integer, List<Integer>> union,
                       Map<Integer, Integer> find) {
        List<Integer> newGroup = new LinkedList<>();
        if (union.containsKey(group1)) {
            newGroup.addAll(union.get(group1));
            union.remove(group1);
        }
        if (union.containsKey(group2)) {
            newGroup.addAll(union.get(group2));
            union.remove(group2);
        }
        union.put(groupId, newGroup);
        for (Integer node : newGroup) {
            find.put(node, groupId);
        }
    }

    public void mapToRegisters() {
        for (Instruction instruction : interferenceGraph.getAllInstructions()) {
            Integer instructionId = instruction.getInstructionId();
            Result operand1 = instruction.getOperand1();
            Result operand2 = instruction.getOperand2();
            Result operand3 = instruction.getOperand3();
            mapResultToRegister(operand1);
            mapResultToRegister(operand2);
            mapResultToRegister(operand3);
            if (registerForResults.containsKey(instructionId)) {
                instruction.setRegisterNumber(registerForResults.get(instructionId));
            }
        }
    }

    private void mapResultToRegister(Result result) {
        if (result != null && result.getKind() != CONSTANT) {
            if (result.getKind() == VARIABLE) {
                result.setRegisterNumber(registerForResults.get(result.getSsaVersion()));
                result.setKind(REGISTER);
            }
            if (result.getKind() == INSTRUCTION) {
                result.setRegisterNumber(registerForResults.get(result.getInstructionId()));
                result.setKind(REGISTER);
            }
        }
    }
}
