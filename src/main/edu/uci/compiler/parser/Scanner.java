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
    }

    public boolean isEOF() {
        return isEOF;
    }

    public Token getToken() throws IOException {
        this.setToken();
        return this.currentToken;
    }

    public void gotoNextSymbolAndPeak() throws IOException {
        if(currentSymbol == 255) isEOF = true;
        else {
            currentSymbol = reader.getCurrentSymbol();
            peekSymbol = reader.peekSymbol();
        }
    }

    public void skipNextAndMove() throws  IOException {
        gotoNextSymbolAndPeak();
        gotoNextSymbolAndPeak();
    }

    public void consumeWhiteSpaces() throws IOException {
        while (currentSymbol == ' ') currentSymbol = reader.getCurrentSymbol();
        peekSymbol = reader.peekSymbol();
    }

    public void setToken() throws IOException {
        consumeWhiteSpaces();
        StringBuffer symbolInString = new StringBuffer();
        symbolInString.append(currentSymbol);
        if(currentSymbol == 255) {
            isEOF = true;
            currentToken = Token.EOF;
            return;
        }
        if(singleTokenMap.containsKey(symbolInString.toString())){
            currentToken = singleTokenMap.get(symbolInString.toString());
            gotoNextSymbolAndPeak();
            return;
        }
        // Single Map don't deal with ==, !=, >, >=, <-, <=, <
        if(currentSymbol == '<'){
            if(peekSymbol == '=') {
                currentToken = Token.LEQ;
                skipNextAndMove();
                return;
            }
            else if(peekSymbol == '-') {
                currentToken = Token.BECOMES;
                skipNextAndMove();
                return;
            }
            else {
                currentToken = Token.LSS;
                gotoNextSymbolAndPeak();
                return;
            }
        }
        if(currentSymbol == '>' && peekSymbol == '='){
            skipNextAndMove();
            currentToken = Token.GEQ;
            return;
        }
        if(currentSymbol == '=' && peekSymbol == '='){
            skipNextAndMove();
            currentToken = Token.EQL;
            return;
        }
        if(currentSymbol == '!' && peekSymbol == '='){
            skipNextAndMove();
            currentToken = Token.NEQ;
            return;
        }
        // Now the token could be an number, identifier, keyword
        if(isAlphabet(currentSymbol)){
            StringBuffer token = new StringBuffer();
            while (isAlphabet(currentSymbol) || isDigit(currentSymbol)){
                token.append(currentSymbol);
                if(peekSymbol == '$') {
                    gotoNextSymbolAndPeak();
                    break;
                }
                gotoNextSymbolAndPeak();
            }
            if(keywordTokenMap.containsKey(token.toString())) currentToken = keywordTokenMap.get(token.toString());
            else currentToken = Token.IDEN; // else it is an identifier

            return;
        }
        if(isDigit(currentSymbol)){
            StringBuffer token = new StringBuffer();
            while (isDigit(currentSymbol)){
                token.append(currentSymbol);
                if(peekSymbol == '$') break;
                gotoNextSymbolAndPeak();
            }
            System.out.println("Token is " + token.toString());
            currentToken = Token.NUMBER;
            return;
        }
        System.out.println("error token found is " + currentSymbol);
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
