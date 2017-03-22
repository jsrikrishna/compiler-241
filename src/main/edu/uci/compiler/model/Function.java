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
    private ArrayList<Result> funcParameters;
    private BasicBlock funcBasicBlock;
    private boolean isVisited; // This member is for printing the basic blocks and functions
    private boolean isVisitedAfterPhiRemoval;

    public Function(String funcName){
        this.funcName = funcName;
        this.functionId = numFunctions;
        this.localSSATrackerForVariables = new HashMap<>();
        this.localArrayVariables = new HashMap<>();
        this.funcParameters = new ArrayList<>();
        this.funcBasicBlock = new BasicBlock(BasicBlock.Type.BB_FUNCTION);
        this.isVisited = false;
        this.isVisitedAfterPhiRemoval = false;
        ++numFunctions;
    }

    public Integer getFunctionId(){
        return this.functionId;
    }

    public void updateSSAVariable(String identifier, Integer instructionId){
        this.localSSATrackerForVariables.put(identifier, instructionId);
    }
    public Integer getSSAForVariable(String identifier){
        return this.localSSATrackerForVariables.get(identifier);
    }

    public boolean isLocalVariable(String identifier){
        return this.localSSATrackerForVariables.containsKey(identifier);
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

    public void setFuncParameter(Result parameter){
        this.funcParameters.add(parameter);
    }
    public void setFuncParameters(ArrayList<Result> funcParameters){
        this.funcParameters = funcParameters;
    }
    public ArrayList<Result> getFuncParameters(){
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
    public void setIsVisitedAfterPhiRemoval(){
        this.isVisitedAfterPhiRemoval = true;
    }
    public boolean isVisitedAfterPhiRemoval(){
        return this.isVisitedAfterPhiRemoval;
    }
}
