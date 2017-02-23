package main.edu.uci.compiler.model;

import java.util.Set;

/**
 * Created by srikrishna on 2/22/17.
 */
public class DominatorNode {
    private BasicBlock myBasicBlock;
    private Set<BasicBlock> children;
    private Set<BasicBlock> parent;

    public BasicBlock myBasicBlock(){
        return myBasicBlock;
    }
    public void setMyBasicBlock(BasicBlock myBasicBlock){
        this.myBasicBlock = myBasicBlock;
    }
    public Set<BasicBlock> getChildren(){
        return this.children;
    }
    public void setChildren(Set<BasicBlock> children){
        this.children = children;
    }
    public Set<BasicBlock> getParent(){
        return this.parent;
    }
    public void setParent(Set<BasicBlock> parent){
        this.parent = parent;
    }
}
