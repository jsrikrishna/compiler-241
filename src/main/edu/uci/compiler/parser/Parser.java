package main.edu.uci.compiler.parser;

import main.edu.uci.compiler.cfg.ControlFlowGraph;
import main.edu.uci.compiler.model.*;
import main.edu.uci.compiler.parser.InstructionGenerator.*;

import static main.edu.uci.compiler.model.Token.*;
import static main.edu.uci.compiler.model.ErrorMessage.*;
import static main.edu.uci.compiler.model.Result.KIND.*;
import static main.edu.uci.compiler.model.BasicBlock.Type.*;

import java.io.IOException;
import java.util.ArrayList;

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
    private Tracker tracker;


    public Parser(String fileName) throws IOException {
        scanner = new Scanner(fileName);
        currentToken = scanner.getToken();
        cfg = new ControlFlowGraph();
        tracker = new Tracker();
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
    }

    private void moveToNextToken() throws IOException {
        currentToken = scanner.getToken();
    }

    public void computation() throws IOException {
        if (currentToken != MAIN) generateError(MAIN_NOT_FOUND);
        moveToNextToken();
        BasicBlock startBasicBlock = cfg.getBasicBlock();
        while (currentToken == VAR || currentToken == ARRAY) {
            //TODO: Deal with array's later
            // Need not move to next token, it is handled by varDecl
            varDecl(null, startBasicBlock);
        }
        while (currentToken == Token.FUNCTION || currentToken == PROCEDURE) {
            // Need not move to next token, it is handled by funcDecl
            funcDecl(startBasicBlock);
        }
        if (currentToken != BEGIN) generateError(BEGIN_NOT_FOUND);
        moveToNextToken();
        startBasicBlock = statSequence(startBasicBlock, null);

        if (currentToken != END) generateError(END_NOT_FOUND);
        moveToNextToken();

        if (currentToken != PERIOD) generateError(PERIOD_NOT_FOUND);
        moveToNextToken();
        startBasicBlock.addInstruction(ig.generateEndInstruction());
        cfg.printBasicBlocks(startBasicBlock);
    }


    public void varDecl(Function function, BasicBlock basicBlock) throws IOException {
        ArrayList<Integer> arrayDimensions = typeDecl();
        if (currentToken != IDENTIFIER) generateError(VARIABLE_DECL_ERROR);
        storeVariables(arrayDimensions, function, basicBlock);
        moveToNextToken();
        while (currentToken == COMMA) {
            moveToNextToken();
            if (currentToken != IDENTIFIER) generateError(VARIABLE_DECL_ERROR);
            storeVariables(arrayDimensions, function, basicBlock);
            moveToNextToken();
        }
        if (currentToken != SEMICOLON) generateError(SEMICOLON_NOT_FOUND);
        moveToNextToken();
    }

    public void storeVariables(ArrayList<Integer> arrayDimensions, Function function, BasicBlock basicBlock) {
        String identifier = scanner.getCurrentIdentifier();
        if (function != null) {
            if (arrayDimensions != null) {
                function.addLocalArrayVariable(identifier, arrayDimensions);
            } else {
                Instruction instruction = ig.generateInstructionToInitVar(identifier);
                instruction.getOperand2().setSsaVersion(instruction.getInstructionId());
                function.addLocalSSAVariable(identifier, instruction.getInstructionId());
                // May be normal block as well, need to check -> checked, need not do, it will be either func or non-func
                basicBlock.addInstruction(instruction);
            }

        } else {
            if (arrayDimensions != null) {
                tracker.addArrayVariable(identifier, arrayDimensions);
            } else {
                Instruction instruction = ig.generateInstructionToInitVar(identifier);
                tracker.updateSSAForVariable(identifier, instruction.getInstructionId());
                instruction.getOperand2().setSsaVersion(tracker.getSSAVersion(identifier));
                basicBlock.addInstruction(instruction);
            }
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
        if (currentToken == Token.FUNCTION || currentToken == PROCEDURE) {
            moveToNextToken();
            if (currentToken == IDENTIFIER) {
                String identifier = scanner.getCurrentIdentifier();
                Function function = new Function(identifier);
                tracker.addFunction(identifier, function);
                moveToNextToken();
                formalParam(function); // formalParam, handles of moving to next token
                if (currentToken == SEMICOLON) {
                    moveToNextToken();
                    funcBody(function);
                    if (currentToken == SEMICOLON) moveToNextToken();
                    else generateError(SEMICOLON_NOT_FOUND);
                } else generateError(SEMICOLON_NOT_FOUND);
            } else generateError(IDENTIFIER_NOT_FOUND);
        } else generateError(FUNCTION_PROCEDURE_NOT_FOUND);
    }

    public void formalParam(Function function) throws IOException {
        if (currentToken == OPENPAREN) {
            moveToNextToken();
            if (currentToken == IDENTIFIER) {
                //TODO: need to store the variable at common place
                addFunctionParameters(function);
                moveToNextToken();
                while (currentToken == COMMA) {
                    moveToNextToken();
                    if (currentToken == IDENTIFIER) {
                        //TODO: need to store the variable at common place
                        addFunctionParameters(function);
                        moveToNextToken();
                    } else generateError(FORMAL_PARAM_DECL_ERROR);
                }
            }
            if (currentToken == CLOSEPAREN) {
                moveToNextToken();
            } else generateError(FORMAL_PARAM_DECL_ERROR);
        }
    }

    private void addFunctionParameters(Function function) {
        String identifier = scanner.getCurrentIdentifier();
        Integer ssaVersion = function.getLocalSSAForVariable(identifier);
        if(ssaVersion == null){
            ssaVersion = tracker.getSSAVersion(identifier);
            if(ssaVersion == null){
                generateError(FUNC_PARAM_NOT_DECLARED);
            }
        }
        function.addLocalSSAVariable(identifier, ssaVersion);
        function.setFuncParameter(identifier);
    }

    public void funcBody(Function function) throws IOException {
        while (currentToken == VAR || currentToken == ARRAY) varDecl(function, function.getFuncBasicBlock());
        if (currentToken == BEGIN) {
            moveToNextToken();
            BasicBlock finalBlock = statSequence(function.getFuncBasicBlock(), function);
            if (currentToken == END) {
                moveToNextToken();
            } else generateError(END_NOT_FOUND);
        } else generateError(BEGIN_NOT_FOUND);
    }

    public BasicBlock statSequence(BasicBlock basicBlock, Function function) throws IOException {
        basicBlock = statement(basicBlock, function);
        while (currentToken == SEMICOLON) {
            moveToNextToken();
            basicBlock = statement(basicBlock, function);
        }
        return basicBlock;
    }

    public BasicBlock statement(BasicBlock basicBlock, Function function) throws IOException {
        if (!(statSeqList.contains(currentToken))) generateError(KEYWORD_EXPECTED);

        if (currentToken == IF) {
            return ifStatement(basicBlock, function);
        }
        if (currentToken == WHILE) {
            return whileStatement(basicBlock, function);
        }
        if (currentToken == LET) {
            assignment(basicBlock, function);
        } else if (currentToken == CALL) {
            funcCall(basicBlock);
        } else if (currentToken == RETURN) {
            returnStatement(basicBlock, function);
        }
        return basicBlock;

    }

    public Result assignment(BasicBlock basicBlock, Function function) throws IOException {
        if (currentToken != LET) generateError(ASSIGNMENT_ERROR);
        moveToNextToken();
        Result lhs = designator(basicBlock, function);
        if (currentToken != BECOMES) generateError(BECOMES_NOT_FOUND);
        moveToNextToken();
        Result rhs = expression(basicBlock, function);
        Instruction instruction = ig.generateInstructionForAssignment(lhs, rhs);
        /*
        Update lhs result with Tracker
        Update Tracker for local result, both basic block level and global level
        TODO: Q - do we need to update lhs globally as well ?
        TODO: Q - Does array as well have Tracker ?
         */
        tracker.updateSSAForVariable(lhs.getIdentifierName(), instruction.getInstructionId());
        lhs.setSsaVersion(instruction.getInstructionId());
        basicBlock.updateLocalSSAVersion(lhs.getIdentifierName(), instruction.getInstructionId());
        if(function != null){
            function.addLocalSSAVariable(lhs.getIdentifierName(), instruction.getInstructionId());
        }
        /*
        Update rhs result with Tracker
         */
        if (rhs.getKind() == VARIABLE) {
            Integer localSSAVersion = basicBlock.getSSAVersion(rhs.getIdentifierName());
            if (localSSAVersion == null) {
                rhs.setSsaVersion(tracker.getSSAVersion(rhs.getIdentifierName()));
            } else rhs.setSsaVersion(localSSAVersion);
        }


        basicBlock.addInstruction(instruction);

        return lhs;
    }

    public Result designator(BasicBlock basicBlock, Function function) throws IOException {
        if (currentToken != IDENTIFIER) generateError(DESIGNATOR_ERROR);
        // Could be array variable or a normal variable
        Result res = new Result();
        String identifier = scanner.getCurrentIdentifier();

        boolean isArrayInFunction = function != null && function.getLocalArrayVariable(identifier) != null;
        boolean isArrayInMain = tracker.containsArrayVariable(identifier);
        if (isArrayInFunction || isArrayInMain) {
            ArrayList<Integer> dimensions = null;
            if (function != null) {
                dimensions = function.getLocalArrayVariable(identifier);
            }
            if (dimensions == null) {
                dimensions = tracker.getArrayVariableDimensions(identifier);
            }
            ArrayList<Result> dimensionExp = new ArrayList<>();
            moveToNextToken();
            while (currentToken == OPENBRACKET) {
                moveToNextToken();
                Result dimensionResult = expression(basicBlock, function);
                dimensionExp.add(dimensionResult);
                //TODO: it could be CLOSE_BRACKET_NOT_FOUND, need to design error messages
                if (currentToken != CLOSEBRACKET) generateError(DESIGNATOR_ERROR);
                moveToNextToken();
            }
            // Need to generate instructions and return the final Result that contains the instruction id
            if (dimensionExp.size() != dimensions.size()) generateError(ARRAY_DIMENSION_MISMATCH);
            Result arrayDimensionResult = handleArrayDimInstructions(dimensionExp, dimensions, basicBlock);
            Result indexInArray = handleArrayDesignator(arrayDimensionResult, identifier, basicBlock);
            return indexInArray;
        } else {
            // ToDo: Q - Need to check, may be null as well,
            // for assignment - its a different process, this will be used for terms or factors
            // Need to add ssa-version to result from the instruction number or figure out
            res.setKind(VARIABLE);
            res.setIdentifierName(identifier);
            if (function != null) {
                Integer ssaVersion = function.getLocalSSAForVariable(identifier);//Need to perform a check here, compared with old code
                if (ssaVersion == null) {
                    ssaVersion = tracker.getSSAVersion(identifier);
                    if (ssaVersion == null) generateError(VARIABLE_NOT_DECLARED);
                }
                res.setSsaVersion(ssaVersion);
            } else {
                res.setSsaVersion(basicBlock.getSSAVersion(identifier));
            }
        }

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

    public Result expression(BasicBlock basicBlock, Function function) throws IOException {
        Result lhs = term(basicBlock, function);
        while (currentToken == PLUS || currentToken == MINUS) {
            Token prevToken = currentToken;
            moveToNextToken();
            Result rhs = term(basicBlock, function);
            lhs = ig.computeExpression(prevToken, lhs, rhs);
            if (lhs.getKind() == INSTRUCTION) {
                basicBlock.addInstruction(ig.getInstruction(lhs.getInstructionId()));
                lhs.setBasicBlockId(basicBlock.getId());
            }
        }
        return lhs;
    }

    public Result term(BasicBlock basicBlock, Function function) throws IOException {
        Result lhs = factor(basicBlock, function);
        while (currentToken == TIMES || currentToken == DIV) {
            Token prevToken = currentToken;
            moveToNextToken();
            Result rhs = factor(basicBlock, function);
            lhs = ig.computeExpression(prevToken, lhs, rhs);
            if (lhs.getKind() == INSTRUCTION) {
                basicBlock.addInstruction(ig.getInstruction(lhs.getInstructionId()));
                lhs.setBasicBlockId(basicBlock.getId());
            }
        }
        return lhs;
    }

    public Result factor(BasicBlock basicBlock, Function function) throws IOException {
        Result result = null;
        if (currentToken == IDENTIFIER) {
            //TODO: Need to deal with identifiers
            result = designator(basicBlock, function);
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
            result = expression(basicBlock, function);
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
        /*
            TODO: this code may be never be reached, as we already checked for let in statement, need to identify a
            TODO: pattern to handle these kind of duplicate code
             */
        if (currentToken != CALL) generateError(CALL_NOT_FOUND);
            moveToNextToken();
        if (currentToken == IDENTIFIER) {
            String identifier = scanner.getCurrentIdentifier();
            if (isPreDefinedFunction(identifier)) {
                moveToNextToken();
                Result paramResult = getParametersForSpecialFunctions(basicBlock);
                res = ig.generateInstructionForPreDefinedFunctions(identifier, paramResult);
                basicBlock.addInstruction(ig.getInstruction(res.getInstructionId()));
            } else {
                Function function = tracker.getFunction(identifier);
                basicBlock.addFunctionCalled(function);
                moveToNextToken();
                ArrayList<Result> parameters = new ArrayList<>();
                if (currentToken == OPENPAREN) {
                    moveToNextToken();
                    // Go from expression -> term -> factor -> designator -> identifier
                    if (currentToken != CLOSEPAREN) {
                        Result param = expression(basicBlock, function);
                        parameters.add(param);
                        while (currentToken == COMMA) {
                            moveToNextToken();
                            param = expression(basicBlock, function);
                            parameters.add(param);
                        }
                        ArrayList<Instruction> instructions = ig.generateInstructionForParams(parameters);
                        for (Instruction instruction : instructions) basicBlock.addInstruction(instruction);
                    }
                    if (currentToken == CLOSEPAREN) {
                        moveToNextToken();
                    } else generateError(CLOSE_PAREN_NOT_FOUND);
                }
                res = ig.generateInstructionForFunctionCall(parameters.size(), basicBlock.getId());
                basicBlock.addInstruction(ig.getInstruction(res.getInstructionId()));
            }
        }
        return res;
    }

    private boolean isPreDefinedFunction(String identifier) {
        return identifier.equals("InputNum") || identifier.equals("OutputNum") || identifier.equals("OutputNewLine");
    }

    private Result getParametersForSpecialFunctions(BasicBlock basicBlock) throws IOException {
        Result result = null;
        if (currentToken == OPENPAREN) {
            moveToNextToken();
            if (currentToken == IDENTIFIER) {
                String identifier = scanner.getCurrentIdentifier();
                moveToNextToken();
                result = new Result();
                result.setKind(VARIABLE);
                result.setIdentifierName(identifier);
                result.setSsaVersion(basicBlock.getSSAVersion(identifier));
            } else if (currentToken == NUMBER) {
                result = new Result();
                result.setKind(CONSTANT);
                result.setValue(number());
            }
            if (currentToken == CLOSEPAREN) moveToNextToken();
            else generateError(CLOSE_PAREN_NOT_FOUND);
        }
        return result;
    }

    public BasicBlock ifStatement(BasicBlock basicBlock, Function function) throws IOException {
        if (currentToken != IF) generateError(IF_STATEMENT_ERROR);
        moveToNextToken();

        BasicBlock ifConditionBlock = new BasicBlock(BB_IF_CONDITION);
        ifConditionBlock.addParent(basicBlock);
        basicBlock.addChildren(ifConditionBlock);

        BasicBlock ifThenBlock = new BasicBlock(BB_IF_THEN);
        ifThenBlock.addParent(ifConditionBlock);
        ifConditionBlock.addChildren(ifThenBlock);

        BasicBlock joinBlock = new BasicBlock(BB_IF_JOIN);
        joinBlock.addParent(ifThenBlock);
        ifThenBlock.addChildren(joinBlock);

        BasicBlock elseBlock = null;
        Result fixUpResult = relation(ifConditionBlock, function);
        if (currentToken != THEN) generateError(THEN_STATEMENT_ERROR);
        moveToNextToken();

        ifThenBlock = statSequence(ifThenBlock, function);

        if (currentToken == ELSE) {
            moveToNextToken();
            elseBlock = new BasicBlock(BB_ELSE);
            elseBlock.addParent(ifConditionBlock);
            elseBlock.addChildren(joinBlock);

            ifConditionBlock.addChildren(elseBlock);

            joinBlock.addParent(elseBlock);
            joinBlock.setType(BB_IF_ELSE_JOIN);

            elseBlock = statSequence(elseBlock, function);
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

    public Result relation(BasicBlock basicBlock, Function function) throws IOException {
        Result lhs = expression(basicBlock, function);
        Result condition = relOp();
        Result rhs = expression(basicBlock, function);
        RelationResult res = ig.computeRelation(condition, lhs, rhs);
        basicBlock.addInstruction(res.compareInstruction);
        basicBlock.addInstruction(res.negCompareInstruction);
        return res.fixUpResult;
    }

    public BasicBlock whileStatement(BasicBlock basicBlock, Function function) throws IOException {
        if (currentToken != WHILE) generateError(WHILE_STATEMENT_ERROR);
        moveToNextToken();

        BasicBlock whileConditionBlock = new BasicBlock(BB_WHILE);
        whileConditionBlock.addParent(basicBlock);
        basicBlock.addChildren(whileConditionBlock);

        BasicBlock whileJoinBlock = new BasicBlock(BB_WHILE_JOIN);
        whileJoinBlock.addParent(whileConditionBlock);

        // Being added only in do
        whileConditionBlock.addChildren(whileJoinBlock);

        Result fixUpResult = relation(whileConditionBlock, function);
        fixUpNegCompareInstruction(fixUpResult, whileJoinBlock);

        if (currentToken != DO) generateError(DO_EXPECTED);

        moveToNextToken();
        BasicBlock whileBodyBlock = new BasicBlock(BB_WHILE_BODY);
        whileBodyBlock.addParent(whileConditionBlock);
        whileBodyBlock.addChildren(whileConditionBlock);

        whileConditionBlock.addChildren(whileBodyBlock);

        whileBodyBlock = statSequence(whileBodyBlock, function);
        //Go Back to while condition -> adding instruction for that
        addBranchInstruction(whileBodyBlock, whileConditionBlock);
        if (currentToken != OD) generateError(OD_EXPECTED);
        moveToNextToken();
        return whileJoinBlock;
    }

    public void returnStatement(BasicBlock basicBlock, Function function) throws IOException {
        if (currentToken != RETURN) generateError(RETURN_EXPECTED);
        moveToNextToken();
        if (currentToken != END) {
            Result expResult = expression(basicBlock, function);
            Instruction instruction = ig.generateInstructionForReturn(expResult);
            basicBlock.addInstruction(instruction);
        }
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
