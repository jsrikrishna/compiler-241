package main.edu.uci.compiler;

import main.edu.uci.compiler.parser.Parser;
import main.edu.uci.compiler.parser.Scanner;
import main.edu.uci.compiler.model.Token;
import java.io.IOException;
import main.edu.uci.compiler.model.*;

public class Main {

    public static void main(String[] args) {
        try {
            String resourcePath = "resources/programs";
            String fileName = resourcePath + "/test002.txt";
            Parser p = new Parser(fileName);
            p.computation();
        } catch (IOException e){
            System.out.println("IO Exception has happened " + e);
        } catch (Exception e){
            System.out.println("hey, something weird has happened " + e);
        }
    }
}
