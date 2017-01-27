package main.edu.uci.compiler.parser;

import javafx.util.Pair;
import main.edu.uci.compiler.model.ErrorMessage;
import main.edu.uci.compiler.model.Token;

import static main.edu.uci.compiler.model.Token.*;
import static main.edu.uci.compiler.model.ErrorMessage.*;

import java.io.IOException;

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
                moveToNextToken();
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
//        moveToNextToken();
    }

    public void funcDecl() {
    }

    public void statSequence() {
    }

    public void generateError(ErrorMessage message) {
        System.out.println("Syntax Error occurred - " + message);
    }
}
