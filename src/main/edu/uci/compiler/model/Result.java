package main.edu.uci.compiler.model;

/**
 * Created by srikrishna on 2/1/17.
 */
public class Result {
    public enum KIND {
        CONSTANT, VARIABLE, REGISTER, CONDITION, INSTRUCTION, BRANCH_INSTRUCTION, FIX_UP;
    }

    private KIND kind;
    private int value; // If it is constant
    private int address; // address, if it is a variable
    private int regNo; // register Number, if it is a register or a condition
    private int fixUpInstructionId; // if it is a condition, it will be jump instruction id
    private Token condition;  // if it is a condition, it contains relational operators like ==, !=, <, <=, >, >=
    private String identifierName;
    private Integer instructionId;
    private Integer basicBlockId; // for branch instructions, BRA



    public int getInstructionId(){
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
    public int getFixUpInstructionId(){
        return fixUpInstructionId;
    }
    public void setFixUpInstructionId(int fixUpInstructionId){
        this.fixUpInstructionId = fixUpInstructionId;
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
    public void setBasicBlockId(Integer basicBlockId){
        this.basicBlockId = basicBlockId;
    }
    public Integer getBasicBlockId(){
        return this.basicBlockId;
    }


}
