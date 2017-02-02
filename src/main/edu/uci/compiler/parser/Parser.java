package main.edu.uci.compiler.parser;

import main.edu.uci.compiler.model.ErrorMessage;
import main.edu.uci.compiler.model.Result;
import main.edu.uci.compiler.model.Token;

import static main.edu.uci.compiler.model.Token.*;
import static main.edu.uci.compiler.model.ErrorMessage.*;
import static main.edu.uci.compiler.model.Result.KIND.*;

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
//            System.out.println("Token is " + currentToken);
            while (currentToken == VAR || currentToken == ARRAY) {
                //TODO: Deal with array's later
                // Need not move to next token, it is handled by varDecl
                varDecl();
            }
            while (currentToken == FUNCTION || currentToken == PROCEDURE) {
                // Need not move to next token, it is handled by funcDecl
                funcDecl();
            }
            if (currentToken == BEGIN) {
                moveToNextToken();
//                System.out.println("Back to computation - in BEGIN Block " + currentToken);
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
//        System.out.println("Current in varDecl is " + currentToken);
        if (currentToken == IDENTIFIER) {
//            System.out.println("current identifier is " + scanner.getCurrentIdentifier());
            moveToNextToken();
//            System.out.println("Current in varDecl is " + currentToken);
            //TODO: need to store the variable, it could be an array variable or normal variable
            while (currentToken == COMMA) {
                moveToNextToken();
//                System.out.println("Current in varDecl while is " + currentToken);
                if (currentToken == IDENTIFIER) {
                    moveToNextToken();
//                    System.out.println("current identifier is " + scanner.getCurrentIdentifier());
                    //TODO: need to store the variable, it could be an array variable or normal variable
                } else {
                    generateError(VARIABLE_DECL_ERROR);
                }
            }
            if (currentToken == SEMICOLON) {
                moveToNextToken();
//                System.out.println("SEMICOLON block " + currentToken);
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
//            System.out.println("typeDecl current token is " + currentToken);
            arrayDimensions = new ArrayList<Integer>();
            if(currentToken == OPENBRACKET){
                while (currentToken == OPENBRACKET){
//                    System.out.println("open bracket " + currentToken);
                    moveToNextToken();
                    arrayDimensions.add(number());
                    if (currentToken == CLOSEBRACKET) {
//                        System.out.println("close bracket " + currentToken);
                        moveToNextToken();
                    } else generateError(TYPE_DECL_ERROR);
                }
            } else generateError(OPEN_BRACKET_NOT_FOUND);
        } else generateError(TYPE_DECL_ERROR);
//        System.out.println("coming here and returning array dimensions and next token is " + currentToken);
        return arrayDimensions;
    }

    public int number() throws IOException {
        if (currentToken == NUMBER) {
            moveToNextToken();
//            System.out.println("current number is " + scanner.getCurrentNumber());
            return scanner.getCurrentNumber();
        } else generateError(NUMBER_EXPECTED);
        //TODO: Code never reach here though if we exit in generateError(), need to decide what to do
        return -1;
    }

    public void funcDecl() throws IOException {
        if (currentToken == FUNCTION || currentToken == PROCEDURE) {
            moveToNextToken();
//            System.out.println("funcDecl - " + currentToken);
            if (currentToken == IDENTIFIER) {
                moveToNextToken();
                formalParam(); // formalParam, handles of moving to next token
                if (currentToken == SEMICOLON) {
                    moveToNextToken();
//                    System.out.println("came to semicolon " + currentToken);
                    funcBody();
                    if (currentToken == SEMICOLON) moveToNextToken();
                    else generateError(SEMICOLON_NOT_FOUND);
                } else generateError(SEMICOLON_NOT_FOUND);
            } else generateError(IDENTIFIER_NOT_FOUND);
        } else generateError(FUNCTION_PROCEDURE_NOT_FOUND);
//        System.out.println("end - funcDecl - " + currentToken);
    }

    public void formalParam() throws IOException {
        if (currentToken == OPENPAREN) {
            moveToNextToken();
            if (currentToken == IDENTIFIER) {
                //TODO: need to store the variable at common place
                moveToNextToken();
                while (currentToken == COMMA) {
                    moveToNextToken();
                    if (currentToken == IDENTIFIER) {
                        //TODO: need to store the variable at common place
                        moveToNextToken();
                    } else generateError(FORMAL_PARAM_DECL_ERROR);
                }
            }
            if (currentToken == CLOSEPAREN) {
                moveToNextToken();
            } else generateError(FORMAL_PARAM_DECL_ERROR);
        }
//        System.out.println("end - formalParam - " + currentToken);
    }

    public void funcBody() throws IOException {
        while (currentToken == VAR || currentToken == ARRAY) varDecl();
        if (currentToken == BEGIN) {
            moveToNextToken();
//            System.out.println("start - funcBody - " + currentToken);
            statSequence();
            if (currentToken == END) {
                moveToNextToken();
            } else generateError(END_NOT_FOUND);
        } else generateError(BEGIN_NOT_FOUND);
    }

    public void statSequence() throws IOException {
//        System.out.println("statSequence 1" + currentToken);
        statement();
//        System.out.println("statSequence 2" + currentToken);
        while (currentToken == SEMICOLON) {
            moveToNextToken();
            statement();
        }

    }

    public void statement() throws IOException {
        if (currentToken == LET) {
            assignment();
        } else if (currentToken == CALL) {
            funcCall();
        } else if (currentToken == IF) {
            ifStatement();
        } else if (currentToken == WHILE) {
            whileStatement();
        } else if (currentToken == RETURN) {
            returnStatement();
        } else {
            generateError(KEYWORD_EXPECTED);
        }

    }

    public void assignment() throws IOException {
//        System.out.println("came to assignment with token " + currentToken);
        Result lhs = null, rhs = null;
        if (currentToken == LET) {
            moveToNextToken();
//            System.out.println("came to assignment, next token is " + currentToken);
            lhs = designator();
            if (currentToken == BECOMES) {
                moveToNextToken();
//                System.out.println("came into become block of assignment, token now is " + currentToken);
                rhs = expression();

            } else {
                generateError(BECOMES_NOT_FOUND);
            }


        } else {
            /*
            TODO: this code may be never be reached, as we already checked for let in statement, need to identify a
            pattern to handle these kind of duplicate code
             */
            generateError(ASSIGNMENT_ERROR);
        }
    }

    public Result designator() throws IOException {
        Result res = null;
        if (currentToken == IDENTIFIER) {

            res = new Result();
            res.setKind(VARIABLE);
            res.setIdentifierName(scanner.getCurrentIdentifier());
//            System.out.println("in Designator, current token is " + currentToken + " with name is " + scanner.getCurrentIdentifier());
            moveToNextToken();
//            System.out.println("in Designator, " + currentToken);
            while (currentToken == OPENBRACKET)
            {
//                System.out.println("came here, with token " + currentToken);
                //TODO: Need to deal with arrays
                moveToNextToken();
                expression();
                if (currentToken == CLOSEBRACKET) {
                    moveToNextToken();
                } else {
//                    System.out.println("why i am coming here");
                    //TODO: it could be CLOSE_BRACKET_NOT_FOUND, need to design error messages
                    generateError(DESIGNATOR_ERROR);
                }
            }
        } else {
            generateError(DESIGNATOR_ERROR);
        }
//        System.out.println("current token is " + currentToken + " came here");
        return res;
    }

    public Result expression() throws IOException {
//        System.out.println("expression - " + currentToken);
        Result res = term();
        while (currentToken == PLUS || currentToken == MINUS) {
            moveToNextToken();
            term();
        }
        return res;
    }

    public Result term() throws IOException {
//        System.out.println("term - " + currentToken);
        Result lhs = factor();
        while (currentToken == TIMES || currentToken == DIV) {
            moveToNextToken();
            factor();
        }
        return lhs;
    }

    public Result factor() throws IOException {
        Result result = null;
//        System.out.println("factor - current token is " + currentToken);
        if (currentToken == IDENTIFIER) {
            //TODO: Need to deal with identifiers
            result = designator();
        } else if (currentToken == NUMBER) {
            //TODO: Need to deal with number
            result = new Result();
            result.setKind(CONSTANT);
            result.setValue(number());
        } else if (currentToken == CALL) {
            //TODO: Need to deal with function calls
            funcCall();
        } else if (currentToken == OPENPAREN) {
            moveToNextToken();
            expression();
            if (currentToken == CLOSEPAREN) {
                moveToNextToken();
            } else {
                //TODO: Can be CLOSE_PAREN_NOT_FOUND, again needs to design the error message
                //TODO: Done i guess, but still keeping to clarify
                generateError(CLOSE_PAREN_NOT_FOUND);
            }
        } else generateError(FACTOR_ERROR);
//        System.out.println("going out of number now with token as " +currentToken);
        return result;
    }

    public void funcCall() throws IOException {
        if (currentToken == CALL) {
            moveToNextToken();
            if (currentToken == IDENTIFIER) {
                moveToNextToken();
                //TODO: Handle identifier here, figure out how to do it
                if (currentToken == OPENPAREN) {
                    moveToNextToken();
//                    System.out.println("funcCall - openparen - " + currentToken);
                    // Go from expression -> term -> factor -> designator -> identifier
                    if(currentToken != CLOSEPAREN){
                        expression();
                        while (currentToken == COMMA) {
                            moveToNextToken();
                            expression();
                        }
                    }
                    if (currentToken == CLOSEPAREN) {
                        moveToNextToken();
                    } else generateError(CLOSE_PAREN_NOT_FOUND);
                }
            }
        } else {
            /*
            TODO: this code may be never be reached, as we already checked for let in statement, need to identify a
            TODO: pattern to handle these kind of duplicate code
             */
            generateError(CALL_NOT_FOUND);
        }
    }

    public void ifStatement() throws IOException {
        if (currentToken == IF) {
            moveToNextToken();
            relation(); //TODO Need to handle with result of relation() while generating instruction sets
            if (currentToken == THEN) {
                moveToNextToken();
                statSequence();
                if (currentToken == ELSE) {
                    moveToNextToken();
                    statSequence();
                }
                if (currentToken == FI) {
                    moveToNextToken();
                } else generateError(IF_STATEMENT_ERROR);
            } else {
                generateError(IF_STATEMENT_ERROR);
            }
        } else {
            /*
            TODO: this code may be never be reached, as we already checked for let in statement, need to identify a
            TODO: pattern to handle these kind of duplicate code
             */
            generateError(IF_STATEMENT_ERROR);
        }

    }

    public void relation() throws IOException {
        expression();
        if (isTokenRelOp(currentToken)) {
            moveToNextToken();
            expression();
        } else {
            generateError(RELATION_OP_NOT_FOUND);
        }
    }

    public void whileStatement() throws IOException {
        if (currentToken == WHILE) {
            moveToNextToken();
            relation();
            if (currentToken == DO) {
                moveToNextToken();
                statSequence();
                if (currentToken == OD) {
                    moveToNextToken();
                } else generateError(OD_EXPECTED);
            } else generateError(DO_EXPECTED);
        } else {
            generateError(WHILE_STATEMENT_ERROR);
        }

    }

    public void returnStatement() throws IOException {
        if(currentToken == RETURN){
            moveToNextToken();
            // expression -> term -> factor -> designator -> identifier
//            System.out.println("returnStatement - identifier - " + currentToken);
            expression();
//            System.out.println("Returing from returnStatement");
        } else generateError(RETURN_EXPECTED);

    }

    public boolean isTokenRelOp(Token currentToken) {
        if (currentToken == EQL || currentToken == NEQ ||
                currentToken == LSS || currentToken == LEQ || currentToken == GTR || currentToken == GEQ) return true;
        return false;
    }

    public void generateError(ErrorMessage message) {
        System.out.println("Syntax Error occurred - " + message);
        System.exit(1);
    }
}
