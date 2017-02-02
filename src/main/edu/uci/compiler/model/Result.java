package main.edu.uci.compiler.model;

/**
 * Created by srikrishna on 2/1/17.
 */
public class Result {
    public enum KIND {
        CONSTANT, VARIABLE, REGISTER, CONDITION, INSTRUCTION;
    }

    private KIND kind;
    int value; // If it is constant
    int address; // address, if it is a variable
    int regNo; // register Number, if it is a register or a condition
    int fixUpLocation; // if it is a condition, it will be jump instruction id
    Token condition;  // if it is a condition
    String identifierName;
    int instructionId;

    public int getInstruction(){
        return instructionId;
    }
    public void setInstructionId(int instructionId){
        this.instructionId = instructionId;
    }
    public KIND getKind(){
        return kind;
    }
    public void setKind(KIND kind){
        this.kind = kind;
    }
    public int getValue(){
        return value;
    }
    public void setValue(int value){
        this.value = value;
    }
    public int getAddress(){
        return address;
    }
    public void setAddress(int address){
        this.address = address;
    }
    public int getRegNo(){
        return regNo;
    }
    public void setRegNo(int regNo){
        this.regNo = regNo;
    }
    public int getFixUpLocation(){
        return fixUpLocation;
    }
    public void setFixUpLocation(int fixUpLocation){
        this.fixUpLocation = fixUpLocation;
    }
    public Token getCondition(){
        return this.condition;
    }
    public void setCondition(Token condition){
        this.condition = condition;
    }
    public String getIdentifierName(){
        return identifierName;
    }
    public void setIdentifierName(String identifierName){
        this.identifierName = identifierName;
    }


}
