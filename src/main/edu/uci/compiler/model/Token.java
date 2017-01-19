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
    GTR(25, ">"), LSS(22, "<"), GEQ(23, ">="), LEQ(24, "<="),


    /*
    Parenthesis, Brackets, SEMICOLON, PERIOD
     */
    OPENPAREN(40, "("), CLOSEPAREN(41, ")"),
    OPENBRACKET(42, "["), CLOSEBRACKET(43, "]"),
    BEGIN(44, "{"), END(45, "}"),
    SEMICOLON(46, ";"), PERIOD(47, "."), COMMA(48, ","),

    /*
    KeyWords -
    Assignment, funcCall, ifStatement, whileStatement, returnStatement
     */
    LET(100, "let"), CALL(101, "call"),
    IF(102, "if"), THEN(103, "then"), ELSE(104, "else"), FI(105, "fi"),
    WHILE(104, "while"), DO(107, "do"), OD(108, "od"),
    RETURN(109, "return"),

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
