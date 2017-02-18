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
    private int instructionId;

    public Instruction() {
        this.instructionId = numberOfInstructions;
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

    public int getInstructionId() {
        return instructionId;
    }

    public void setInstructionId(int instructionId) {
        this.instructionId = instructionId;
    }

    @Override
    public String toString() {
        if (this.operation == null) return null;
        if (this.isBinaryOperand()) return forTwoOperands();
        if (this.isUnaryOperand()) return forOneOperand();
        if (this.noOperand()) return forNoOperand();
        if (this.operation == Operation.MOVE
                || this.operation == Operation.STORE)
            return this.operation + " " + this.operand1.toString() + " " + this.operand2.toString();
        if(this.operation == Operation.PHI){
            return this.operation + " " + this.operand1.toString() + " "+ this.operand2.toString() + " " + this.operand3.toString();
        }
        return "";
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
                || this.operation == Operation.BLT);
    }

    private boolean isUnaryOperand() {
        return (this.operation == Operation.BRA
                || this.operation == Operation.RET
                || this.operation == Operation.PARAM
                || this.operation == Operation.WRITE);
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


}
