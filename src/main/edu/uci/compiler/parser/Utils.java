package main.edu.uci.compiler.parser;

import main.edu.uci.compiler.model.ErrorMessage;

import java.util.HashMap;

/**
 * Created by srikrishna on 2/6/17.
 */
public class Utils {
    private static Utils instance;

    private Utils() {
    }

    static {
        instance = new Utils();
    }

    public static Utils getInstance() {
        return instance;
    }

    public void generateError(ErrorMessage message) {
        System.out.println("Syntax Error occurred - " + message);
        System.exit(1);
    }
}
