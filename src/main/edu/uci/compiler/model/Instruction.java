package main.edu.uci.compiler.model;

import jdk.nashorn.internal.runtime.regexp.joni.constants.OPCode;

/**
 * Created by srikrishna on 2/1/17.
 */
public class Instruction {

    static int numberOfInstructions;
    Operation operation;
    Result operand1;
    Result operand2;
    Result operand3; // Used for PHI FUNCTIONS
    Result arrayVariable; // Used for Array Variable kill instructions
    private int instructionId;
    private Instruction anchorInstruction;
    private Integer registerNumber;
    private BasicBlock basicBlock;

    public Instruction() {
        this.instructionId = numberOfInstructions;
        anchorInstruction = null;
        operand1 = null;
        operand2 = null;
        operand3 = null;
        arrayVariable = null;
        ++numberOfInstructions;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public Result getOperand1() {
        return operand1;
    }

    public void setOperand1(Result operand1) {
        this.operand1 = operand1;
    }

    public Result getOperand2() {
        return operand2;
    }

    public void setOperand2(Result operand2) {
        this.operand2 = operand2;
    }

    public Result getOperand3() {
        return operand3;
    }

    public void setOperand3(Result operand3) {
        this.operand3 = operand3;
    }

    public void setArrayVariable(Result arrayVariable) {
        this.arrayVariable = arrayVariable;
    }

    public Result getArrayVariable() {
        return this.arrayVariable;
    }

    public int getInstructionId() {
        return instructionId;
    }

    public void setBasicBlock(BasicBlock basicBlock) {
        this.basicBlock = basicBlock;
    }

    public BasicBlock getBasicBlock() {
        return this.basicBlock;
    }

    public void setInstructionId(int instructionId) {
        this.instructionId = instructionId;
    }

    public Instruction getAnchorInstruction() {
        return this.anchorInstruction;
    }

    public void setAnchorInstruction(Instruction anchorInstruction) {
        this.anchorInstruction = anchorInstruction;
    }

    public void setRegisterNumber(Integer registerNumber) {
        this.registerNumber = registerNumber;
    }

    public Integer getRegisterNumber() {
        return this.registerNumber;
    }

    @Override
    public boolean equals(Object object) {
        Instruction instruction = (Instruction) object;
        boolean isSameOperation = this.operation.equals(instruction.getOperation());
        boolean isSameOperand1 = areSameOperands(operand1, instruction.getOperand1());
        boolean isSameOperand2 = areSameOperands(operand2, instruction.getOperand2());
        if (!isSameOperand1 && !isSameOperand2) {
            isSameOperand1 = areSameOperands(operand1, instruction.getOperand2());
            isSameOperand2 = areSameOperands(operand2, instruction.getOperand1());
        }
        boolean isSameOperand3 = areSameOperands(operand3, instruction.getOperand3());
        boolean isSameArrayVariables = areSameOperands(arrayVariable, instruction.getArrayVariable());

        return isSameOperation && isSameOperand1 && isSameOperand2 && isSameOperand3 && isSameArrayVariables;
    }

    private boolean areSameOperands(Result operand, Result targetOperand) {
        if (operand == null && targetOperand == null) return true;
        if (operand == null || targetOperand == null) return false;
        return operand.equals(targetOperand);
    }

    @Override
    public String toString() {
        if (this.operation == null) return null;
        if (isKillInstruction()) return forKill();
        if (this.isLoadStore()) return forLoadStore();
        if (this.isBinaryOperand()) return forTwoOperands();
        if (this.isUnaryOperand()) return forOneOperand();
        if (this.noOperand()) return forNoOperand();
        if (this.operation == Operation.PHI) {
            return this.operation
                    + " " + this.operand1.toString()
                    + " " + this.operand2.toString()
                    + " " + this.operand3.toString();
        }
        return "";
    }

    private boolean isKillInstruction() {
        return this.operation == Operation.KILL;
    }

    private boolean isBinaryOperand() {
        return (this.operation == Operation.ADD
                || this.operation == Operation.SUB
                || this.operation == Operation.MUL
                || this.operation == Operation.DIV
                || this.operation == Operation.MULI
                || this.operation == Operation.ADDA
                || this.operation == Operation.CALL
                || this.operation == Operation.CMP
                || this.operation == Operation.BNE
                || this.operation == Operation.BEQ
                || this.operation == Operation.BGE
                || this.operation == Operation.BGT
                || this.operation == Operation.BLE
                || this.operation == Operation.BLT
                || this.operation == Operation.MOVE);
    }

    private boolean isUnaryOperand() {
        return (this.operation == Operation.BRA
                || this.operation == Operation.RET
                || this.operation == Operation.PARAM
                || this.operation == Operation.WRITE);
    }

    private boolean isLoadStore() {
        return (this.operation == Operation.LOAD
                || this.operation == Operation.STORE);
    }

    private boolean noOperand() {
        return (this.operation == Operation.END
                || this.operation == Operation.WRITENL
                || this.operation == Operation.READ);

    }

    private String forTwoOperands() {
        return this.operation.toString() + " " + this.operand1.toString() + " " + this.operand2.toString();
    }

    private String forOneOperand() {
        return this.operation.toString() + " " + this.operand1.toString();
    }

    private String forNoOperand() {
        return this.operation.toString();
    }

    private String forLoadStore() {
        String instructionString = this.operation.toString() + " " + this.operand1.toString();
        if (operation == Operation.STORE) {
            instructionString += " " + operand2.toString();
        }
        return instructionString + " {" + arrayVariable.getIdentifierName() + "}";
    }

    private String forKill() {
        return this.operation.toString() + " " + this.arrayVariable.toString();
    }


}
