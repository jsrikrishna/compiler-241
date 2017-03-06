package main.edu.uci.compiler.parser;

import main.edu.uci.compiler.cfg.ControlFlowGraph;
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



}
