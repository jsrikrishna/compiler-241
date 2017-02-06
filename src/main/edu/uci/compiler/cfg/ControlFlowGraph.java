package main.edu.uci.compiler.cfg;

import main.edu.uci.compiler.model.BasicBlock;

import java.util.List;

/**
 * Created by srikrishna on 2/2/17.
 */
public class ControlFlowGraph {
    BasicBlock startBasicBlock;
    public ControlFlowGraph(){
        startBasicBlock = new BasicBlock();
    }
    public BasicBlock getBasicBlock(){
        return startBasicBlock;
    }
}
