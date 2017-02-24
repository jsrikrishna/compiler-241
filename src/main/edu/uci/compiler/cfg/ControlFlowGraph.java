package main.edu.uci.compiler.cfg;import main.edu.uci.compiler.model.BasicBlock;import main.edu.uci.compiler.model.Function;import main.edu.uci.compiler.model.Instruction;import java.io.*;import java.util.*;/** * Created by srikrishna on 2/2/17. */public class ControlFlowGraph {    BasicBlock startBasicBlock;    public void setStartBasicBlock(BasicBlock basicBlock) {        this.startBasicBlock = basicBlock;    }    public BasicBlock getBasicBlock() {        return startBasicBlock;    }    private void printBasicBlocks(BasicBlock basicBlock, List<String> digraph) {        if (basicBlock == null) return;        if (basicBlock.isVisited()) return;        basicBlock.setIsVisited();        digraph.add("BasicBlock" + basicBlock.getId()                + "[shape=\"box\", label=\"BasicBlock" + basicBlock.getId()                + "\n\n Type is " + basicBlock.getType() + "\n");        System.out.println("Basic Block " + basicBlock.getId() + " " + basicBlock.getType());        if (basicBlock.getInstructions().size() == 0) {            digraph.add("No Instructions in this basic block");        }        for (Instruction instruction : basicBlock.getInstructions()) {            System.out.println(instruction.getInstructionId() + ": " + instruction.toString());            digraph.add(instruction.getInstructionId() + ": " + instruction.toString());        }        System.out.println();        digraph.add("\"]");        for (BasicBlock children : basicBlock.getChildren()) printBasicBlocks(children, digraph);        for (Function function : basicBlock.getFunctionCalled()) {            if (function.isVisited()) continue;            function.setIsVisited();            printBasicBlocks(function.getFuncBasicBlock(), digraph);        }    }    private void printBasicBlockTree(LinkedList<BasicBlock> allBasicBlocks, List<String> digraph) {        for (BasicBlock b : allBasicBlocks) {            for (BasicBlock children : b.getChildren()) {                digraph.add("BasicBlock" + b.getId() + " -> BasicBlock" + children.getId());            }        }    }    private void generateCFG(BasicBlock startBasicBlock,                             LinkedList<BasicBlock> allBasicBlocks,                             List<String> digraph) {        digraph.add("digraph{");        printBasicBlockTree(allBasicBlocks, digraph);        printBasicBlocks(startBasicBlock, digraph);        digraph.add("}");    }    public void writeToCFGFile(String fileName,                               boolean isCP,                               BasicBlock startBasicBlock,                               LinkedList<BasicBlock> allBasicBlocks) {        List<String> digraph = new ArrayList<>();        generateCFG(startBasicBlock, allBasicBlocks, digraph);        Writer writer = null;        try {            String newFileName, cfgFileName;            if (isCP) {                System.out.println("Generating CFG with Copy Propagation");                newFileName =fileName.substring(0, fileName.length() - 4) + "CFG_CP.dot";                cfgFileName =fileName.substring(0, fileName.length() - 4) + "CFG_CP.png";            } else {                System.out.println("Generating CFG without Copy Propagation");                newFileName =fileName.substring(0, fileName.length() - 4) + "CFG_NO_CP.dot";                cfgFileName =fileName.substring(0, fileName.length() - 4) + "CFG_NO_CP.png";            }            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newFileName), "utf-8"));            for (String str : digraph) {                writer.write(str + "\n");            }            Runtime.getRuntime().exec("dot -Tpng " + newFileName + " -o " + cfgFileName);            System.out.println("Generated CFG at " + cfgFileName);        } catch (Exception ex) {            System.err.print("Error occured while writing CFG Data to file");        } finally {            try {                writer.close();            } catch (Exception ex) {                System.err.println("Error while closing writer and exiting");            }        }    }}