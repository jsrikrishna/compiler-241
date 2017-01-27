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
    SEMICOLON_NOT_FOUND("Semicolon(;) not found"),
    NUMBER_EXPECTED("Number expected in type declaration");

    private String errorMessage;
    private ErrorMessage(String errorMessage){
        this.errorMessage = errorMessage;
    }
}
