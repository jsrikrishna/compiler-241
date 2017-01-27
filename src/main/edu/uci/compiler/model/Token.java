package main.edu.uci.compiler.model;

/**
 * Created by srikrishna on 1/18/17.
 */
public enum Token {
    /*
    Error
     */
    ERROR(0, "error"),

    /*
    Relational Operators
     */
    TIMES(1, "*"), DIV(2, "/"), PLUS(11, "+"), MINUS(12, "-"),
    EQL(20, "=="), NEQ(21, "!="),
    GTR(25, ">"), LSS(22, "<"),
    GEQ(23, ">="), LEQ(24, "<="),


    /*
    Parenthesis, Brackets, SEMICOLON, PERIOD
     */
    OPENPAREN(50, "("), CLOSEPAREN(35, ")"),
    OPENBRACKET(32, "["), CLOSEBRACKET(34, "]"),
    BEGIN(150, "{"), END(80, "}"),
    SEMICOLON(70, ";"), PERIOD(30, "."), COMMA(31, ","),

    /*
    KeyWords -
    Assignment, funcCall, ifStatement, whileStatement, returnStatement
     */
    LET(100, "let"), CALL(101, "call"),
    IF(102, "if"), THEN(41, "then"), ELSE(90, "else"), FI(82, "fi"),
    WHILE(103, "while"), DO(42, "do"), OD(81, "od"),
    RETURN(104, "return"),

    /*
    KeyWords -
    Declaration
     */
    VAR(110, "var"), ARRAY(111, "array"), FUNCTION(112, "function"), PROCEDURE(113, "procedure"),
    NUMBER(60, "number"), IDEN(61, "identifier"), BECOMES(40, "<-"),

    /*
    KeyWords -
    Main
     */
    MAIN(200, "main"), EOF(255, "eof"), INIT(256, "init");


    private int id;
    private String representation;
    private Token(int id, String representation){
        this.id = id;
        this.representation = representation;
    }
}
