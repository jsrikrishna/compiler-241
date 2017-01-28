package main.edu.uci.compiler.model;

/**
 * Created by srikrishna on 1/27/17.
 */
public enum ErrorMessage {
    MAIN_NOT_FOUND("Expected main to be present"),
    BEGIN_NOT_FOUND("No Begin block found, expected {"),
    END_NOT_FOUND("No End symbol is found, expected }"),
    PERIOD_NOT_FOUND("Period(.) not found, program must be ended with a period"),
    OPEN_BRACKET_NOT_FOUND("Expected [ to be present"),
    TYPE_DECL_ERROR("Error in type declaration"),
    VARIABLE_DECL_ERROR("Error in Variable declaration"),
    FORMAL_PARAM_DECL_ERROR("Erorr in formalParam declaration"),
    FUNC_BODY_ERROR("Error in defining function body"),
    SEMICOLON_NOT_FOUND("Semicolon(;) not found"),
    FUNCTION_PROCEDURE_NOT_FOUND("function or procedure expected"),
    IDENTIFIER_NOT_FOUND("Expected Identifier to be present"),
    NUMBER_EXPECTED("Number expected in type declaration"),
    KEYWORD_EXPECTED("Keyword exepcted"),
    ASSIGNMENT_ERROR("Assignment error"),
    DESIGNATOR_ERROR("Expected identifier in designator declaration"),
    CLOSE_BRACKET_NOT_FOUND("Expected ] to be present"),
    CLOSE_PAREN_NOT_FOUND("Expected ) to be present"),
    FACTOR_ERROR("Factor error"),
    BECOMES_NOT_FOUND("Expected <- to be present"),
    CALL_NOT_FOUND("Call not found in function call statment"),
    IF_STATEMENT_ERROR("Keyword missing in if-then-else statement"),
    WHILE_STATEMENT_ERROR("Keyword missing in while statement"),
    DO_EXPECTED("Expected do keyword to be present"),
    OD_EXPECTED("Expected od keyword to be present"),
    RETURN_EXPECTED("Expected return keyword to be present"),
    RELATION_OP_NOT_FOUND("Expected Relation Operation to be present");

    private String errorMessage;
    private ErrorMessage(String errorMessage){
        this.errorMessage = errorMessage;
    }
}
