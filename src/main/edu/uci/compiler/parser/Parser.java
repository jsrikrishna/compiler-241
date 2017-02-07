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
import java.util.HashMap;

/**
 * Created by srikrishna on 1/27/17.
 */
public class Parser {
    private Scanner scanner;
    private Token currentToken;
    private ControlFlowGraph cfg;
    private InstructionGenerator ig;
    ArrayList<Token> relOpList;
    ArrayList<Token> statSeqList;
    private SSA ssaTracker;
    private HashMap<String, ArrayList<Integer>> arrayVariables;

    public Parser(String fileName) throws IOException {
        scanner = new Scanner(fileName);
        currentToken = scanner.getToken();
        cfg = new ControlFlowGraph();
        ig = new InstructionGenerator();
        relOpList = new ArrayList<Token>() {{
            add(EQL);
            add(NEQ);
            add(LSS);
            add(LEQ);
            add(GTR);
            add(GEQ);
        }};
        statSeqList = new ArrayList<Token>() {{
            add(LET);
            add(CALL);
            add(IF);
            add(WHILE);
            add(RETURN);
        }};
        arrayVariables = new HashMap<>();
        ssaTracker = SSA.getInstance();
    }

    private void moveToNextToken() throws IOException {
        currentToken = scanner.getToken();
    }

    public void computation() throws IOException {
        if (currentToken != MAIN) generateError(MAIN_NOT_FOUND);
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
        if (currentToken != BEGIN) generateError(BEGIN_NOT_FOUND);
        moveToNextToken();
        basicBlock = statSequence(basicBlock);

        if (currentToken != END) generateError(END_NOT_FOUND);
        moveToNextToken();

        if (currentToken != PERIOD) generateError(PERIOD_NOT_FOUND);
        moveToNextToken();
        basicBlock.addInstruction(ig.generateEndInstruction());
    }


    public void varDecl() throws IOException {
        ArrayList<Integer> arrayDimensions = typeDecl();
        if (currentToken != IDENTIFIER) generateError(VARIABLE_DECL_ERROR);
        storeVariables(arrayDimensions);
        moveToNextToken();
        while (currentToken == COMMA) {
            moveToNextToken();
            if (currentToken != IDENTIFIER) generateError(VARIABLE_DECL_ERROR);
            storeVariables(arrayDimensions);
            moveToNextToken();
        }
        if (currentToken != SEMICOLON) generateError(SEMICOLON_NOT_FOUND);
        moveToNextToken();
    }

    public void storeVariables(ArrayList<Integer> arrayDimensions) {
        if (arrayDimensions != null) {
            arrayVariables.put(scanner.getCurrentIdentifier(), arrayDimensions);
        } else {
            // Add to SSA data structure
        }
    }

    public ArrayList<Integer> typeDecl() throws IOException {
        if (currentToken != VAR && currentToken != ARRAY) generateError(TYPE_DECL_ERROR);
        if (currentToken == VAR) {
            moveToNextToken();
            return null;
        }
        moveToNextToken();
        ArrayList<Integer> arrayDimensions = new ArrayList<>();
        if (currentToken != OPENBRACKET) generateError(OPEN_BRACKET_NOT_FOUND);
        while (currentToken == OPENBRACKET) {
            moveToNextToken();
            arrayDimensions.add(number());
            if (currentToken != CLOSEBRACKET) generateError(TYPE_DECL_ERROR);
            moveToNextToken();
        }
        return arrayDimensions;
    }

    public int number() throws IOException {
        if (currentToken != NUMBER) generateError(NUMBER_EXPECTED);
        moveToNextToken();
        return scanner.getCurrentNumber();
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
        if (!(statSeqList.contains(currentToken))) generateError(KEYWORD_EXPECTED);

        if (currentToken == IF) {
            return ifStatement(basicBlock);
        }
        if (currentToken == WHILE) {
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
        if (currentToken != LET) generateError(ASSIGNMENT_ERROR);
        moveToNextToken();
        Result lhs = designator(basicBlock);
        if (currentToken != BECOMES) generateError(BECOMES_NOT_FOUND);
        moveToNextToken();
        Result rhs = expression(basicBlock);
        Instruction instruction = ig.generateInstructionForAssignment(lhs, rhs);
        /*
        Update lhs result with SSA
        Update SSA for local result, both basic block level and global level
        TODO: Q - do we need to update lhs globally as well ?
        TODO: Q - Does array as well have SSA ?
         */
        ssaTracker.updateSSAForVariable(lhs.getIdentifierName(), instruction.getInstructionId());
        lhs.setSsaVersion(instruction.getInstructionId());
        basicBlock.updateLocalSSAVersion(lhs.getIdentifierName(), instruction.getInstructionId());
        /*
        Update rhs result with SSA
         */
        Integer localSSAVersion = basicBlock.getSSAVersion(rhs.getIdentifierName());
        if(localSSAVersion == null){
            rhs.setSsaVersion(ssaTracker.getSSAVersion(rhs.getIdentifierName()));
        } else rhs.setSsaVersion(localSSAVersion);

        basicBlock.addInstruction(instruction);

        return lhs;
    }

    public Result designator(BasicBlock basicBlock) throws IOException {
        if (currentToken != IDENTIFIER) generateError(DESIGNATOR_ERROR);
        // Could be array variable or a normal variable
        Result res = new Result();
        if (arrayVariables.containsKey(scanner.getCurrentIdentifier())) {
            String arrayIdentifier = scanner.getCurrentIdentifier();
            ArrayList<Integer> dimensions = arrayVariables.get(scanner.getCurrentIdentifier());
            ArrayList<Result> dimensionExp = new ArrayList<>();
            moveToNextToken();
            while (currentToken == OPENBRACKET) {
                moveToNextToken();
                Result dimensionResult = expression(basicBlock);
                dimensionExp.add(dimensionResult);
                //TODO: it could be CLOSE_BRACKET_NOT_FOUND, need to design error messages
                if (currentToken != CLOSEBRACKET) generateError(DESIGNATOR_ERROR);
                moveToNextToken();
            }
            // Need to generate instructions and return the final Result that contains the instruction id
            if (dimensionExp.size() != dimensions.size()) generateError(ARRAY_DIMENSION_MISMATCH);
            Result arrayDimensionResult = handleArrayDimInstructions(dimensionExp, dimensions, basicBlock);
            Result indexInArray = handleArrayDesignator(arrayDimensionResult, arrayIdentifier, basicBlock);
            return indexInArray;
        }
        String identifier = scanner.getCurrentIdentifier();
        res.setKind(VARIABLE);
        res.setIdentifierName(identifier);
        // ToDo: Q - Need to check, may be null as well, for assignment - its a different process, this will be used for terms or factors
        res.setSsaVersion(basicBlock.getSSAVersion(identifier));
        // Need to add ssa-version to result from the instruction number or figure out
        moveToNextToken();
        return res;
    }

    private Result handleArrayDimInstructions(ArrayList<Result> dimExp, ArrayList<Integer> dims, BasicBlock basicBlock) {
        ArrayBase res = ig.generateInstructionsForArrDim(dimExp, dims);
        for (Integer instrId : res.instructionIds) basicBlock.addInstruction(ig.getInstruction(instrId));
        return res.finalResult;
    }

    private Result handleArrayDesignator(Result dimResult, String identifier, BasicBlock basicBlock) {
        ArrayBase res = ig.computeArrayDesignator(dimResult, identifier);
        for (Integer instrId : res.instructionIds) basicBlock.addInstruction(ig.getInstruction(instrId));
        return res.finalResult;
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
        if (currentToken != IF) generateError(IF_STATEMENT_ERROR);
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
        if (currentToken != THEN) generateError(THEN_STATEMENT_ERROR);
        moveToNextToken();

        ifThenBlock = statSequence(ifThenBlock);

        if (currentToken == ELSE) {
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

        if (currentToken == FI) {
            moveToNextToken();
            // BRA instruction from IF Block to JOIN Block
            addBranchInstruction(ifThenBlock, joinBlock);

        } else generateError(FI_STATEMENT_ERROR);

        if (elseBlock != null) {
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
        if (currentToken != WHILE) generateError(WHILE_STATEMENT_ERROR);
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

        if (currentToken != DO) generateError(DO_EXPECTED);

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
        if (currentToken != RETURN) generateError(RETURN_EXPECTED);
        moveToNextToken();
        expression(basicBlock);
    }

    public Result relOp() throws IOException {
        if ((!relOpList.contains(currentToken))) generateError(RELATION_OP_NOT_FOUND);
        Result relOpResult = new Result();
        relOpResult.setKind(CONDITION);
        relOpResult.setCondition(currentToken);
        moveToNextToken();
        return relOpResult;
    }

    private void fixUpNegCompareInstruction(Result fixUpResult, BasicBlock joinBlock) {
        ig.getInstruction(fixUpResult.getFixUpInstructionId()).getOperand2().setBasicBlockId(joinBlock.getId());
    }

    private void addBranchInstruction(BasicBlock fromBlock, BasicBlock gotoBlock) {
        Instruction branchInstruction = ig.generateBranchInstruction();
        branchInstruction.getOperand1().setBasicBlockId(gotoBlock.getId());
        fromBlock.addInstruction(branchInstruction);
    }

    public void generateError(ErrorMessage message) {
        System.out.println("Syntax Error occurred - " + message);
        System.exit(1);
    }
}
