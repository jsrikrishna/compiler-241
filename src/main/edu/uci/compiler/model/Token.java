package main.edu.uci.compiler.model;

/**
 * Created by srikrishna on 1/18/17.
 */
public enum Token {
    /*
    Error
     */
    ERROR(0),

    /*
    Relational Operators
     */
    TIMES(1), DIV(2), PLUS(11), MINUS(12),
    EQL(20), NEQ(21),
    GTR(25), LSS(22), GEQ(23), LEQ(24),


    /*
    Parenthesis, Brackets, SEMICOLON, PERIOD
     */
    OPENPAREN(40), CLOSEPAREN(41),
    OPENBRACKET(42), CLOSEBRACKET(43),
    SEMICOLON(44), PERIOD(45), COMMA(46),

    /*
    Assignment, funcCall, ifStatement, whileStatement, returnStatement
     */
    LET(100), CALL(101),
    IF(102), THEN(103), ELSE(104), FI(105),
    WHILE(104), DO(107), OD(108),
    RETURN(109),

    /*
    Declaration
     */
    VAR(110), ARRAY(111), FUNCTION(112), PROCEDURE(113),
    NUMBER(60), IDEN(61),

    /*
    Main
     */
    MAIN(200), EOF(255), INIT(256);


    private int id;
    private Token(int id){
        this.id = id;
    }
}
