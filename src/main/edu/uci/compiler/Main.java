package main.edu.uci.compiler;

import main.edu.uci.compiler.parser.Scanner;

import java.io.IOException;
import main.edu.uci.compiler.model.*;

public class Main {

    public static void main(String[] args) {
        try {
            Scanner s = new Scanner("test001.txt");
            Token str;
            while (!s.isEOF()){
                str = s.getToken();
                System.out.println(str);
                if(str == Token.ERROR) break;
            }
        } catch (IOException e){
            System.out.println("IO Exception has happened " + e);
        } catch (Exception e){
            System.out.println("something weired has happened " + e);
        }
    }
}
