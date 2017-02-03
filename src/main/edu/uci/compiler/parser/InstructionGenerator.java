package main.edu.uci.compiler.parser;

import main.edu.uci.compiler.model.*;

import static main.edu.uci.compiler.model.Operation.END;
import static main.edu.uci.compiler.model.Token.*;
import static main.edu.uci.compiler.model.Operation.*;
import static main.edu.uci.compiler.model.Result.KIND.*;

import java.util.HashMap;

/**
 * Created by srikrishna on 2/2/17.
 */
public class InstructionGenerator {
    private HashMap<Integer, Instruction> instructions;
    private HashMap<Token, Operation> operations;
    public InstructionGenerator(){
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
        instructions = new HashMap<Integer, Instruction>();

    }
    class RelationResult {
        Result compareResult;
        Result branchResult;
        Result fixUpResult;
        Instruction compareInstruction;
        Instruction negCompareInstruction;
    }

    public Instruction getInstruction(Integer instructionId){
        return instructions.get(instructionId);
    }

    private Instruction generateInstruction(Operation operation, Result r1, Result r2){
        Instruction instruction = new Instruction();
        instruction.setOperation(operation);
        instruction.setOperand1(r1);
        instruction.setOperand2(r2);
        instructions.put(instruction.getInstructionId(), instruction);
        return instruction;
    }

    public Result computeExpression(Token token, Result r1, Result r2){
        if(r1 != null && r2 != null){
            if(r1.getKind() == CONSTANT && r2.getKind() == CONSTANT){
                Result constResult = new Result();
                constResult.setKind(CONSTANT);
                if(token == PLUS) {
                    constResult.setValue(r1.getValue() + r2.getValue());
                    return constResult;
                }
                if(token == MINUS){
                    constResult.setValue(r1.getValue() - r2.getValue());
                    return constResult;
                }
                if(token == TIMES){
                    constResult.setValue(r1.getValue() * r2.getValue());
                    return constResult;
                }
                if(token == Token.DIV){
                    constResult.setValue(r1.getValue() / r2.getValue());
                    return constResult;
                }
            } else {
                Instruction instruction = generateInstruction(operations.get(token), r1, r2);
                Result res = new Result();
                res.setKind(INSTRUCTION);
                res.setInstructionId(instruction.getInstructionId());
                return res;
            }
        }
        if(r1 != null) return r1; else return r2;
    }

    public Instruction generateInstructionForAssignment(Result r1, Result r2){
        // Here it is MOVE, so move y x => assign x:= y
        return generateInstruction(MOVE, r2, r1);
    }

    public RelationResult computeRelation(Result condition, Result r1, Result r2){
        RelationResult r = new RelationResult();
        r.compareInstruction = generateInstruction(CMP, r1, r2);
        /*
        Compare Result
         */
        r.compareResult = new Result();
        r.compareResult.setKind(INSTRUCTION);
        r.compareResult.setInstructionId(r.compareInstruction.getInstructionId());
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

    public Instruction generateBranchInstruction(){
        Result r = new Result();
        r.setKind(BRANCH_INSTRUCTION);
        return generateInstruction(BRA, r, null);
    }

    public Instruction generateEndInstruction(){
        return generateInstruction(END, null, null);
    }
}
