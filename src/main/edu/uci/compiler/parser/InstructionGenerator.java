package main.edu.uci.compiler.parser;

import main.edu.uci.compiler.model.*;

import static main.edu.uci.compiler.model.Operation.END;
import static main.edu.uci.compiler.model.Result.KIND.FUNCTION;
import static main.edu.uci.compiler.model.Token.*;
import static main.edu.uci.compiler.model.Operation.*;
import static main.edu.uci.compiler.model.Result.KIND.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by srikrishna on 2/2/17.
 */
public class InstructionGenerator {
    private static HashMap<Integer, Instruction> instructions;
    private HashMap<Token, Operation> operations;
    private HashMap<Instruction, Result> instructionResults;

    class RelationResult {
        Result compareResult;
        Result branchResult;
        Result fixUpResult;
        Instruction compareInstruction;
        Instruction negCompareInstruction;
    }

    /*
    This is class is used for returning Array Related Instructions
     */
    class ArrayBase {
        ArrayList<Integer> instructionIds;
        Result finalResult; // i.e. after summing up all dimensions -> k.mn + i.n + j for indices [k][i][j] and [l][m][n]

        ArrayBase() {
            instructionIds = new ArrayList<>();
        }
    }

    public InstructionGenerator(HashMap<Instruction, Result> instructionResults,
                                HashMap<Integer, Instruction> allInstructions) {
        operations = new HashMap<Token, Operation>();
        operations.put(PLUS, ADD);
        operations.put(MINUS, SUB);
        operations.put(TIMES, MUL);
        operations.put(EQL, BNE);
        operations.put(NEQ, BEQ);
        operations.put(LSS, BGE);
        operations.put(LEQ, BGT);
        operations.put(GTR, BLE);
        operations.put(GEQ, BLT);
        operations.put(Token.DIV, Operation.DIV);
        instructions = allInstructions;
        this.instructionResults = instructionResults;
    }

    public Instruction getInstruction(Integer instructionId) {
        return instructions.get(instructionId);
    }

    public void printTotalNumberOfInstructions() {
        System.out.println("Total Number of Instructions are " + instructions.size());
        System.out.println("Total Number of Instruction Results are " + instructionResults.size());
    }

    private Instruction generateInstruction(Operation operation, Result r1, Result r2) {
        Instruction instruction = new Instruction();
        instruction.setOperation(operation);
        instruction.setOperand1(r1);
        instruction.setOperand2(r2);
        Instruction prev = instructions.get(instruction.getInstructionId() - 1);
        if (prev != null) {
            prev.setNextInstruction(instruction);
            instruction.setPrevInstruction(prev);
        }
        instructions.put(instruction.getInstructionId(), instruction);
        return instruction;
    }

    public Result computeExpression(Token token, Result r1, Result r2) {
        if (r1 != null && r2 != null) {
            if (r1.getKind() == CONSTANT && r2.getKind() == CONSTANT) {
                Result constResult = new Result();
                constResult.setKind(CONSTANT);
                if (token == PLUS) {
                    constResult.setValue(r1.getValue() + r2.getValue());
                    return constResult;
                }
                if (token == MINUS) {
                    constResult.setValue(r1.getValue() - r2.getValue());
                    return constResult;
                }
                if (token == TIMES) {
                    constResult.setValue(r1.getValue() * r2.getValue());
                    return constResult;
                }
                if (token == Token.DIV) {
                    constResult.setValue(r1.getValue() / r2.getValue());
                    return constResult;
                }
            } else {
                Instruction instruction = generateInstruction(operations.get(token), r1, r2);
                return resultForInstruction(instruction);
            }
        }
        if (r1 != null) return r1;
        else return r2;
    }

    public Result computeConstantResult(Instruction constantInstruction) {
        Operation operation = constantInstruction.getOperation();
        Result operand1 = constantInstruction.getOperand1();
        Result operand2 = constantInstruction.getOperand2();
        if (operation == ADD) return resultForConstant(operand1.getValue() + operand2.getValue());
        if (operation == SUB) return resultForConstant(operand1.getValue() - operand2.getValue());
        if (operation == Operation.DIV) return resultForConstant(operand1.getValue() / operand2.getValue());
        if (operation == MUL) return resultForConstant(operand1.getValue() * operand2.getValue());
        return null;
    }

    public Result generateInstructionForAssignment(Result r1, Result r2) {
        if (r1.getKind() == VARIABLE) {
            // Here it is MOVE, so move y x => assign x:= y
            Instruction moveInstruction = generateInstruction(MOVE, r2, r1);
            return resultForInstruction(moveInstruction);
        }
        // Else it is a array variable
        Instruction storeInstruction = generateInstruction(STORE, r2, r1);

        Result arrayVariable = new Result();
        arrayVariable.setKind(ARRAY_VARIABLE);
        arrayVariable.setIdentifierName(r1.getIdentifierName());
        storeInstruction.setArrayVariable(arrayVariable);

        return resultForInstruction(storeInstruction);

    }

    public RelationResult computeRelation(Result condition, Result r1, Result r2) {
        RelationResult r = new RelationResult();
        r.compareInstruction = generateInstruction(CMP, r1, r2);
        /*
        Compare Result
         */
        r.compareResult = resultForInstruction(r.compareInstruction);

        /*
        Branch Result
         */
        r.branchResult = new Result();
        r.branchResult.setKind(BRANCH_INSTRUCTION);
        /*
        BRANCH INSTRUCTION IS LIKE
        BEQ a c => branch to c if a == 0
         */
        r.negCompareInstruction = generateInstruction(operations.get(condition.getCondition()), r.compareResult, r.branchResult);
        /*
        FixUp Result - contains the location of the basic block to which it compiler should branch
         */
        r.fixUpResult = new Result();
        r.fixUpResult.setKind(FIX_UP);
        r.fixUpResult.setFixUpInstructionId(r.negCompareInstruction.getInstructionId());
        return r;
    }

    public Instruction generateBranchInstruction() {
        Result r = new Result();
        r.setKind(BRANCH_INSTRUCTION);
        return generateInstruction(BRA, r, null);
    }

    public Instruction generateEndInstruction() {
        return generateInstruction(END, null, null);
    }

    public ArrayBase generateInstructionsForArrDim(ArrayList<Result> dimExps, ArrayList<Integer> dims) {

        ArrayBase res = handleArrayDim(dimExps, dims);

        Result integerSizeMultiplier = new Result();
        integerSizeMultiplier.setKind(CONSTANT);
        integerSizeMultiplier.setValue(4);

        Instruction dimInstruction = generateInstruction(MULI, res.finalResult, integerSizeMultiplier);
        res.instructionIds.add(dimInstruction.getInstructionId());

        res.finalResult = resultForInstruction(dimInstruction);
        return res;
    }

    private ArrayBase handleArrayDim(ArrayList<Result> dimExps, ArrayList<Integer> dims) {
        ArrayBase result = new ArrayBase();
        if (dims.size() == 1) {
            result.finalResult = dimExps.get(0);
            return result;
        }
        Integer prodDim = 1;
        for (int dim : dims) prodDim *= dim;

        prodDim /= dims.get(0);
        Result dim0Result = generateInstructionForDimension(prodDim, dimExps.get(0));

        prodDim /= dims.get(1);
        Result dim1Result = generateInstructionForDimension(prodDim, dimExps.get(1));

        result.finalResult = generateAddInstructionForTwoResults(dim0Result, dim1Result);

        result.instructionIds.add(dim0Result.getInstructionId());
        result.instructionIds.add(dim1Result.getInstructionId());
        result.instructionIds.add(result.finalResult.getInstructionId());

        for (int i = 2; i < dimExps.size(); i++) {
            prodDim /= dims.get(i);
            Result dimIResult = generateInstructionForDimension(prodDim, dimExps.get(i));
            result.finalResult = generateAddInstructionForTwoResults(result.finalResult, dimIResult);
            result.instructionIds.add(dimIResult.getInstructionId());
            result.instructionIds.add(result.finalResult.getInstructionId());
        }
        return result;
    }

    // Check about that Need to do MUL (i) #4
    private Result generateInstructionForDimension(int multiplier, Result currDimResult) {
        // Make a result class for a multiplier
        Result multiplierRes = new Result();
        multiplierRes.setKind(CONSTANT);
        multiplierRes.setValue(multiplier);

        // Now generate the MUL instruction
        Instruction mulInstruction = generateInstruction(MUL, currDimResult, multiplierRes);

        // Make a result class to hold this result
        return resultForInstruction(mulInstruction);
    }

    private Result generateAddInstructionForTwoResults(Result r1, Result r2) {
        Instruction addInstruction = generateInstruction(ADD, r1, r2);
        return resultForInstruction(addInstruction);
    }

    public ArrayBase computeArrayDesignator(Result arrDimResult, String arrayIdentifier, boolean isRHS) {
        //TODO: Need to understand frame pointer

        ArrayBase arrayBase = new ArrayBase();
        Result baseAddress = new Result();
        baseAddress.setKind(BASE_ADDRESS);
        baseAddress.setIdentifierName(arrayIdentifier);

        //TODO: Fix up base Address

        Result framePointer = new Result();
        framePointer.setKind(FRAME_POINTER);
        //TODO: Deal with frame pointer


        Instruction arrayBaseAddrInstr = generateInstruction(ADD, framePointer, baseAddress);
        arrayBase.instructionIds.add(arrayBaseAddrInstr.getInstructionId());

        // Keep array base address in a result
        Result arrayBaseAddrInstrRes = resultForInstruction(arrayBaseAddrInstr);

        Instruction locInArrayInstr = generateInstruction(ADDA, arrDimResult, arrayBaseAddrInstrRes);
        Result addaResult = resultForInstruction(locInArrayInstr);
        arrayBase.instructionIds.add(locInArrayInstr.getInstructionId());

        Result arrayVariable = new Result();
        arrayVariable.setKind(ARRAY_VARIABLE);
        arrayVariable.setIdentifierName(arrayIdentifier);
        locInArrayInstr.setArrayVariable(arrayVariable);

        if (isRHS) {
            Instruction loadInstruction = generateInstruction(LOAD, addaResult, null);
            loadInstruction.setArrayVariable(arrayVariable);
            arrayBase.instructionIds.add(loadInstruction.getInstructionId());
            arrayBase.finalResult = resultForInstruction(loadInstruction);
            arrayBase.finalResult.setIdentifierName(arrayIdentifier);
            // Keep the above instruction in a result
            return arrayBase;
        }
        arrayBase.finalResult = addaResult;
        arrayBase.finalResult.setIdentifierName(arrayIdentifier);
        // Keep the above instruction in a result
        return arrayBase;

    }

    public Result generateInstructionForReturn(Result result) {
        return resultForInstruction(generateInstruction(RET, result, null));
    }

    private Result resultForInstruction(Instruction instruction) {
        Result result = new Result();
        result.setKind(INSTRUCTION);
        result.setInstructionId(instruction.getInstructionId());
        instructionResults.put(instruction, result);
        return result;
    }

    public ArrayList<Instruction> generateInstructionForParams(ArrayList<Result> parameters) {
        ArrayList<Instruction> instructions = new ArrayList<>();
        for (Result paramResult : parameters) {
            instructions.add(generateInstruction(PARAM, paramResult, null));
        }
        return instructions;
    }

    public Result generateInstructionForFunctionCall(Integer parameterCount, Integer funcBasicBlockId) {
        Result funcResult = new Result();
        funcResult.setKind(FUNCTION);
        funcResult.setFuncBasicBlockId(funcBasicBlockId);

        Result paramResult = new Result();
        paramResult.setKind(PARAMETER_COUNT);
        paramResult.setParameterCount(parameterCount);

        Instruction instruction = generateInstruction(Operation.CALL, funcResult, paramResult);
        return resultForInstruction(instruction);
    }

    public Result generateInstructionForPreDefinedFunctions(String funcName, Result paramResult) {
        if (funcName.equals("InputNum")) {
            if (paramResult != null) generateError("Parameter to InputNum is not expected");
            Instruction instruction = generateInstruction(READ, null, null);
            return resultForInstruction(instruction);
        }
        if (funcName.equals("OutputNum")) {
            if (paramResult == null) generateError("Parameter to OutputNum is expected");
            Instruction instruction = generateInstruction(WRITE, paramResult, null);
            return resultForInstruction(instruction);
        }
        if (funcName.equals("OutputNewLine")) {
            if (paramResult != null) generateError("Parameter to OutputNewLine is not expected");
            Instruction instruction = generateInstruction(WRITENL, null, null);
            return resultForInstruction(instruction);
        }
        return null;
    }

    public Instruction generateInstructionToInitVar(String identifier) {
        Result zero = new Result();
        zero.setKind(CONSTANT);
        zero.setValue(0);
        Result varResult = new Result();
        varResult.setKind(VARIABLE);
        varResult.setIdentifierName(identifier);
        return generateInstruction(MOVE, zero, varResult);
    }

    public Result generatePhiInstruction(Result lhs, Result rhs) {
        Instruction phiInstruction = generateInstruction(PHI, lhs, rhs);
        Result phiResult = resultForVariable(lhs.getIdentifierName(), phiInstruction.getInstructionId());
        phiInstruction.setOperand3(phiResult);
        return resultForInstruction(phiInstruction);
    }

    public Instruction generateKillInstruction(Result arrayVariable) {
        Instruction killInstruction = generateInstruction(KILL, null, null);
        killInstruction.setArrayVariable(arrayVariable);
        return killInstruction;
    }

    public Result generateMoveInstructionForPhi(Result from, Result to) {
        Instruction moveInstruction = generateInstruction(MOVE, from, to);
        return resultForInstruction(moveInstruction);
    }

    public Result resultForVariable(String identifier, Integer ssaVersion) {
        Result result = new Result();
        result.setKind(VARIABLE);
        result.setIdentifierName(identifier);
        result.setSsaVersion(ssaVersion);
        return result;
    }

    public Result resultForConstant(Integer value) {
        Result result = new Result();
        result.setKind(CONSTANT);
        result.setValue(value);
        return result;
    }

    public void generateError(String message) {
        System.out.println("Syntax Error occurred in Instruction generator - " + message);
        System.exit(1);
    }
}
