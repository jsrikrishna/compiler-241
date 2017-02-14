package main.edu.uci.compiler.model;

/**
 * Created by srikrishna on 2/1/17.
 */
public enum Operation {
    NEG("neg"),
    ADD("add"),
    SUB("sub"),
    MUL("mul"),
    MULI("muli"),
    DIV("div"),
    CMP("cmp"),
    ADDA("adda"),
    LOAD("load"),
    STORE("store"),
    MOVE("move"),
    PHI("phi"),
    END("end"),
    BRA("bra"),
    BNE("bne"),
    BEQ("beq"),
    BLE("ble"),
    BLT("blt"),
    BGE("bge"),
    BGT("bgt"),
    READ("read"),
    WRITE("write"),
    WRITENL("writeNL"),

    RET("ret"),

    // Function Calls
    PARAM("param"),
    CALL("call");


    private String operationName;
    public String getOperationName(){
        return this.operationName;
    }
    private Operation(String operationName){
        this.operationName = operationName;
    }

}
