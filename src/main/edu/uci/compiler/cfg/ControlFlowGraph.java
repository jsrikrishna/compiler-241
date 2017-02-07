package main.edu.uci.compiler.cfg;

import main.edu.uci.compiler.model.BasicBlock;

import java.util.List;

/**
 * Created by srikrishna on 2/2/17.
 */
public class ControlFlowGraph {
    private static ControlFlowGraph instance;
    BasicBlock startBasicBlock;

    private ControlFlowGraph(){
        startBasicBlock = new BasicBlock();
    }
    public BasicBlock getBasicBlock(){
        return startBasicBlock;
    }
    static {
        instance = new ControlFlowGraph();
    }

    public static ControlFlowGraph getInstance() {
        return instance;
    }
}
