package main.edu.uci.compiler.model;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by srikrishna on 2/13/17.
 */
public class Function {
    private String funcName;
    private static Integer numFunctions = 0;
    private Integer functionId;
    private HashMap<String, Integer> localSSATrackerForVariables;
    private HashMap<String, ArrayList<Integer>> localArrayVariables;
    private ArrayList<String> funcParameters;
    private BasicBlock funcBasicBlock;
    private boolean isVisited; // This member is for printing the basic blocks and functions

    public Function(String funcName){
        this.funcName = funcName;
        this.functionId = numFunctions;
        this.localSSATrackerForVariables = new HashMap<>();
        this.localArrayVariables = new HashMap<>();
        this.funcParameters = new ArrayList<>();
        this.funcBasicBlock = new BasicBlock(BasicBlock.Type.BB_FUNCTION);
        this.isVisited = false;
        ++numFunctions;
    }

    public Integer getFunctionId(){
        return this.functionId;
    }

    public void addLocalSSAVariable(String identifier, Integer instructionId){
        if(instructionId == null){
            instructionId = 1; // this for function parameters only
        }
        this.localSSATrackerForVariables.put(identifier, instructionId);
    }
    public Integer getLocalSSAForVariable(String identifier){
        return this.localSSATrackerForVariables.get(identifier);
    }
    public void addLocalArrayVariable(String arrayIdentifier, ArrayList<Integer> dimensions){
        this.localArrayVariables.put(arrayIdentifier, dimensions);
    }
    public HashMap<String, ArrayList<Integer>> getLocalArrayVariables(){
        return this.localArrayVariables;
    }
    public ArrayList<Integer> getLocalArrayVariable(String arrayIdentifier){
        return this.localArrayVariables.get(arrayIdentifier);
    }

    public void setFuncParameter(String identifier){
        this.funcParameters.add(identifier);
    }
    public void setFuncParameters(ArrayList<String> funcParameters){
        this.funcParameters = funcParameters;
    }
    public ArrayList<String> getFuncParameters(){
        return this.funcParameters;
    }
    public BasicBlock getFuncBasicBlock(){
        return this.funcBasicBlock;
    }
    public String getFuncName(){
        return this.funcName;
    }
    public boolean isVisited(){
        return this.isVisited;
    }
    public void setIsVisited(){
        this.isVisited = true;
    }
}
