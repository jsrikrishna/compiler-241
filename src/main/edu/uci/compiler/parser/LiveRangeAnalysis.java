package main.edu.uci.compiler.parser;

import main.edu.uci.compiler.model.BasicBlock;
import main.edu.uci.compiler.model.InterferenceGraph;
import main.edu.uci.compiler.model.Result;

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
    private HashMap<Result, HashSet<Result>> adjacencyList;

    public LiveRangeAnalysis(Set<BasicBlock> endBasicBlocks) {
        this.liveRangeSet = new HashSet<>();
        this.endBasicBlocks = endBasicBlocks;
        this.interferenceGraph = new InterferenceGraph();
        adjacencyList = new HashMap<>();
        this.interferenceGraph.setAdjacencyList(adjacencyList);
    }

    public void generateInterferenceGraph() {

    }

    public void printParentsForProgram(String fileName) {
        System.out.println("Number of End Basic Blocks are " + this.endBasicBlocks.size());
        List<String> parentDigraph = new ArrayList<>();
        parentDigraph.add("digraph{");
        for (BasicBlock basicBlock : this.endBasicBlocks) {
            printParentsForEndBlock(basicBlock, parentDigraph);
        }
        parentDigraph.add("}");
        generateParentFlow(fileName, parentDigraph);
    }

    private void printParentsForEndBlock(BasicBlock endBasicBlock, List<String> parentDigraph) {
        Set<BasicBlock> visited = new HashSet<>();
        Queue<BasicBlock> frontier = new LinkedList<>();
        frontier.add(endBasicBlock);
        while (!frontier.isEmpty()) {
            BasicBlock currentBasicBlock = frontier.poll();
            visited.add(currentBasicBlock);
            if(currentBasicBlock.getParent().isEmpty()){
                parentDigraph.add("BasicBlock" + currentBasicBlock.getId());
            }
            for (BasicBlock parent : currentBasicBlock.getParent()) {
                parentDigraph.add("BasicBlock" + parent.getId() + " -> BasicBlock" + currentBasicBlock.getId());
                if (!visited.contains(parent)) {
                    frontier.add(parent);
                }
            }
        }
    }

    private void generateParentFlow(String fileName, List<String> domDigraph) {
        Writer writer = null;
        try {
            String newFileName = fileName.substring(0, fileName.length() - 4) + "parent.dot";
            String parentPng = fileName.substring(0, fileName.length() - 4) + "parent.png";
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newFileName), "utf-8"));
            for (String str : domDigraph) {
                writer.write(str + "\n");
            }
            Runtime.getRuntime().exec("dot -Tpng " + newFileName + " -o " + parentPng);
            System.out.println("Generated Parent Flows " + parentPng);
        } catch (Exception ex) {
            System.err.print("Error occured while writing DOM Data to file");
        } finally {
            try {
                writer.close();
            } catch (Exception ex) {
                System.err.println("Error while closing writer and exiting");
            }
        }

    }

}
