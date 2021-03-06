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
    VARIABLE_NOT_DECLARED("Variable must be declared"),
    FORMAL_PARAM_DECL_ERROR("Erorr in formalParam declaration"),
    FUNC_PARAM_NOT_DECLARED("Func Parameter not declared"),
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
    THEN_STATEMENT_ERROR("Keyword then missing in if-then-else statement"),
    IF_STATEMENT_ERROR("Keyword if missing in if-then-else statement"),
    FI_STATEMENT_ERROR("Keyword fi missing in if-then-else statement"),
    WHILE_STATEMENT_ERROR("Keyword missing in while statement"),
    DO_EXPECTED("Expected do keyword to be present"),
    OD_EXPECTED("Expected od keyword to be present"),
    RETURN_EXPECTED("Expected return keyword to be present"),
    RELATION_OP_NOT_FOUND("Expected Relation Operation to be present"),
    ARRAY_DIMENSION_MISMATCH("Error in specifying array dimensions"),

    NOT_ABLE_TO_LINK_NEG_INSTR("Not able to link branch instructions"),
    OPCODE_ERROR("Opcode must be with in range"),
    REGISTER_NOT_IN_RANGE("Register not in range"),
    LITERAL_NOT_IN_RANGE("Literal is not in range"),
    ABSOLUTE_NOT_IN_RANGE ("Absolute is not in range"),
    WRONG_FIX_UP_LOCATION("Wrong Fix Up Location");

    private String errorMessage;
    private ErrorMessage(String errorMessage){
        this.errorMessage = errorMessage;
    }
}
