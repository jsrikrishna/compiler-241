package main.edu.uci.compiler.cfg;

import main.edu.uci.compiler.model.BasicBlock;

/**
 * Created by srikrishna on 2/2/17.
 */
public class ControlFlowGraph {
    BasicBlock basicBlock;
    public ControlFlowGraph(){
        basicBlock = new BasicBlock();
    }
    public BasicBlock getBasicBlock(){
        return basicBlock;
    }
    public void setBasicBlock(BasicBlock basicBlock){
        this.basicBlock = basicBlock;
    }
}
