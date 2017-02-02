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
                if (currentToken == IDENTIFIER) {
                    moveToNextToken();
                    //TODO: need to store the variable, it could be an array variable or normal variable
                } else {
                    generateError(VARIABLE_DECL_ERROR);
                }
            }
            if (currentToken == SEMICOLON) {
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
        //TODO: Code never reach here though if we exit in generateError(), need to decide what to do
        return -1;
    }

    public void funcDecl() throws IOException {
        if (currentToken == FUNCTION || currentToken == PROCEDURE) {
            moveToNextToken();
            if (currentToken == IDENTIFIER) {
                moveToNextToken();
                formalParam(); // formalParam, handles of moving to next token
                if (currentToken == SEMICOLON) {
                    moveToNextToken();
                    funcBody();
                    if (currentToken == SEMICOLON) moveToNextToken();
                    else generateError(SEMICOLON_NOT_FOUND);
                } else generateError(SEMICOLON_NOT_FOUND);
            } else generateError(IDENTIFIER_NOT_FOUND);
        } else generateError(FUNCTION_PROCEDURE_NOT_FOUND);
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
    }

    public void funcBody() throws IOException {
        while (currentToken == VAR || currentToken == ARRAY) varDecl();
        if (currentToken == BEGIN) {
            moveToNextToken();
            statSequence();
            if (currentToken == END) {
                moveToNextToken();
            } else generateError(END_NOT_FOUND);
        } else generateError(BEGIN_NOT_FOUND);
    }

    public void statSequence() throws IOException {
        statement();
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
        Result lhs = null, rhs = null;
        if (currentToken == LET) {
            moveToNextToken();
            lhs = designator();
            if (currentToken == BECOMES) {
                moveToNextToken();
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
            moveToNextToken();
            while (currentToken == OPENBRACKET) ;
            {
                //TODO: Need to deal with arrays
                moveToNextToken();
                expression();
                if (currentToken == CLOSEBRACKET) {
                    moveToNextToken();
                } else {
                    //TODO: it could be CLOSE_BRACKET_NOT_FOUND, need to design error messages
                    generateError(DESIGNATOR_ERROR);
                }
            }
        } else {
            generateError(DESIGNATOR_ERROR);
        }
        return res;
    }

    public Result expression() throws IOException {
        Result res = term();
        while (currentToken == PLUS || currentToken == MINUS) {
            term();
        }
        return res;
    }

    public Result term() throws IOException {
        Result lhs = factor();
        while (currentToken == TIMES || currentToken == DIV) {
            factor();
        }
        return lhs;
    }

    public Result factor() throws IOException {
        Result result = null;
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
            expression();
            if (currentToken == CLOSEPAREN) {
                moveToNextToken();
            } else {
                //TODO: Can be CLOSE_PAREN_NOT_FOUND, again needs to design the error message
                //TODO: Done i guess, but still keeping to clarify
                generateError(CLOSE_PAREN_NOT_FOUND);
            }
        } else generateError(FACTOR_ERROR);
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
                    // Go from expression -> term -> factor -> designator -> identifier
                    if (currentToken == IDENTIFIER) {
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
            if(currentToken == IDENTIFIER){
                expression();
            }
        } else generateError(RETURN_EXPECTED);

    }

    public boolean isTokenRelOp(Token currentToken) {
        if (currentToken == EQL || currentToken == NEQ ||
                currentToken == LSS || currentToken == LEQ || currentToken == GTR || currentToken == GEQ) return true;
        return false;
    }

    public void generateError(ErrorMessage message) {
        System.out.println("Syntax Error occurred - " + message);
    }
}
