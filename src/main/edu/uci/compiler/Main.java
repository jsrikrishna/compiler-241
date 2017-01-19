package main.edu.uci.compiler;

import main.edu.uci.compiler.parser.Scanner;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        try {
            Scanner s = new Scanner("test001.txt");
            while (!s.isEOF()){
                System.out.println(s.getToken());
            }
        } catch (IOException e){
            System.out.println("IO Exception has happened " + e);
        } catch (Exception e){
            System.out.println("something weired has happened " + e);
        }
    }
}
