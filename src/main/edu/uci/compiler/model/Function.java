package main.edu.uci.compiler.model;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by srikrishna on 2/13/17.
 */
public class Function {
    private String funcName;
    private static Integer defaultSSAVersion = -1;
    private HashMap<String, Integer> localSSATrackerForVariables;
    private HashMap<String, ArrayList<Integer>> localArrayVariables;
    private ArrayList<String> funcParameters;
    private BasicBlock funcBasicBlock;

    public Function(String funcName){
        this.funcName = funcName;
        this.localSSATrackerForVariables = new HashMap<>();
        this.localArrayVariables = new HashMap<>();
        this.funcParameters = new ArrayList<>();
        this.funcBasicBlock = new BasicBlock();
    }

    public void addLocalVariable(String identifier, Integer instructionId){
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
    public Integer getDefaultSSAVersion(){
        return this.defaultSSAVersion;
    }
    public BasicBlock getFuncBasicBlock(){
        return this.funcBasicBlock;
    }
    public String getFuncName(){
        return this.funcName;
    }
}
