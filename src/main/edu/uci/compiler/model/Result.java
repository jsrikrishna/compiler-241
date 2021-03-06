package main.edu.uci.compiler.model;

/**
 * Created by srikrishna on 2/1/17.
 */
public class Result {
    public enum KIND {
        CONSTANT, VARIABLE, REGISTER, CONDITION, INSTRUCTION, BRANCH_INSTRUCTION, FIX_UP,
        ARRAY_VARIABLE, BASE_ADDRESS, FRAME_POINTER,
        FUNCTION, PARAMETER_COUNT, PARAMETER,
        INIT;
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
    private Integer registerNumber;

    public Result() {
        kind = KIND.INIT;
        value = address = regNo = fixUpInstructionId = -1;
        registerNumber = -1;
        instructionId = basicBlockId = ssaVersion = funcBasicBlockId = parameterCount = -1;
        condition = Token.INIT;
        identifierName = "";
    }


    public Integer getInstructionId() {
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

    public int getBasicBlockId() {
        return this.basicBlockId;
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
        return this.identifierName;
    }

    public void setIdentifierName(String identifierName) {
        this.identifierName = identifierName;
    }

    public void setBasicBlockId(Integer basicBlockId) {
        this.basicBlockId = basicBlockId;
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

    public void setRegisterNumber(Integer color) {
        this.registerNumber = color;
    }

    public Integer getRegisterNumber() {
        return this.registerNumber;
    }


    @Override
    public String toString() {

        if (kind == KIND.VARIABLE) return this.identifierName + "_" + ssaVersion;
        if (kind == KIND.CONSTANT) return Integer.toString(this.value);
        if (kind == KIND.CONDITION) return this.condition.toString();
        if (kind == KIND.INSTRUCTION) return "(" + this.instructionId + ")";
        if (kind == KIND.BRANCH_INSTRUCTION) return "[" + basicBlockId + "]";
        if (kind == KIND.FIX_UP) return "fix_up instruction_id (" + this.fixUpInstructionId + ")";
        if (kind == KIND.ARRAY_VARIABLE) return this.identifierName;
        if (kind == KIND.BASE_ADDRESS) return this.identifierName + "_baseAddress";
        if (kind == KIND.FRAME_POINTER) return "FRAME_POINTER";
        if (kind == KIND.FUNCTION) return "[" + this.funcBasicBlockId + "]";
        if (kind == KIND.PARAMETER_COUNT) return "parameter_count " + this.parameterCount;
        if (kind == KIND.PARAMETER) return "Parameter " + this.getIdentifierName();
        if (kind == KIND.REGISTER) {
            if (this.getRegisterNumber() > 8) {
                return "SR" + registerNumber;
            }
            return "R" + registerNumber;
        }
        return super.toString();
    }

    @Override
    public int hashCode() {
        return 31 * (this.value
                + this.address
                + this.ssaVersion
                + this.regNo
                + this.fixUpInstructionId
                + this.instructionId
                + this.basicBlockId
                + this.funcBasicBlockId
                + this.parameterCount
                + this.registerNumber)
                + identifierName.length();
    }

    @Override
    public boolean equals(Object obj) {
        Result result = (Result) obj;
        return this.kind == result.getKind()
                & this.value == result.getValue()
                & this.address == result.getAddress()
                & this.ssaVersion.equals(result.getSsaVersion())
                & this.regNo == result.getRegNo()
                & this.fixUpInstructionId == result.getFixUpInstructionId()
                & this.instructionId == result.getInstructionId()
                & this.basicBlockId == result.getBasicBlockId()
                & this.funcBasicBlockId.equals(result.getFuncBasicBlockId())
                & this.parameterCount.equals(result.getParameterCount())
                & this.identifierName.equals(result.getIdentifierName())
                & this.condition == result.getCondition()
                & this.registerNumber.equals(result.getRegisterNumber());
    }
}
