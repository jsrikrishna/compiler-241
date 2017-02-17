package main.edu.uci.compiler.cfg;

import main.edu.uci.compiler.model.BasicBlock;
import main.edu.uci.compiler.model.Function;
import main.edu.uci.compiler.model.Instruction;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by srikrishna on 2/2/17.
 */
public class ControlFlowGraph {
    BasicBlock startBasicBlock;

    public void setStartBasicBlock(BasicBlock basicBlock){
        this.startBasicBlock = basicBlock;
    }

    public BasicBlock getBasicBlock() {
        return startBasicBlock;
    }

    public void printBasicBlocks(BasicBlock basicBlock) {
        if (basicBlock == null) return;
        if (basicBlock.isVisited()) return;
        basicBlock.setIsVisited();
        System.out.println("Basic Block " + basicBlock.getId() + " type is " + basicBlock.getType());
        if(basicBlock.getInstructions().size() == 0){
            System.out.println("No Instructions in this basic block");
        }
        for(Instruction instruction: basicBlock.getInstructions()) {
            System.out.println(instruction.getInstructionId() + ": " + instruction.toString());
        }
        System.out.println();
        for (BasicBlock children : basicBlock.getChildren()) printBasicBlocks(children);
        for (Function function : basicBlock.getFunctionCalled()) {
            if (function.isVisited()) continue;
            function.setIsVisited();
            printBasicBlocks(function.getFuncBasicBlock());
        }
    }
}
