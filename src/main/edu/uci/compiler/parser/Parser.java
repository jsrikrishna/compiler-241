package main.edu.uci.compiler.parser;

import main.edu.uci.compiler.cfg.ControlFlowGraph;
import main.edu.uci.compiler.model.*;
import main.edu.uci.compiler.parser.InstructionGenerator.*;

import static main.edu.uci.compiler.model.Token.*;
import static main.edu.uci.compiler.model.ErrorMessage.*;
import static main.edu.uci.compiler.model.Result.KIND.*;
import static main.edu.uci.compiler.model.BasicBlock.Type.*;

import java.io.IOException;
import java.util.*;

/**
 * Created by srikrishna on 1/27/17.
 */
public class Parser {
    private String fileName;
    private Scanner scanner;
    private Token currentToken;
    private BasicBlock startBasicBlock;
    private ControlFlowGraph cfg;
    private InstructionGenerator ig;
    private LiveRangeAnalysis lra;
    private Set<DominatorBlock> allRootDominatorBlocks;
    private HashMap<BasicBlock, BasicBlock> allDomParents;
    private Set<BasicBlock> endBasicBlocks;
    private ArrayList<Function> declaredFunctions;
    private HashMap<Instruction, Result> instructionResults;
    private HashMap<Integer, Instruction> allInstructions;
    private LinkedList<Instruction> phiInstructions;
    private HashMap<Integer, Integer> registerForResults;
    private CopyPropagator cp;
    private CommonSubExpElimination cse;
    private LTDominatorTree ltDomTree;
    private DominatorTree domTree;
    private RegisterAllocator ra;
    private ArrayList<Token> relOpList;
    private ArrayList<Token> statSeqList;
    private Tracker tracker;
    private Integer DEFAULT_SSA_VERSION = -1;


    public Parser(String fileName) throws IOException {
        this.fileName = fileName;
        endBasicBlocks = new HashSet<>();
        allRootDominatorBlocks = new HashSet<>();
        instructionResults = new HashMap<>();
        allInstructions = new HashMap<>();
        phiInstructions = new LinkedList<>();
        registerForResults = new HashMap<>();
        allDomParents = new HashMap<>();
        declaredFunctions = new ArrayList<>();
        scanner = new Scanner(fileName);
        currentToken = scanner.getToken();
        ig = new InstructionGenerator(instructionResults, allInstructions);
        cfg = new ControlFlowGraph(this.endBasicBlocks);
        tracker = new Tracker();
        domTree = new DominatorTree(allRootDominatorBlocks, endBasicBlocks, allDomParents);
//        ltDomTree = new LTDominatorTree(startBasicBlock);
        cp = new CopyPropagator(allRootDominatorBlocks, ig, instructionResults);

        cse = new CommonSubExpElimination(allRootDominatorBlocks, instructionResults, allInstructions);
        lra = new LiveRangeAnalysis(endBasicBlocks, allDomParents, phiInstructions, instructionResults);
        ra = new RegisterAllocator(lra.getInterferenceGraph(), cfg, registerForResults);

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
        BasicBlock startBasicBlock = new BasicBlock(BB_MAIN);
        this.startBasicBlock = startBasicBlock;
        cfg.setStartBasicBlock(startBasicBlock);
        while (currentToken == VAR || currentToken == ARRAY) {
            //TODO: Deal with array's later
            // Need not move to next token, it is handled by varDecl
            varDecl(null, startBasicBlock);
        }
        startBasicBlock.setLocalTracker(tracker.getCopyOfVariableTracker());
        while (currentToken == Token.FUNCTION || currentToken == PROCEDURE) {
            // Need not move to next token, it is handled by funcDecl
            funcDecl();
        }

        // Update DOM Tree Class with Start Basic Block and functions called
        domTree.updateDomTree(startBasicBlock, tracker.getFunctions());

        if (currentToken != BEGIN) generateError(BEGIN_NOT_FOUND);
        moveToNextToken();
        BasicBlock endBasicBlock = statSequence(startBasicBlock, null, null);

        if (currentToken != END) generateError(END_NOT_FOUND);
        moveToNextToken();

        if (currentToken != PERIOD) generateError(PERIOD_NOT_FOUND);
        moveToNextToken();
        Instruction endInstruction = ig.generateEndInstruction();
        endInstruction.setBasicBlock(endBasicBlock);
        endBasicBlock.addInstruction(endInstruction);
        // This need not be done, have to check
        cfg.addEndBasicBlock(endBasicBlock);
        this.endBasicBlocks.add(endBasicBlock);

        domTree.generateDomRelationsForProgram();
//        ltDomTree.getDominatorTree();
//        cfg.printParentsForProgram(fileName);
    }

    public void doCopyPropagation(boolean doBranchFolding) {
        cp.propagateCopiesForProgram(doBranchFolding);
    }

    public void printCFG(boolean isCP, boolean isCSE, boolean isPhiRemoved) {
        if (isPhiRemoved) {
            cfg.writeToCFGAfterPhiRemoval(
                    fileName,
                    cfg.getStartBasicBlock(),
                    startBasicBlock.getListOfAllBasicBlocks(),
                    registerForResults,
                    declaredFunctions);
            return;
        }
        cfg.writeToCFGFile(
                fileName,
                isCP,
                isCSE,
                cfg.getStartBasicBlock(),
                startBasicBlock.getListOfAllBasicBlocks(),
                declaredFunctions);
    }

    public void printNumberOfInstructions() {
        ig.printTotalNumberOfInstructions();
    }

    public void printDomVCG() {
        domTree.printDomVCGForProgram(fileName);
    }

    public void doCommonSubExpressionElimination() {
        cse.doCSEForProgram();
    }

    public void doLiveRangeAnalysis() {
        lra.generateInterferenceGraphForProgram();
        List<String> adjListStrictGraph = lra.getInterferenceGraph().writeAdjList();
        cfg.generateFlow(fileName, adjListStrictGraph, "lra");
    }

    public void doRegisterAllocation(String fileName) {
        ra.allocateRegister(fileName);
        ra.mapToRegisters();
        removePhiInstructions();
    }

    private void varDecl(Function function, BasicBlock basicBlock) throws IOException {
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

    private void storeVariables(ArrayList<Integer> arrayDimensions, Function function, BasicBlock basicBlock) {
        String identifier = scanner.getCurrentIdentifier();
        if (function != null) {
            if (arrayDimensions != null) {
                function.addLocalArrayVariable(identifier, arrayDimensions);
            } else {
                Instruction instruction = ig.generateInstructionToInitVar(identifier);
                instruction.getOperand2().setSsaVersion(instruction.getInstructionId());
                function.updateSSAVariable(identifier, instruction.getInstructionId());
                // Here for sure, code will have the instruction id, so updating in basic block copy as well
                basicBlock.updateSSAVersion(identifier, instruction.getInstructionId());
                // May be normal block as well,
                // need to check -> checked, need not do, it will be either func or non-func
                instruction.setBasicBlock(basicBlock);
                basicBlock.addInstruction(instruction);
            }
            return;
        }
        if (arrayDimensions != null) {
            tracker.addArrayVariable(identifier, arrayDimensions);
        } else {
            Instruction instruction = ig.generateInstructionToInitVar(identifier);
            tracker.updateSSAForVariable(identifier, instruction.getInstructionId());
            instruction.getOperand2().setSsaVersion(tracker.getSSAVersion(identifier));
            instruction.setBasicBlock(basicBlock);
            basicBlock.addInstruction(instruction);
        }
    }

    private ArrayList<Integer> typeDecl() throws IOException {
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

    private int number() throws IOException {
        if (currentToken != NUMBER) generateError(NUMBER_EXPECTED);
        moveToNextToken();
        return scanner.getCurrentNumber();
    }

    private void funcDecl() throws IOException {
        if (currentToken != Token.FUNCTION && currentToken != PROCEDURE) generateError(FUNCTION_PROCEDURE_NOT_FOUND);
        moveToNextToken();
        if (currentToken != IDENTIFIER) generateError(IDENTIFIER_NOT_FOUND);
        String identifier = scanner.getCurrentIdentifier();
        Function function = new Function(identifier);
        this.declaredFunctions.add(function);
        tracker.addFunction(identifier, function);
        moveToNextToken();
        formalParam(function); // formalParam, handles of moving to next token
        if (currentToken != SEMICOLON) generateError(SEMICOLON_NOT_FOUND);
        moveToNextToken();
        funcBody(function);
        if (currentToken != SEMICOLON) generateError(SEMICOLON_NOT_FOUND);
        moveToNextToken();
    }

    private void formalParam(Function function) throws IOException {
        if (currentToken != OPENPAREN) return;
        moveToNextToken();
        if (currentToken == IDENTIFIER) {
            addFunctionParameters(function);
            moveToNextToken();
            while (currentToken == COMMA) {
                moveToNextToken();
                if (currentToken != IDENTIFIER) generateError(FORMAL_PARAM_DECL_ERROR);
                addFunctionParameters(function);
                moveToNextToken();
            }
        }

        if (currentToken != CLOSEPAREN) generateError(FORMAL_PARAM_DECL_ERROR);
        moveToNextToken();
    }

    private void addFunctionParameters(Function function) {
        String identifier = scanner.getCurrentIdentifier();
        Integer ssaVersion = function.getSSAForVariable(identifier);
//        I shouldn't be doing the following IF and getting from global tracker i guess, commenting it out
//        if (ssaVersion == null) {
//            ssaVersion = tracker.getSSAVersion(identifier);
//        }
//        SSA Version might be NULL, then it will be initialized of -1
        if (ssaVersion == null) {
            ssaVersion = DEFAULT_SSA_VERSION;
            tracker.updateSSAForVariable(identifier, ssaVersion);
            function.getFuncBasicBlock().updateSSAVersion(identifier, ssaVersion);
        }
        Result paramResult = new Result();
        paramResult.setKind(PARAMETER);
        paramResult.setIdentifierName(identifier);
        function.updateSSAVariable(identifier, ssaVersion);
        function.setFuncParameter(paramResult);
    }

    private void funcBody(Function function) throws IOException {
        while (currentToken == VAR || currentToken == ARRAY) varDecl(function, function.getFuncBasicBlock());
        if (currentToken != BEGIN) generateError(BEGIN_NOT_FOUND);
        moveToNextToken();
        BasicBlock finalBlock = statSequence(function.getFuncBasicBlock(), function, null);
        if (currentToken != END) generateError(END_NOT_FOUND);
        moveToNextToken();
    }

    private BasicBlock statSequence(BasicBlock basicBlock,
                                    Function function,
                                    List<Instruction> toBeKilled) throws IOException {
        basicBlock = statement(basicBlock, function, toBeKilled);
        while (currentToken == SEMICOLON) {
            moveToNextToken();
            basicBlock = statement(basicBlock, function, toBeKilled);
        }
        return basicBlock;
    }

    private BasicBlock statement(BasicBlock basicBlock,
                                 Function function,
                                 List<Instruction> toBeKilled) throws IOException {

        if (!(statSeqList.contains(currentToken))) generateError(KEYWORD_EXPECTED);

        if (currentToken == IF) {
            return ifStatement(basicBlock, function, toBeKilled);
        }
        if (currentToken == WHILE) {
            return whileStatement(basicBlock, function, toBeKilled);
        }
        if (currentToken == LET) {
            assignment(basicBlock, function, toBeKilled);
        } else if (currentToken == CALL) {
            funcCall(basicBlock);
        } else if (currentToken == RETURN) {
            returnStatement(basicBlock, function);
        }
        return basicBlock;

    }

    private Result assignment(BasicBlock basicBlock,
                              Function function,
                              List<Instruction> toBeKilled) throws IOException {
        if (currentToken != LET) generateError(ASSIGNMENT_ERROR);
        moveToNextToken();
        Result lhs = designator(basicBlock, function, false);
        if (currentToken != BECOMES) generateError(BECOMES_NOT_FOUND);
        moveToNextToken();
        Result rhs = expression(basicBlock, function);
        Result assignmentResult = ig.generateInstructionForAssignment(lhs, rhs);
        Instruction instruction = allInstructions.get(assignmentResult.getInstructionId());
        instruction.setBasicBlock(basicBlock);
        basicBlock.addInstruction(instruction);
        // If Assignment is a store instruction,
        // then it will be an array, so insert kill instruction for the array variable represented by lhs
        if (instruction.getOperation() == Operation.STORE) {
            Instruction killInstruction = ig.generateKillInstruction(instruction.getArrayVariable());
            killInstruction.setBasicBlock(basicBlock);
            basicBlock.addInstruction(killInstruction);
            if (toBeKilled != null) toBeKilled.add(killInstruction);
        }
        /*
        Update lhs result with SSA Version
        Update Tracker for local result, both basic block level and global level
         */
        if (lhs.getKind() == VARIABLE) {
            isVariableDeclared(function, lhs);
            tracker.updateSSAForVariable(lhs.getIdentifierName(), instruction.getInstructionId());
            lhs.setSsaVersion(instruction.getInstructionId());
            // Keep a Copy in Basic Block also, so that it can be used while generating Phi Functions
            basicBlock.updateSSAVersion(lhs.getIdentifierName(), instruction.getInstructionId());
            if (function != null) {
                function.updateSSAVariable(lhs.getIdentifierName(), instruction.getInstructionId());
            }
        }
        if (lhs.getKind() == INSTRUCTION) {
            Result arrayVariable = new Result();
            arrayVariable.setKind(ARRAY_VARIABLE);
            arrayVariable.setIdentifierName(lhs.getIdentifierName());
            instruction.setArrayVariable(arrayVariable);
        }

        /*
        Update rhs result with SSA Version
         */
        if (rhs.getKind() == VARIABLE) {

            isVariableDeclared(function, rhs);

            String identifier = rhs.getIdentifierName();
            // I may get Basic Block correct all the time,
            // so i can take the ssa from Basic Block all the time instead of checking for func == null
            if (function != null) {
                Integer funcSSAVersion = function.getFuncBasicBlock().getSSAVersion(identifier);
                if (funcSSAVersion == null) funcSSAVersion = tracker.getSSAVersion(identifier);
                rhs.setSsaVersion(funcSSAVersion);
            } else {
                Integer basicBlockSSAVersion = basicBlock.getSSAVersion(identifier);
                if (basicBlockSSAVersion == null) basicBlockSSAVersion = tracker.getSSAVersion(identifier);
                rhs.setSsaVersion(basicBlockSSAVersion);
            }
        }
        return lhs;
    }

    private void isVariableDeclared(Function function, Result result) {
        String identifier = result.getIdentifierName();
        if (function != null) {
            // Function Parameters have a defaul ssa version of -1
            String funcName = function.getFuncName();
            boolean isNotAFuncParameter = !containFuncParameter(function, result.getIdentifierName());
            boolean isNotAFuncLocalVariable = !function.isLocalVariable(result.getIdentifierName());
            boolean isNotAGlobalVariable = result.getSsaVersion() == null;

            if (isNotAFuncParameter && isNotAFuncLocalVariable && isNotAGlobalVariable) {
                System.err.println("Variable '" + identifier + "' in function '" + funcName + "' must declared");
                System.exit(103);
            }

        } else {
            if (result.getSsaVersion() == null || result.getSsaVersion() == -1) {
                System.err.println("Variable " + identifier + " must declared before being used");
                System.exit(103);
            }
        }
    }

    private boolean containFuncParameter(Function function, String identifierName) {

        Result paramResult = new Result();
        paramResult.setKind(PARAMETER);
        paramResult.setIdentifierName(identifierName);

        for (Result result : function.getFuncParameters()) {
            if (result.equals(paramResult)) {
                return true;
            }
        }
        return false;
    }

    private Result designator(BasicBlock basicBlock, Function function, boolean isRHS) throws IOException {
        if (currentToken != IDENTIFIER) generateError(DESIGNATOR_ERROR);
        // Could be array variable or a normal variable
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
            Result indexInArray = handleArrayDesignator(arrayDimensionResult, identifier, basicBlock, isRHS);
            return indexInArray;
        }


        // ToDo: Q - Need to check, may be null as well,
        // for assignment - its a different process, this will be used for terms or factors
        // Need to add ssa-version to result from the instruction number or figure out
        Result res = new Result();
        res.setKind(VARIABLE);
        res.setIdentifierName(identifier);
        if (function != null) {
            //Need to perform a check here, compared with old code
            Integer ssaVersion = function.getFuncBasicBlock().getSSAVersion(identifier);
            if (ssaVersion == null) {
                ssaVersion = tracker.getSSAVersion(identifier);
                if (ssaVersion == null) generateError(VARIABLE_NOT_DECLARED);
            }
            res.setSsaVersion(ssaVersion);
        } else {
            res.setSsaVersion(basicBlock.getSSAVersion(identifier));
        }


        moveToNextToken();
        return res;
    }

    private Result handleArrayDimInstructions(ArrayList<Result> dimExp,
                                              ArrayList<Integer> dims,
                                              BasicBlock basicBlock) {
        ArrayBase res = ig.generateInstructionsForArrDim(dimExp, dims);
        for (Integer instrId : res.instructionIds) {
            Instruction arrDimInstruction = ig.getInstruction(instrId);
            arrDimInstruction.setBasicBlock(basicBlock);
            basicBlock.addInstruction(arrDimInstruction);
        }
        return res.finalResult;
    }

    private Result handleArrayDesignator(Result dimResult, String identifier, BasicBlock basicBlock, boolean isRHS) {
        ArrayBase res = ig.computeArrayDesignator(dimResult, identifier, isRHS);
        for (Integer instrId : res.instructionIds) {
            Instruction arrDesigInstruction = ig.getInstruction(instrId);
            arrDesigInstruction.setBasicBlock(basicBlock);
            basicBlock.addInstruction(arrDesigInstruction);
        }
        return res.finalResult;
    }

    private Result expression(BasicBlock basicBlock, Function function) throws IOException {
        Result lhs = term(basicBlock, function);
        while (currentToken == PLUS || currentToken == MINUS) {
            Token prevToken = currentToken;
            moveToNextToken();
            Result rhs = term(basicBlock, function);
            if (isVariableNotDeclared(lhs)) {
                System.err.println("Variable " + lhs.getIdentifierName() + " must declared before being used");
                System.exit(103);
            }
            if (isVariableNotDeclared(rhs)) {
                System.err.println("Variable " + rhs.getIdentifierName() + " must declared before being used");
                System.exit(103);
            }
            lhs = ig.computeExpression(prevToken, lhs, rhs);
            if (lhs.getKind() == INSTRUCTION) {
                addInstructionUtil(basicBlock, lhs);
            }
        }
        return lhs;
    }

    private void addInstructionUtil(BasicBlock basicBlock, Result result) {
        Instruction instruction = ig.getInstruction(result.getInstructionId());
        instruction.setBasicBlock(basicBlock);
        basicBlock.addInstruction(instruction);
        result.setBasicBlockId(basicBlock.getId());
    }

    private Result term(BasicBlock basicBlock, Function function) throws IOException {
        Result lhs = factor(basicBlock, function);
        while (currentToken == TIMES || currentToken == DIV) {
            Token prevToken = currentToken;
            moveToNextToken();
            Result rhs = factor(basicBlock, function);
            if (isVariableNotDeclared(lhs)) {
                System.err.println("Variable " + lhs.getIdentifierName() + " must declared before being used");
                System.exit(103);
            }
            if (isVariableNotDeclared(rhs)) {
                System.err.println("Variable " + rhs.getIdentifierName() + " must declared before being used");
                System.exit(103);
            }
            lhs = ig.computeExpression(prevToken, lhs, rhs);
            if (lhs.getKind() == INSTRUCTION) {
                addInstructionUtil(basicBlock, lhs);
            }
        }
        return lhs;
    }

    private Result factor(BasicBlock basicBlock, Function function) throws IOException {
        if (!isAFactor(currentToken)) generateError(FACTOR_ERROR);
        if (currentToken == IDENTIFIER) {
            return designator(basicBlock, function, true);
        }
        if (currentToken == NUMBER) {
            Result result = new Result();
            result.setKind(CONSTANT);
            result.setValue(number());
            return result;
        }
        if (currentToken == CALL) {
            return funcCall(basicBlock);
        }
        if (currentToken == OPENPAREN) {
            moveToNextToken();
            Result result = expression(basicBlock, function);
            if (currentToken != CLOSEPAREN) generateError(CLOSE_PAREN_NOT_FOUND);
            moveToNextToken();
            return result;
        }
        return null;
    }

    private boolean isAFactor(Token token) {
        return token == IDENTIFIER || token == NUMBER || token == CALL || token == OPENPAREN;
    }

    private Result funcCall(BasicBlock basicBlock) throws IOException {
        Result res = null;
        if (currentToken != CALL) generateError(CALL_NOT_FOUND);
        moveToNextToken();
        if (currentToken == IDENTIFIER) {
            String identifier = scanner.getCurrentIdentifier();
            if (isPreDefinedFunction(identifier)) {
                moveToNextToken();
                Result paramResult = getParametersForSpecialFunctions(basicBlock);
                res = ig.generateInstructionForPreDefinedFunctions(identifier, paramResult);
                Instruction preDefinedInstruction = ig.getInstruction(res.getInstructionId());
                preDefinedInstruction.setBasicBlock(basicBlock);
                basicBlock.addInstruction(preDefinedInstruction);
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
//                        ArrayList<Instruction> instructions = ig.generateInstructionForParams(parameters);
//                        for (Instruction instruction : instructions) {
//                            instruction.setBasicBlock(basicBlock);
//                            basicBlock.addInstruction(instruction);
//                        }
                    }
                    if (currentToken != CLOSEPAREN) generateError(CLOSE_PAREN_NOT_FOUND);
                    moveToNextToken();
                }
                if (parameters.size() != function.getFuncParameters().size()) {
                    Integer expected = function.getFuncParameters().size();
                    Integer got = parameters.size();
                    String funcName = function.getFuncName();
                    System.err.println("Function parameters does not match for - '" + function.getFuncName() + "'");
                    System.err.println("Expected " + expected + " parameters for " + funcName + " but got " + got);
                    System.exit(103);
                }
                res = ig.generateInstructionForFunctionCall(parameters.size(), function.getFuncBasicBlock().getId());
                Instruction funcCallInstruction = ig.getInstruction(res.getInstructionId());
                funcCallInstruction.setBasicBlock(basicBlock);
                basicBlock.addInstruction(funcCallInstruction);
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
                //TODO: Need to understand the need of getting SSA from basic block
//                result.setSsaVersion(tracker.getSSAVersion(identifier));
            } else if (currentToken == NUMBER) {
                result = new Result();
                result.setKind(CONSTANT);
                result.setValue(number());
            }
            if (currentToken != CLOSEPAREN) generateError(CLOSE_PAREN_NOT_FOUND);
            moveToNextToken();
        }
        return result;
    }

    private BasicBlock ifStatement(BasicBlock basicBlock,
                                   Function function,
                                   List<Instruction> toBeKilled) throws IOException {
        if (currentToken != IF) generateError(IF_STATEMENT_ERROR);
        moveToNextToken();

        BasicBlock ifConditionBlock = new BasicBlock(BB_IF_CONDITION);
        ifConditionBlock.addParent(basicBlock);
        basicBlock.addChildrenAndUpdateChildrenTracker(ifConditionBlock);

        BasicBlock ifThenBlock = new BasicBlock(BB_IF_THEN);
        ifThenBlock.addParent(ifConditionBlock);
        ifConditionBlock.addChildrenAndUpdateChildrenTracker(ifThenBlock);

        BasicBlock elseBlock = null;
        Result fixUpResult = relation(ifConditionBlock, function);
        List<Instruction> toBeKilledInIfThen = new LinkedList<>();
        List<Instruction> toBeKilledInElse = new LinkedList<>();
        if (currentToken != THEN) generateError(THEN_STATEMENT_ERROR);
        moveToNextToken();
        ifThenBlock = statSequence(ifThenBlock, function, toBeKilledInIfThen);

        BasicBlock joinBlock = new BasicBlock(BB_IF_THEN_JOIN);
        ifThenBlock.addChildrenAndUpdateChildrenTracker(joinBlock);
        joinBlock.addParent(ifThenBlock);
        joinBlock.setLeftParent(ifThenBlock);
        // BRA instruction from IF Block to JOIN Block
        addBranchInstruction(ifThenBlock, joinBlock);

        if (currentToken == ELSE) {
            moveToNextToken();
            elseBlock = new BasicBlock(BB_ELSE);
            // NEGATED Jump to ELSE BLOCK from IF_CONDITION BLOCK if not TRUE
            fixUpNegCompareInstruction(fixUpResult, elseBlock);

            ifConditionBlock.addChildrenAndUpdateChildrenTracker(elseBlock);
            elseBlock.addParent(ifConditionBlock);

            joinBlock.setType(BB_IF_ELSE_JOIN);

            elseBlock = statSequence(elseBlock, function, toBeKilledInElse);
            // Need to add here, as parent relationships are used in Live Range Analysis
            joinBlock.addParent(elseBlock);
            joinBlock.setRightParent(elseBlock);
            // Need to add here, because, last elseBlock returned should link to if-else-join
            elseBlock.addChildrenAndUpdateChildrenTracker(joinBlock);
            // BRA instruction from ELSE Block to JOIN Block
//            addBranchInstruction(elseBlock, joinBlock);
            insertKillInstructionsForIfStatement(joinBlock, toBeKilledInIfThen, toBeKilledInElse);
            insertPhiFunctionForIfStatement(ifThenBlock, elseBlock, joinBlock);
        }

        if (currentToken != FI) generateError(FI_STATEMENT_ERROR);
        moveToNextToken();

        if (elseBlock == null) {
            // NEGATED Jump to JOIN BLOCK from IF_CONDITION block, if condition is NOT TRUE
            ifConditionBlock.addChildrenAndUpdateChildrenTracker(joinBlock);
            joinBlock.addParent(ifConditionBlock);
            joinBlock.setRightParent(ifConditionBlock);
            fixUpNegCompareInstruction(fixUpResult, joinBlock);
            insertKillInstructionsForIfStatement(joinBlock, toBeKilledInIfThen, toBeKilledInElse);
            insertPhiFunctionForIfStatement(ifThenBlock, ifConditionBlock, joinBlock);
        }

        if (toBeKilled != null) {
            toBeKilled.addAll(toBeKilledInIfThen);
            toBeKilled.addAll(toBeKilledInElse);
        }

        return joinBlock;
    }

    private Result relation(BasicBlock basicBlock, Function function) throws IOException {
        Result lhs = expression(basicBlock, function);
        Result condition = relOp();
        Result rhs = expression(basicBlock, function);
        RelationResult res = ig.computeRelation(condition, lhs, rhs);
        setSSAForVariableResult(basicBlock, lhs, function);
        setSSAForVariableResult(basicBlock, rhs, function);
        basicBlock.addInstruction(res.compareInstruction);
        basicBlock.addInstruction(res.negCompareInstruction);
        res.compareInstruction.setBasicBlock(basicBlock);
        res.negCompareInstruction.setBasicBlock(basicBlock);
        return res.fixUpResult;
    }

    private void setSSAForVariableResult(BasicBlock basicBlock, Result res, Function function) {
        if (res.getKind() == VARIABLE) {
            if (function != null) {
                Integer ssaVersion = function.getSSAForVariable(res.getIdentifierName());
                if (ssaVersion == null) {
                    ssaVersion = tracker.getSSAVersion(res.getIdentifierName());
                    if (ssaVersion == null) generateError(VARIABLE_NOT_DECLARED);
                }
                res.setSsaVersion(ssaVersion);
            } else {
                Integer ssaVersion = basicBlock.getSSAVersion(res.getIdentifierName());
                if (ssaVersion == null) {
                    ssaVersion = tracker.getSSAVersion(res.getIdentifierName());
                    if (ssaVersion == null) generateError(VARIABLE_NOT_DECLARED);
                }
                res.setSsaVersion(ssaVersion);
            }
        }
    }

    private void insertKillInstructionsForIfStatement(BasicBlock basicBlock,
                                                      List<Instruction> leftKill,
                                                      List<Instruction> rightKill) {
        insertKillInstructions(basicBlock, leftKill);
        insertKillInstructions(basicBlock, rightKill);
    }

    private void insertKillInstructions(BasicBlock basicBlock, List<Instruction> killInstructions) {
        if (killInstructions != null) {
            for (Instruction kill : killInstructions) {
                Instruction newKill = ig.generateKillInstruction(kill.getArrayVariable());
                basicBlock.addInstruction(newKill);
                newKill.setBasicBlock(basicBlock);
            }
        }
    }

    private BasicBlock whileStatement(BasicBlock basicBlock,
                                      Function function,
                                      List<Instruction> toBekilled) throws IOException {
        if (currentToken != WHILE) generateError(WHILE_STATEMENT_ERROR);
        moveToNextToken();

        BasicBlock whileConditionJoinBlock = new BasicBlock(BB_WHILE_CONDITION_AND_JOIN);
        basicBlock.addChildrenAndUpdateChildrenTracker(whileConditionJoinBlock);
        whileConditionJoinBlock.addParent(basicBlock);

        BasicBlock whileFallThrough = new BasicBlock(BB_WHILE_FALL_THROUGH);
        whileConditionJoinBlock.addChildrenAndUpdateChildrenTracker(whileFallThrough);
        whileFallThrough.addParent(whileConditionJoinBlock);


        // Being added only in do
        Result fixUpResult = relation(whileConditionJoinBlock, function);
        fixUpNegCompareInstruction(fixUpResult, whileFallThrough);

        if (currentToken != DO) generateError(DO_EXPECTED);

        moveToNextToken();
        BasicBlock whileBodyBlock = new BasicBlock(BB_WHILE_BODY);
        whileConditionJoinBlock.addChildrenAndUpdateChildrenTracker(whileBodyBlock);
        whileBodyBlock.addParent(whileConditionJoinBlock);


        // Should this after whileCondition.addChildren(whileBody) because, SSA needs to be present in parent
        whileBodyBlock.addChildrenAndUpdateChildrenTracker(whileConditionJoinBlock);


        BasicBlock whileBodyEndBlock = statSequence(whileBodyBlock, function, toBekilled);
        //Go Back to while condition -> adding instruction for that
        if (whileBodyBlock != whileBodyEndBlock) {
            whileBodyEndBlock.addChildrenAndUpdateChildrenTracker(whileConditionJoinBlock);
            whileConditionJoinBlock.addParent(whileBodyEndBlock);
            whileBodyBlock.removeChildren(whileConditionJoinBlock);
        } else {
            whileConditionJoinBlock.addParent(whileBodyBlock);
        }


        addBranchInstruction(whileBodyBlock, whileConditionJoinBlock);

        insertPhiFunctionForWhileStatement(basicBlock,
                whileConditionJoinBlock,
                whileBodyEndBlock,
                whileBodyBlock,
                whileFallThrough);

        if (currentToken != OD) generateError(OD_EXPECTED);
        moveToNextToken();
        return whileFallThrough;
    }

    private void returnStatement(BasicBlock basicBlock, Function function) throws IOException {
        if (currentToken != RETURN) generateError(RETURN_EXPECTED);
        moveToNextToken();
        if (currentToken != END) {
            Result expResult = expression(basicBlock, function);
            Result endResult = ig.generateInstructionForReturn(expResult);
            Instruction endInstruction = allInstructions.get(endResult.getInstructionId());
            endInstruction.setBasicBlock(basicBlock);
            basicBlock.addInstruction(endInstruction);
        }
    }

    private Result relOp() throws IOException {
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

    private void addBranchInstruction(BasicBlock fromBlock, BasicBlock toBlock) {
        Instruction branchInstruction = ig.generateBranchInstruction();
        branchInstruction.getOperand1().setBasicBlockId(toBlock.getId());
        branchInstruction.setBasicBlock(fromBlock);
        fromBlock.addInstruction(branchInstruction);
    }

    private void insertPhiFunctionForIfStatement(BasicBlock leftBlock,
                                                 BasicBlock rightBlock,
                                                 BasicBlock joinBlock) {
        HashMap<String, Integer> leftTracker = leftBlock.getLocalTracker();
        HashMap<String, Integer> rightTracker = rightBlock.getLocalTracker();
        HashMap<String, Integer> joinTracker = new HashMap<>();

        for (Map.Entry<String, Integer> trackEntry : leftTracker.entrySet()) {
            String identifier = trackEntry.getKey();
            Integer leftSSAVersion = trackEntry.getValue();
            if (rightTracker.containsKey(identifier) && !leftSSAVersion.equals(rightTracker.get(identifier))) {
                Integer rightSSAVersion = rightTracker.get(identifier);

                Result leftResult = ig.resultForVariable(identifier, leftSSAVersion);
                Result rightResult = ig.resultForVariable(identifier, rightSSAVersion);

                Result phiInstructionResult = ig.generatePhiInstruction(leftResult, rightResult);
                Instruction phiInstruction = allInstructions.get(phiInstructionResult.getInstructionId());

                phiInstruction.setBasicBlock(joinBlock);
                joinBlock.addInstruction(phiInstruction);
                // Same as phiInstruction.getInstructionId() instead of phiInstruction.getOperand3().getSsaVersion()

                joinTracker.put(identifier, phiInstruction.getOperand3().getSsaVersion());
                tracker.updateSSAForVariable(identifier, phiInstruction.getInstructionId());
            } else joinTracker.put(identifier, leftSSAVersion);
        }
        putRemainingTrackerEntries(leftTracker, rightTracker, joinTracker);
        joinBlock.setLocalTracker(joinTracker);
    }

    private void insertPhiFunctionForWhileStatement(BasicBlock parentBasicBlock,
                                                    BasicBlock whileConditionBlock,
                                                    BasicBlock whileBodyEndBlock,
                                                    BasicBlock whileBodyBlock,
                                                    BasicBlock whileJoinBlock) {

        HashMap<String, Integer> parentTracker = parentBasicBlock.getLocalTracker();
        HashMap<String, Integer> whileBodyTracker = whileBodyEndBlock.getLocalTracker();
        HashMap<String, Integer> whileJoinTracker = new HashMap<>();
        ArrayList<Instruction> phiInstructionList = new ArrayList<>();

        for (Map.Entry<String, Integer> whileBodyEntry : whileBodyTracker.entrySet()) {
            String identifier = whileBodyEntry.getKey();
            Integer whileBodySSAVersion = whileBodyEntry.getValue();
            if (parentTracker.containsKey(identifier)
                    && !whileBodySSAVersion.equals(parentTracker.get(identifier))) {

                Integer parentSSAVersion = parentTracker.get(identifier);

                Result whileBodyResult = ig.resultForVariable(identifier, whileBodySSAVersion);
                Result parentResult = ig.resultForVariable(identifier, parentSSAVersion);

                // First Operand of Phi Instruction should be parent always for while phi instructions
                Result phiInstructionResult = ig.generatePhiInstruction(parentResult, whileBodyResult);
                Instruction phiInstruction = allInstructions.get(phiInstructionResult.getInstructionId());
                phiInstructionList.add(phiInstruction);
                Integer phiInstructionId = phiInstruction.getInstructionId();

                tracker.updateSSAForVariable(identifier, phiInstructionId);
                whileConditionBlock.updateSSAVersion(identifier, phiInstructionId);
                whileBodyEndBlock.updateSSAVersion(identifier, phiInstructionId);

                whileJoinTracker.put(identifier, phiInstructionId);
            } else whileJoinTracker.put(identifier, whileBodySSAVersion);
        }
        putRemainingTrackerEntries(whileBodyTracker, parentTracker, whileJoinTracker);

        // Update all the Phi Variables in WHILE_CONDITION_BLOCK to use PHI_RESULT
        updatePhiVariablesInWhileConditionBlock(whileConditionBlock, phiInstructionList);

        // Update all the Phi Variables in WHILE_BODY_BLOCK and
        // its children and children's children ... continues [BFS]
        updatePhiVariablesInWhileBodyBlock(whileBodyBlock, phiInstructionList);

        // Insert phi instructions into WHILE_CONDITION_BLOCK at the START
        for (Instruction phiInstruction : phiInstructionList) {
            phiInstruction.setBasicBlock(whileConditionBlock);
            whileConditionBlock.addInstructionAtStart(phiInstruction);
        }

        // Update SSA Tracker of whileJoinBlock
        whileJoinBlock.setLocalTracker(whileJoinTracker);

    }

    private void putRemainingTrackerEntries(HashMap<String, Integer> leftTracker,
                                            HashMap<String, Integer> rightTracker,
                                            HashMap<String, Integer> joinTracker) {

        for (Map.Entry<String, Integer> rightEntry : rightTracker.entrySet()) {
            String identifier = rightEntry.getKey();
            Integer rightSSAVersion = rightEntry.getValue();
            if (leftTracker.containsKey(identifier)) continue;
            joinTracker.put(identifier, rightSSAVersion);
        }
    }

    private void updatePhiVariablesInWhileConditionBlock(BasicBlock whileConditionBlock,
                                                         ArrayList<Instruction> phiInstructions) {

        for (Instruction phiInstruction : phiInstructions) {
            String identifier = phiInstruction.getOperand3().getIdentifierName();
            updateInstructionsWithPhiVariables(whileConditionBlock, phiInstruction, identifier);
        }
    }

    private void updatePhiVariablesInWhileBodyBlock(BasicBlock whileBodyBlock,
                                                    ArrayList<Instruction> phiInstructions) {
        ArrayList<BasicBlock> childrenBasicBlocks = getChildrenOfWhileBodyBlock(whileBodyBlock);
        for (Instruction phiInstruction : phiInstructions) {
            String identifier = phiInstruction.getOperand3().getIdentifierName();
            for (BasicBlock basicBlock : childrenBasicBlocks) {
                updateInstructionsWithPhiVariables(basicBlock, phiInstruction, identifier);
            }
        }
    }

    private ArrayList<BasicBlock> getChildrenOfWhileBodyBlock(BasicBlock whileBodyBlock) {

        HashSet<BasicBlock> visitedBlocks = new HashSet<>();

        ArrayList<BasicBlock> childrenBasicBlocks = new ArrayList<>();
        Queue<BasicBlock> frontier = new LinkedList<>();
        frontier.add(whileBodyBlock);

        while (!frontier.isEmpty()) {

            BasicBlock currentBasicBlock = frontier.poll();
            visitedBlocks.add(currentBasicBlock);
            childrenBasicBlocks.add(currentBasicBlock);

            for (BasicBlock children : currentBasicBlock.getChildren()) {
                if (!visitedBlocks.contains(children)) frontier.add(children);
            }
        }
        return childrenBasicBlocks;

    }

    private void updateInstructionsWithPhiVariables(BasicBlock basicBlock,
                                                    Instruction phiInstruction,
                                                    String phiIdentifier) {
        Integer phiInstructionId = phiInstruction.getInstructionId();

        Result phiInstructionOperand1 = phiInstruction.getOperand1();

        Integer leftResSsaVersion = phiInstructionOperand1.getSsaVersion();
        for (Instruction instruction : basicBlock.getInstructions()) {
            Result operand1 = instruction.getOperand1();
            Result operand2 = instruction.getOperand2();
            if (checkIsSameOperandWithSsaVersion(operand1, phiIdentifier, leftResSsaVersion)) {
                operand1.setSsaVersion(phiInstructionId);
            }

            if (checkIsSameOperandWithSsaVersion(operand2, phiIdentifier, leftResSsaVersion)) {
                operand2.setSsaVersion(phiInstructionId);
            }
        }
    }

    private boolean checkIsSameOperandWithSsaVersion(Result operand,
                                                     String phiIdentifier,
                                                     Integer leftResSsaVersion) {
        // Need to update only when parentSSAVersion is used like a_1
        if (isSameIdentifier(operand, phiIdentifier)) {
            Integer operandSsa = operand.getSsaVersion();
            return operandSsa.equals(leftResSsaVersion);
        }
        return false;
    }

    private boolean isSameIdentifier(Result operand, String phiIdentifier) {
        return operand != null
                && operand.getKind() == VARIABLE
                && operand.getIdentifierName().equals(phiIdentifier);
    }

    private void removePhiInstructions() {
        for (Instruction phi : phiInstructions) {
            BasicBlock phiBasicBlock = phi.getBasicBlock();
            Result phiOperand = phi.getOperand3();
            Result leftOperand = phi.getOperand1();
            Result rightOperand = phi.getOperand2();
            if (leftOperand.equals(rightOperand)) {
                phiBasicBlock.removeInstruction(phi);
                continue;
            }
            Integer phiRegisterNumber = registerForResults.get(phi.getInstructionId());
            BasicBlock leftParent = null, rightParent = null;
            if (phiBasicBlock.getType() == BB_IF_ELSE_JOIN || phiBasicBlock.getType() == BB_IF_THEN_JOIN) {
                leftParent = phiBasicBlock.getLeftParent();
                rightParent = phiBasicBlock.getRightParent();
            }
            if (phiBasicBlock.getType() == BB_WHILE_CONDITION_AND_JOIN) {
                leftParent = allDomParents.get(phiBasicBlock);
                List<BasicBlock> parentBlocks = phiBasicBlock.getParents();
                rightParent = lra.getWhileBody(parentBlocks, leftParent);
            }
            if (leftOperand != null) {
                checkAndAddMoveInstruction(leftOperand, phiOperand, phiRegisterNumber, phiBasicBlock, leftParent);
            }
            if (rightOperand != null) {
                checkAndAddMoveInstruction(rightOperand, phiOperand, phiRegisterNumber, phiBasicBlock, rightParent);
            }
            phiBasicBlock.removeInstruction(phi);
        }
    }

    private void checkAndAddMoveInstruction(Result operand,
                                            Result phiOperand,
                                            Integer phiRegisterNumber,
                                            BasicBlock phiBasicBlock,
                                            BasicBlock parent) {
        if (operand == null) return;
        if (operand.getKind() == CONSTANT) {
            addMoveInstruction(phiBasicBlock, parent, phiOperand, operand);
            return;
        }
        if (!(phiRegisterNumber.intValue() == operand.getRegisterNumber().intValue())) {
            addMoveInstruction(phiBasicBlock, parent, phiOperand, operand);
        }
        return;
    }

    private void addMoveInstruction(BasicBlock phiBasicBlock,
                                    BasicBlock parentBasicBlock,
                                    Result phiOperand,
                                    Result operand) {
        Result moveResult = ig.generateMoveInstructionForPhi(operand, phiOperand);
        Instruction moveInstruction = ig.getInstruction(moveResult.getInstructionId());
        moveInstruction.setBasicBlock(phiBasicBlock);
        BasicBlock.Type basicBlockType = parentBasicBlock.getType();
        if (basicBlockType == BB_MAIN) {
            parentBasicBlock.addInstruction(moveInstruction);
        } else {
            parentBasicBlock.addInstructionAtLastButOne(moveInstruction);
        }

    }

    private boolean isVariableNotDeclared(Result result) {
        return result.getKind() == VARIABLE && (result.getSsaVersion() == null || result.getSsaVersion() == -1);
    }

    private void generateError(ErrorMessage message) {
        System.out.println("Syntax Error occurred - " + message.toString());
        System.exit(1);
    }
}
