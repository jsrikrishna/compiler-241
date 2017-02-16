package main.edu.uci.compiler.model;

/**
 * Created by srikrishna on 2/1/17.
 */
public class Result {
    public enum KIND {
        CONSTANT, VARIABLE, REGISTER, CONDITION, INSTRUCTION, BRANCH_INSTRUCTION, FIX_UP,
        ARRAY_VARIABLE, BASE_ADDRESS, FRAME_POINTER,
        FUNCTION, PARAMETER_COUNT;
    }

    private KIND kind;
    private int value; // If it is constant
    private int address; // address, if it is a variable
    private int regNo; // register Number, if it is a register or a condition
    private int fixUpInstructionId; // if it is a condition, it will be jump instruction id // TODO: Take for while
    private Token condition;  // if it is a condition, it contains relational operators like ==, !=, <, <=, >, >=
    private String identifierName; // used for calculating the array base address //TODO: what
    private Integer instructionId; // if it is a instruction,to hold the result of instruction like MUL, ADD
    private Integer basicBlockId; // for branch instructions, BRA
    private Integer ssaVersion; // Tracker Version for a variable
    private Integer funcBasicBlockId;
    private Integer parameterCount;


    public int getInstructionId() {
        return instructionId;
    }

    public void setInstructionId(int instructionId) {
        this.instructionId = instructionId;
    }

    public KIND getKind() {
        return kind;
    }

    public void setKind(KIND kind) {
        this.kind = kind;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public int getRegNo() {
        return regNo;
    }

    public void setRegNo(int regNo) {
        this.regNo = regNo;
    }

    public int getFixUpInstructionId() {
        return fixUpInstructionId;
    }

    public void setFixUpInstructionId(int fixUpInstructionId) {
        this.fixUpInstructionId = fixUpInstructionId;
    }

    public Token getCondition() {
        return this.condition;
    }

    public void setCondition(Token condition) {
        this.condition = condition;
    }

    public String getIdentifierName() {
        return identifierName;
    }

    public void setIdentifierName(String identifierName) {
        this.identifierName = identifierName;
    }

    public void setBasicBlockId(Integer basicBlockId) {
        this.basicBlockId = basicBlockId;
    }

    public Integer getBasicBlockId() {
        return this.basicBlockId;
    }

    public void setSsaVersion(Integer ssaVersion) {
        this.ssaVersion = ssaVersion;
    }

    public Integer getSsaVersion() {
        return this.ssaVersion;
    }

    public void setFuncBasicBlockId(Integer funcBasicBlockId) {
        this.funcBasicBlockId = funcBasicBlockId;
    }

    public Integer getFuncBasicBlockId() {
        return this.funcBasicBlockId;
    }

    public void setParameterCount(Integer parameterCount) {
        this.parameterCount = parameterCount;
    }

    public Integer getParameterCount() {
        return this.parameterCount;
    }


    @Override
    public String toString() {

        if (kind == KIND.VARIABLE) return identifierName + "_" + ssaVersion;
        if (kind == KIND.CONSTANT) return Integer.toString(value);
        if (kind == KIND.CONDITION) return condition.toString();
        if (kind == KIND.INSTRUCTION) return "(" + instructionId + ")";
        if (kind == KIND.BRANCH_INSTRUCTION) return "[" + basicBlockId + "]";
        if (kind == KIND.FIX_UP) return "fix_up instruction_id (" + fixUpInstructionId + ")";
        if (kind == KIND.ARRAY_VARIABLE) return identifierName;
        if (kind == KIND.BASE_ADDRESS) return identifierName + "_baseAddress";
        if (kind == KIND.FRAME_POINTER) return "FRAME_POINTER";
        if (kind == KIND.FUNCTION) return "[" + funcBasicBlockId + "]";
        if (kind == KIND.PARAMETER_COUNT) return "parameter_count " + parameterCount;
        return super.toString();
    }
}
