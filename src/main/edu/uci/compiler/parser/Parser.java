package main.edu.uci.compiler.parser;

import main.edu.uci.compiler.model.ErrorMessage;
import main.edu.uci.compiler.model.Token;

import static main.edu.uci.compiler.model.Token.*;
import static main.edu.uci.compiler.model.ErrorMessage.*;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by srikrishna on 1/27/17.
 */
public class Parser {
    private Scanner scanner;
    private Token currentToken;

    public Parser(String fileName) throws IOException {
        scanner = new Scanner(fileName);
        currentToken = scanner.getToken();
    }

    private void moveToNextToken() throws IOException {
        currentToken = scanner.getToken();
    }

    public void computation() throws IOException {
        if (currentToken == MAIN) {
            moveToNextToken();
            while (currentToken == VAR || currentToken == ARRAY) {
                //TODO: Deal with array's later
                varDecl();
            }
            while (currentToken == FUNCTION || currentToken == PROCEDURE) {
                moveToNextToken();
                funcDecl();
            }
            if (currentToken == BEGIN) {
                moveToNextToken();
                statSequence();
                if (currentToken == END) {
                    moveToNextToken();
                    if (currentToken == PERIOD) {
                        moveToNextToken();
                    } else generateError(PERIOD_NOT_FOUND);
                } else generateError(END_NOT_FOUND);
            } else generateError(BEGIN_NOT_FOUND);
        } else generateError(MAIN_NOT_FOUND);
    }


    public void varDecl() throws IOException {
        ArrayList<Integer> arrayDimensions = typeDecl();
        if (currentToken == IDENTIFIER) {
            moveToNextToken();
            //TODO: need to store the variable, it could be an array variable or normal variable
            while (currentToken == COMMA) {
                moveToNextToken();
                if(currentToken == IDENTIFIER){
                    moveToNextToken();
                    //TODO: need to store the variable, it could be an array variable or normal variable
                } else {
                    generateError(VARIABLE_DECL_ERROR);
                }
            }
            if(currentToken == SEMICOLON){
                moveToNextToken();
                // done with variable declaration
            } else generateError(SEMICOLON_NOT_FOUND);
        } else generateError(VARIABLE_DECL_ERROR);


    }

    public ArrayList<Integer> typeDecl() throws IOException {
        ArrayList<Integer> arrayDimensions = null;
        if (currentToken == VAR) {
            moveToNextToken();
        } else if (currentToken == ARRAY) {
            moveToNextToken();
            arrayDimensions = new ArrayList<Integer>();
            if (currentToken == OPENBRACKET) {
                moveToNextToken();
                arrayDimensions.add(number());
                if (currentToken == CLOSEBRACKET) {
                    moveToNextToken();
                    while (currentToken == OPENBRACKET) {
                        moveToNextToken();
                        arrayDimensions.add(number());
                        if (currentToken == CLOSEBRACKET) moveToNextToken();
                        else generateError(TYPE_DECL_ERROR);
                    }
                } else generateError(TYPE_DECL_ERROR);
            } else generateError(OPEN_BRACKET_NOT_FOUND);
        } else generateError(TYPE_DECL_ERROR);
        return arrayDimensions;
    }

    public int number() throws IOException {
        if (currentToken == NUMBER) {
            moveToNextToken();
            return scanner.getCurrentNumber();
        } else generateError(NUMBER_EXPECTED);
        //TODO: // Code never reach here though if we exit in generateError(), need to decide what to do
        return -1;
    }

    public void funcDecl() {
    }

    public void statSequence() {
    }

    public void generateError(ErrorMessage message) {
        System.out.println("Syntax Error occurred - " + message);
    }
}
