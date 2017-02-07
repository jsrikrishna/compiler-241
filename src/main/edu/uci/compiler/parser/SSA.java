package main.edu.uci.compiler.parser;

import java.util.HashMap;

/**
 * Created by srikrishna on 2/6/17.
 */
public class SSA {

    private static SSA instance;
    private HashMap<String, Integer> ssaTrackerForVariables;

    private SSA() {
        ssaTrackerForVariables = new HashMap<>();
    }

    static {
        instance = new SSA();
    }

    public static SSA getInstance() {
        return instance;
    }

    public void updateSSAForVariable(String identifier, Integer instructionId) {
        ssaTrackerForVariables.put(identifier, instructionId);
    }

    public Integer getSSAVersion(String identifier) {
        return ssaTrackerForVariables.get(identifier);
    }

}
