package main.edu.uci.compiler.parser;

import main.edu.uci.compiler.model.Token;

import java.io.IOException;
import java.util.*;

/**
 * Created by srikrishna on 1/18/17.
 */

/*
Scanner tokenize the char stream
1. Skip Comments starting with //
2. Get Next Token
 */
public class Scanner {
    private Reader reader;
    private char currentSymbol;
    private char peekSymbol;
    private Token currentToken;
    private boolean isEOF;
    private Map<String, Token> singleTokenMap = new HashMap<String, Token>(){{
        put("*", Token.TIMES);
        put("/", Token.DIV);
        put("+", Token.PLUS);
        put("-", Token.MINUS);
        put("(", Token.OPENPAREN);
        put(")", Token.CLOSEPAREN);
        put("[", Token.OPENBRACKET);
        put("]", Token.CLOSEBRACKET);
        put("{", Token.BEGIN);
        put("}", Token.END);
        put(";", Token.SEMICOLON);
        put(",", Token.COMMA);
        put(".", Token.PERIOD);
    }};
    private Map<String, Token> keywordTokenMap = new HashMap<String, Token>(){{
        put("let", Token.LET);
        put("call", Token.CALL);
        put("if", Token.IF);
        put("then", Token.THEN);
        put("else", Token.ELSE);
        put("fi", Token.FI);
        put("while", Token.WHILE);
        put("do", Token.DO);
        put("od", Token.OD);
        put("return", Token.RETURN);
        put("var", Token.VAR);
        put("array", Token.ARRAY);
        put("function", Token.FUNCTION);
        put("procedure", Token.PROCEDURE);
        put("main", Token.MAIN);
    }};


    public Scanner(String fileName) throws IOException {
        reader = new Reader(fileName);
        isEOF = false;
        currentSymbol = reader.getCurrentSymbol();
        if(currentSymbol == 255) isEOF = true;
        System.out.println("First symbol is ");
    }

    public boolean isEOF() {
        return isEOF;
    }

    public Token getToken() throws IOException {
        this.setToken();
        gotoNextSymbolAndPeak();
        return this.currentToken;
    }

    public void gotoNextSymbolAndPeak() throws IOException {
        if(currentSymbol == 255) {
            isEOF = true;
            currentSymbol = 255;
        } else {
            currentSymbol = reader.getCurrentSymbol();
            peekSymbol = reader.peekSymbol();
        }
    }

    public void setToken() throws IOException {
        String symbolInString = new StringBuffer(currentSymbol).toString();
        peekSymbol = reader.peekSymbol();
        if(currentSymbol == 255) {
            isEOF = true;
            currentToken = Token.EOF;
            return;
        }
        if(singleTokenMap.containsKey(symbolInString)){
            currentToken = singleTokenMap.get(symbolInString);
            return;
        }
        // Single Map don't deal with ==, !=, >, >=, <-, <=, <
        if(currentSymbol == '<'){
            if(peekSymbol == '=') {
                gotoNextSymbolAndPeak();
                currentToken = Token.LEQ;
            }
            else if(peekSymbol == '-') {
                gotoNextSymbolAndPeak();
                currentToken = Token.BECOMES;
            }
            else currentToken = Token.LSS;
            return;
        }
        if(currentSymbol == '>' && peekSymbol == '='){
            gotoNextSymbolAndPeak();
            currentToken = Token.GEQ;
            return;
        }
        if(currentSymbol == '=' && peekSymbol == '='){
            gotoNextSymbolAndPeak();
            currentToken = Token.EQL;
            return;
        }
        if(currentSymbol == '!' && peekSymbol == '='){
            gotoNextSymbolAndPeak();
            currentToken = Token.NEQ;
            return;
        }
        // Now the token could be an number, identifier, keyword
        if(isAlphabet(currentSymbol)){
            StringBuffer token = new StringBuffer();
            while (isAlphabet(currentSymbol) || isDigit(currentSymbol)){
                token.append(currentSymbol);
                gotoNextSymbolAndPeak();
                if(peekSymbol == '$') {
                    token.append(currentSymbol);
                    break;
                }
            }
            if(keywordTokenMap.containsKey(token.toString())) currentToken = keywordTokenMap.get(token.toString());
            else currentToken = Token.IDEN; // else it is an identifier
//            System.out.println("Token is " + token.toString());
            return;
        }
        if(isDigit(currentSymbol)){
            StringBuffer token = new StringBuffer();
            while (isDigit(currentSymbol)){
                token.append(currentSymbol);
                gotoNextSymbolAndPeak();
                if(peekSymbol == '$') break;
            }
            System.out.println("Token is " + token.toString());
            currentToken = Token.NUMBER;
        }
        currentToken = Token.ERROR;
        return;
    }

    private boolean isAlphabet(char currentSymbol){
        return (currentSymbol >= 'a' && currentSymbol <= 'z') || (currentSymbol >= 'A' && currentSymbol <= 'Z');
    }

    private boolean isDigit(char currentSymbol){
        return (currentSymbol >= '0' && currentSymbol <= '9');
    }
}
