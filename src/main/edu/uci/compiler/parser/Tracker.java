package main.edu.uci.compiler.parser;

import main.edu.uci.compiler.model.Function;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by srikrishna on 2/6/17.
 */
public class Tracker {

    private static Integer defaultSSAVersion = -1;
    private static Tracker instance;
    private HashMap<String, Integer> ssaTrackerForVariables;
    private HashMap<String, ArrayList<Integer>> arrayVariables;
    private HashMap<String, Function> functions;

    public Tracker() {
        ssaTrackerForVariables = new HashMap<>();
        arrayVariables = new HashMap<>();
        functions = new HashMap<>();
    }

    public void updateSSAForVariable(String identifier, Integer instructionId) {
        ssaTrackerForVariables.put(identifier, instructionId);
    }

    public Integer getSSAVersion(String identifier) {
        return ssaTrackerForVariables.get(identifier);
    }
    public Integer getDefaultSSAVersion(){
        return this.defaultSSAVersion;
    }
    public void addArrayVariable(String identifier, ArrayList<Integer> dimensions){
        this.arrayVariables.put(identifier, dimensions);
    }
    public ArrayList<Integer> getArrayVariableDimensions(String identifier){
        return this.arrayVariables.get(identifier);
    }
    public boolean containsArrayVariable(String identifier){
        return this.arrayVariables.containsKey(identifier);
    }
    public void addFunction(String identifier, Function function){
        this.functions.put(identifier, function);
    }
    public Function getFunction(String identifier){
        return this.functions.get(identifier);
    }

}
