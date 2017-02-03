package main.edu.uci.compiler.parser;

import com.sun.org.apache.regexp.internal.RE;
import main.edu.uci.compiler.cfg.ControlFlowGraph;
import main.edu.uci.compiler.model.*;
import main.edu.uci.compiler.parser.InstructionGenerator.*;
import sun.jvm.hotspot.debugger.bsd.amd64.BsdAMD64CFrame;

import static main.edu.uci.compiler.model.Token.*;
import static main.edu.uci.compiler.model.ErrorMessage.*;
import static main.edu.uci.compiler.model.Result.KIND.*;
import static main.edu.uci.compiler.model.BasicBlock.Type.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by srikrishna on 1/27/17.
 */
public class Parser {
    private Scanner scanner;
    private Token currentToken;
    private ControlFlowGraph cfg;
    private InstructionGenerator ig;
    ArrayList<Token> relOpList = new ArrayList<Token>() {{
        add(EQL);
        add(NEQ);
        add(LSS);
        add(LEQ);
        add(GTR);
        add(GEQ);
    }};
    ArrayList<Token> statSeqList = new ArrayList<Token>(){{
        add(LET);
        add(CALL);
        add(IF);
        add(WHILE);
        add(RETURN);
    }};

    public Parser(String fileName) throws IOException {
        scanner = new Scanner(fileName);
        currentToken = scanner.getToken();
        cfg = new ControlFlowGraph();
        ig = new InstructionGenerator();
    }

    private void moveToNextToken() throws IOException {
        currentToken = scanner.getToken();
    }

    public void computation() throws IOException {
        if (currentToken == MAIN) {
            moveToNextToken();
            BasicBlock basicBlock = cfg.getBasicBlock();
            while (currentToken == VAR || currentToken == ARRAY) {
                //TODO: Deal with array's later
                // Need not move to next token, it is handled by varDecl
                varDecl();
            }
            while (currentToken == FUNCTION || currentToken == PROCEDURE) {
                // Need not move to next token, it is handled by funcDecl
                funcDecl(basicBlock);
            }
            if (currentToken == BEGIN) {
                moveToNextToken();
                basicBlock = statSequence(basicBlock);
                if (currentToken == END) {
                    moveToNextToken();
                    if (currentToken == PERIOD) {
                        moveToNextToken();
                        basicBlock.addInstruction(ig.generateEndInstruction());
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
                while (currentToken == OPENBRACKET) {
                    moveToNextToken();
                    arrayDimensions.add(number());
                    if (currentToken == CLOSEBRACKET) {
                        moveToNextToken();
                    } else generateError(TYPE_DECL_ERROR);
                }
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

    public void funcDecl(BasicBlock basicBlock) throws IOException {
        if (currentToken == FUNCTION || currentToken == PROCEDURE) {
            moveToNextToken();
            if (currentToken == IDENTIFIER) {
                moveToNextToken();
                formalParam(); // formalParam, handles of moving to next token
                if (currentToken == SEMICOLON) {
                    moveToNextToken();
                    funcBody(basicBlock);
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

    public void funcBody(BasicBlock basicBlock) throws IOException {
        while (currentToken == VAR || currentToken == ARRAY) varDecl();
        if (currentToken == BEGIN) {
            moveToNextToken();
            statSequence(basicBlock);
            if (currentToken == END) {
                moveToNextToken();
            } else generateError(END_NOT_FOUND);
        } else generateError(BEGIN_NOT_FOUND);
    }

    public BasicBlock statSequence(BasicBlock basicBlock) throws IOException {
        basicBlock = statement(basicBlock);
        while (currentToken == SEMICOLON) {
            moveToNextToken();
            basicBlock = statement(basicBlock);
        }
        return basicBlock;
    }

    public BasicBlock statement(BasicBlock basicBlock) throws IOException {
        if(!(statSeqList.contains(currentToken))) generateError(KEYWORD_EXPECTED);

        if(currentToken == IF){
            return ifStatement(basicBlock);
        }
        if(currentToken == WHILE){
            return whileStatement(basicBlock);
        }
        if (currentToken == LET) {
            assignment(basicBlock);
        } else if (currentToken == CALL) {
            funcCall(basicBlock);
        } else if (currentToken == RETURN) {
            returnStatement(basicBlock);
        }
        return basicBlock;

    }

    public Result assignment(BasicBlock basicBlock) throws IOException {

        Result lhs = null, rhs = null;
        if (currentToken == LET) {
            moveToNextToken();
            lhs = designator(basicBlock);
            if (currentToken == BECOMES) {
                moveToNextToken();
                rhs = expression(basicBlock);
                Instruction instruction = ig.generateInstructionForAssignment(lhs, rhs);
                basicBlock.addInstruction(instruction);
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
        return lhs;
    }

    public Result designator(BasicBlock basicBlock) throws IOException {
        Result res = null;
        if (currentToken == IDENTIFIER) {
            res = new Result();
            res.setKind(VARIABLE);
            res.setIdentifierName(scanner.getCurrentIdentifier());
            moveToNextToken();
            while (currentToken == OPENBRACKET) {
                //TODO: Need to deal with arrays
                moveToNextToken();
                expression(basicBlock);
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

    public Result expression(BasicBlock basicBlock) throws IOException {
        Result lhs = term(basicBlock);
        while (currentToken == PLUS || currentToken == MINUS) {
            Token prevToken = currentToken;
            moveToNextToken();
            Result rhs = term(basicBlock);
            lhs = ig.computeExpression(prevToken, lhs, rhs);
            if (lhs.getKind() == INSTRUCTION) {
                basicBlock.addInstruction(ig.getInstruction(lhs.getInstructionId()));
                lhs.setBasicBlockId(basicBlock.getId());
            }
        }
        return lhs;
    }

    public Result term(BasicBlock basicBlock) throws IOException {
        Result lhs = factor(basicBlock);
        while (currentToken == TIMES || currentToken == DIV) {
            Token prevToken = currentToken;
            moveToNextToken();
            Result rhs = factor(basicBlock);
            lhs = ig.computeExpression(prevToken, lhs, rhs);
            if (lhs.getKind() == INSTRUCTION) {
                basicBlock.addInstruction(ig.getInstruction(lhs.getInstructionId()));
                lhs.setBasicBlockId(basicBlock.getId());
            }
        }
        return lhs;
    }

    public Result factor(BasicBlock basicBlock) throws IOException {
        Result result = null;
        if (currentToken == IDENTIFIER) {
            //TODO: Need to deal with identifiers
            result = designator(basicBlock);
        } else if (currentToken == NUMBER) {
            //TODO: Need to deal with number
            result = new Result();
            result.setKind(CONSTANT);
            result.setValue(number());
        } else if (currentToken == CALL) {
            //TODO: Need to deal with function calls
            result = funcCall(basicBlock);
        } else if (currentToken == OPENPAREN) {
            moveToNextToken();
            result = expression(basicBlock);
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

    public Result funcCall(BasicBlock basicBlock) throws IOException {
        Result res = null;
        if (currentToken == CALL) {
            moveToNextToken();
            if (currentToken == IDENTIFIER) {
                moveToNextToken();
                if (currentToken == OPENPAREN) {
                    moveToNextToken();
                    // Go from expression -> term -> factor -> designator -> identifier
                    if (currentToken != CLOSEPAREN) {
                        expression(basicBlock);
                        while (currentToken == COMMA) {
                            moveToNextToken();
                            expression(basicBlock);
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
        return res;
    }

    public BasicBlock ifStatement(BasicBlock basicBlock) throws IOException {
        if(currentToken != IF) generateError(IF_STATEMENT_ERROR);
        moveToNextToken();

        BasicBlock ifConditionBlock = new BasicBlock();
        ifConditionBlock.setType(BB_IF_CONDITION);
        ifConditionBlock.addParent(basicBlock);
        basicBlock.addChildren(ifConditionBlock);

        BasicBlock ifThenBlock = new BasicBlock();
        ifThenBlock.setType(BB_IF_THEN);
        ifThenBlock.addParent(ifConditionBlock);
        ifConditionBlock.addChildren(ifThenBlock);

        BasicBlock joinBlock = new BasicBlock();
        joinBlock.setType(BB_IF_JOIN);
        joinBlock.addParent(ifThenBlock);
        ifThenBlock.addChildren(joinBlock);

        BasicBlock elseBlock = null;
        Result fixUpResult = relation(ifConditionBlock);
        if(currentToken != THEN) generateError(THEN_STATEMENT_ERROR);
        moveToNextToken();

        ifThenBlock = statSequence(ifThenBlock);

        if(currentToken == ELSE) {
            moveToNextToken();
            elseBlock = new BasicBlock();
            elseBlock.setType(BB_ELSE);
            elseBlock.addParent(ifConditionBlock);
            elseBlock.addChildren(joinBlock);

            ifConditionBlock.addChildren(elseBlock);

            joinBlock.addParent(elseBlock);
            joinBlock.setType(BB_IF_ELSE_JOIN);

            elseBlock = statSequence(elseBlock);
        }

        if(currentToken == FI){
            moveToNextToken();
            // BRA instruction from IF Block to JOIN Block
            addBranchInstruction(ifThenBlock, joinBlock);

        } else generateError(FI_STATEMENT_ERROR);

        if(elseBlock != null){
            fixUpNegCompareInstruction(fixUpResult, elseBlock);
            return elseBlock;
        }
        ifConditionBlock.addChildren(joinBlock);
        joinBlock.addParent(ifConditionBlock);
        fixUpNegCompareInstruction(fixUpResult, joinBlock);
        return joinBlock;
    }

    public Result relation(BasicBlock basicBlock) throws IOException {
        Result lhs = expression(basicBlock);
        Result condition = relOp();
        Result rhs = expression(basicBlock);
        RelationResult res = ig.computeRelation(condition, lhs, rhs);
        basicBlock.addInstruction(res.compareInstruction);
        basicBlock.addInstruction(res.negCompareInstruction);
        return res.fixUpResult;
    }

    public BasicBlock whileStatement(BasicBlock basicBlock) throws IOException {
        if(currentToken != WHILE) generateError(WHILE_STATEMENT_ERROR);
        moveToNextToken();

        BasicBlock whileConditionBlock = new BasicBlock();
        whileConditionBlock.setType(BB_WHILE);
        whileConditionBlock.addParent(basicBlock);
        basicBlock.addChildren(whileConditionBlock);

        BasicBlock whileJoinBlock = new BasicBlock();
        whileJoinBlock.setType(BB_WHILE_JOIN);
        whileJoinBlock.addParent(whileConditionBlock);

        whileConditionBlock.addChildren(whileJoinBlock);

        Result fixUpResult = relation(whileConditionBlock);
        fixUpNegCompareInstruction(fixUpResult, whileJoinBlock);

        if(currentToken != DO) generateError(DO_EXPECTED);

        moveToNextToken();
        BasicBlock whileBodyBlock = new BasicBlock();
        whileBodyBlock.setType(BB_WHILE_BODY);
        whileBodyBlock.addParent(whileConditionBlock);
        whileBodyBlock.addChildren(whileConditionBlock);

        whileConditionBlock.addChildren(whileBodyBlock);

        whileBodyBlock = statSequence(whileBodyBlock);
        //Go Back to while condition -> adding instruction for that
        addBranchInstruction(whileBodyBlock, whileConditionBlock);
        if (currentToken != OD) generateError(OD_EXPECTED);
        moveToNextToken();
        return whileJoinBlock;
    }

    public void returnStatement(BasicBlock basicBlock) throws IOException {
        if (currentToken == RETURN) {
            moveToNextToken();
            expression(basicBlock);
        } else generateError(RETURN_EXPECTED);

    }

    public Result relOp() throws IOException {
        if((!relOpList.contains(currentToken))) generateError(RELATION_OP_NOT_FOUND);
        Result relOpResult = new Result();
        relOpResult.setKind(CONDITION);
        relOpResult.setCondition(currentToken);
        moveToNextToken();
        return relOpResult;
    }

    private void fixUpNegCompareInstruction(Result fixUpResult, BasicBlock joinBlock){
        ig.getInstruction(fixUpResult.getFixUpInstructionId()).getOperand2().setBasicBlockId(joinBlock.getId());
    }

    private void addBranchInstruction(BasicBlock fromBlock, BasicBlock gotoBlock){
        Instruction branchInstruction = ig.generateBranchInstruction();
        branchInstruction.getOperand1().setBasicBlockId(gotoBlock.getId());
        fromBlock.addInstruction(branchInstruction);
    }

    public void generateError(ErrorMessage message) {
        System.out.println("Syntax Error occurred - " + message);
        System.exit(1);
    }
}
