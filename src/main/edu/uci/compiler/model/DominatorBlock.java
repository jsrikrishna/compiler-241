package main.edu.uci.compiler.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by srikrishna on 2/22/17.
 */
public class DominatorBlock {
    private Integer id;
    private BasicBlock myBasicBlock;
    private Set<DominatorBlock> children;
    private DominatorBlock parent;

    public DominatorBlock(BasicBlock basicBlock) {
        this.id = basicBlock.getId();
        this.myBasicBlock = basicBlock;
        children = new HashSet<>();
        parent = null;
    }

    public Integer getId() {
        return this.getId();
    }

    public BasicBlock getMyBasicBlock() {
        return myBasicBlock;
    }

    public void setMyBasicBlock(BasicBlock myBasicBlock) {
        this.myBasicBlock = myBasicBlock;
    }

    public Set<DominatorBlock> getChildren() {
        return this.children;
    }

    public void setChildren(Set<DominatorBlock> children) {
        this.children = children;
    }

    public void addChildren(DominatorBlock dominatorBlock) {
        this.children.add(dominatorBlock);
    }

    public DominatorBlock getParent() {
        return this.parent;
    }

    public void setParent(DominatorBlock parent) {
        this.parent = parent;
    }
}
