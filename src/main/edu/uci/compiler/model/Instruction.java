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
    int instructionId;

    public Instruction(){
        this.instructionId = numberOfInstructions;
        ++numberOfInstructions;
    }

    public Operation getOperation(){
        return operation;
    }
    public void setOperation(Operation operation){
        this.operation = operation;
    }
    public Result getOperand1(){
        return operand1;
    }
    public void setOperand1(Result operand1){
        this.operand1 = operand1;
    }
    public Result getOperand2(){
        return operand2;
    }
    public void setOperand2(Result operand2){
        this.operand2 = operand2;
    }
    public int getInstructionId(){
        return instructionId;
    }
    public void setInstructionId(int instructionId){
        this.instructionId = instructionId;
    }



}
