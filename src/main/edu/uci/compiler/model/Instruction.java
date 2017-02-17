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

    public int getInstructionId() {
        return instructionId;
    }

    public void setInstructionId(int instructionId) {
        this.instructionId = instructionId;
    }

    @Override
    public String toString() {
//        System.out.println("coming here " + this.operation);
        if (this.operation == null) {
//            System.out.println("Operation is null");
            return null;
        }
        ;
        if (this.isBinaryOperand()) return forTwoOperands();
        if (this.isUnaryOperand()) return forOneOperand();
        if (this.noOperand()) return forNoOperand();
        if (this.operation == Operation.MOVE || this.operation == Operation.STORE)
            return this.operation + " " + this.operand2.toString() + " " + this.operand1.toString();
//        System.out.println("Instruction " + this.operation);
        return "";
    }

    private boolean isBinaryOperand() {
//        System.out.println("coming into Binary");
        if (this.operation == Operation.ADD
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
                || this.operation == Operation.BLT) {
//            System.out.println("returning true for binary");
            return true;
        }

        return false;
    }

    private boolean isUnaryOperand() {
        if (this.operation == Operation.BRA
                || this.operation == Operation.RET
                || this.operation == Operation.PARAM
                || this.operation == Operation.WRITE) return true;
        return false;
    }

    private boolean noOperand() {
        if (this.operation == Operation.END || this.operation == Operation.WRITENL || this.operation == Operation.READ)
            return true;
        return false;
    }

    private String forTwoOperands() {
        String res = this.operation.toString() + " " + this.operand1.toString() + " " + this.operand2.toString();
//        System.out.println("two operand result is " + res);
        return res;
    }

    private String forOneOperand() {
        return this.operation.toString() + " " + this.operand1.toString();
    }

    private String forNoOperand() {
        return this.operation.toString();
    }


}
