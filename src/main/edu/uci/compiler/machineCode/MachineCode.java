package main.edu.uci.compiler.machineCode;

import main.edu.uci.compiler.model.*;

import static main.edu.uci.compiler.model.Operation.*;

import java.util.*;

import static main.edu.uci.compiler.model.ErrorMessage.*;
import static main.edu.uci.compiler.model.BasicBlock.Type.*;
import static main.edu.uci.compiler.model.Result.KIND.*;

/**
 * Created by srikrishna on 3/20/17.
 */
public class MachineCode {

    private static final int STACK_POINTER = 29;
    private static final int FRAME_POINTER = 28;
    private static final int GLOBAL_VARIABLE_MEMORY = 30;

    private List<Integer> codes;
    private int codeNumber;
    private Map<Integer, Integer> blockVsCodeNumber;
    private Stack<Integer> branchFixUpLocations;

    public MachineCode() {
        codes = new ArrayList<Integer>();
        blockVsCodeNumber = new HashMap<Integer, Integer>();
    }

    public void generateCode(BasicBlock basicBlock) {
        generateCodeForBlockBody(basicBlock);
        if (basicBlock.getType() == BB_WHILE_CONDITION_AND_JOIN) {

        } else if (basicBlock.getType() == BB_WHILE_BODY) {

        } else if (basicBlock.getType() == BB_IF_CONDITION) {

        } else if (basicBlock.getType() == BB_IF_ELSE_JOIN || basicBlock.getType() == BB_IF_THEN_JOIN) {
            int code = blockVsCodeNumber.get(basicBlock.getId());
            updateBranchCode(OpCode.JSR, code);

        } else if (basicBlock.getType() == BB_ELSE) {

        } else if (basicBlock.getType() == BB_NORMAL) {

        }

        for (BasicBlock child : basicBlock.getChildren()) {
            generateCode(child);
        }

    }

    public void addCodeToProgram(int code) {
        codes.add(code);
        codeNumber++;
    }

    public void updateCodeIntheProgram(int code, int position) {
        if (position < 0 || position >= codes.size()) {
            generateError(WRONG_FIX_UP_LOCATION);
            return;
        }
        codes.add(position, code);
    }

    public void generateCodeForBlockBody(BasicBlock basicBlock) {
        blockVsCodeNumber.put(basicBlock.getId(), codeNumber);
        List<Instruction> instructions = basicBlock.getInstructions();
        for (Instruction instruction : instructions) {
            Operation operation = instruction.getOperation();
            switch (operation) {
                case ADD:
                    addArithmeticCode(OpCode.ADD, OpCode.ADDI, instruction);
                    break;
                case SUB:
                    addArithmeticCode(OpCode.SUB, OpCode.SUBI, instruction);
                    break;
                case MUL:
                    addArithmeticCode(OpCode.MUL, OpCode.MULI, instruction);
                    break;
                case DIV:
                    addArithmeticCode(OpCode.DIV, OpCode.DIVI, instruction);
                    break;
                case ADDA:
                    addArithmeticCode(OpCode.ADD, OpCode.ADDI, instruction);
                    break;
                case CMP:
                    addArithmeticCode(OpCode.CMP, OpCode.CMPI, instruction);
                    break;
                case BRA:
                    addBranchCode(OpCode.JSR, instruction);
                case BEQ:
                    addControlCode(OpCode.BEQ, instruction);
                    break;
                case BNE:
                    addControlCode(OpCode.BNE, instruction);
                    break;
                case BLT:
                    addControlCode(OpCode.BLT, instruction);
                    break;
                case BGE:
                    addControlCode(OpCode.BGE, instruction);
                    break;
                case BLE:
                    addControlCode(OpCode.BLE, instruction);
                    break;
                case BGT:
                    addControlCode(OpCode.BGT, instruction);
                    break;
                case CALL:
                    addCallCode(instruction);
                    break;
                case RET:
                    addReturnCode(instruction);
                    break;
                case PARAM:
                    addParamCode(instruction);
                    break;
            }
        }
    }


    private void addBranchCode(OpCode jsr, Instruction instruction) {
        Result operand1 = instruction.getOperand1();
        Integer targetCode = blockVsCodeNumber.get(operand1.getBasicBlockId());
        if (targetCode == null) {
            targetCode = 0;
        }
        branchFixUpLocations.add(codeNumber);
        //Putting the target code as 0 as a place holder.
        //This will be changed once we fix up the branch locations.
        int code = getF3(jsr.getOpcode(), targetCode);
        addCodeToProgram(code);

    }

    private void updateBranchCode(OpCode jsr, int targetCode) {
        int position = branchFixUpLocations.pop();
        int code = getF3(jsr.getOpcode(), targetCode);
        updateCodeIntheProgram(code, position);
    }

    private void addParamCode(Instruction instruction) {


    }

    private void addReturnCode(Instruction instruction) {


    }

    private void addCallCode(Instruction instruction) {

    }

    private void addControlCode(OpCode opCode, Instruction instruction) {

    }

    private void addArithmeticCode(OpCode opCode, OpCode immediateOpCode, Instruction instruction) {
        Result operand1 = instruction.getOperand1();
        Result operand2 = instruction.getOperand2();
        int resultRegNo = instruction.getRegisterNumber();
        int code = 0;
        if (operand1.getKind() == REGISTER && operand2.getKind() == REGISTER) {
            code = getF2(opCode.getOpcode(), resultRegNo, operand1.getRegisterNumber(), operand2.getRegisterNumber());
        } else if (operand1.getKind() == REGISTER && operand2.getKind() == CONSTANT) {
            code = getF1(immediateOpCode.getOpcode(), resultRegNo, operand1.getRegisterNumber(), operand2.getValue());
        } else if (operand1.getKind() == CONSTANT && operand2.getKind() == REGISTER) {
            code = getF1(immediateOpCode.getOpcode(), resultRegNo, operand2.getRegisterNumber(), operand1.getValue());
        }
        addCodeToProgram(code);

    }

    private void addCode(Integer code) {
        codes.add(code);
        codeNumber++;
    }

    private int getF1(Integer op, Integer a, Integer b, Integer c) {
        if (op > 61 || op < 0) generateError(OPCODE_ERROR);
        if (a > 31 || a < 0 || b > 31 || b < 0) generateError(REGISTER_NOT_IN_RANGE);
        if (c > (65536 - 1) || c < 0) generateError(LITERAL_NOT_IN_RANGE);
        return op << 26 | a << 21 | b << 16 | c;
    }

    private int getF2(Integer op, Integer a, Integer b, Integer c) {
        if (op > 61 || op < 0) generateError(OPCODE_ERROR);
        if (a > 31 || a < 0 || b > 31 || b < 0 || c > 31 || c < 0) generateError(REGISTER_NOT_IN_RANGE);
        return op << 26 | a << 21 | b << 16 | c;
    }

    private int getF3(Integer op, Integer c) {
        if (op > 61 || op < 0) generateError(OPCODE_ERROR);
        if (c < 0 || c > 67108864 - 1) generateError(ABSOLUTE_NOT_IN_RANGE);
        return op << 26 | c;
    }

    private void generateError(ErrorMessage error) {
        System.err.println(error);
        System.exit(101);
    }
}
