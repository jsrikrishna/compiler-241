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
        peekSymbol = reader.peekSymbol();
        if(currentSymbol == 255) isEOF = true;
    }

    public boolean isEOF() {
        return isEOF;
    }

    public Token getToken() throws IOException {
        this.setToken();
        return this.currentToken;
    }

    public void gotoNextSymbol() throws IOException {
        if(currentSymbol == 255) isEOF = true;
        else {
            currentSymbol = reader.getCurrentSymbol();
            peekSymbol = reader.peekSymbol();
        }
    }

    public void skipNextAndMove() throws  IOException {
        gotoNextSymbol();
        gotoNextSymbol();
    }

    public void gotoNextLine() throws IOException {
        reader.gotoNextLine();
        gotoNextSymbol();
    }

    public void consumeWhiteSpaceAndComments() throws IOException {
        while (Character.isWhitespace(currentSymbol) || Character.isSpaceChar(currentSymbol)
                || currentSymbol == '\t' || currentSymbol == '#') {
            if(currentSymbol == '#') gotoNextLine();
            else {
                currentSymbol = reader.getCurrentSymbol();
                peekSymbol = reader.peekSymbol();
            }
        }
        while (currentSymbol == '/'){
            if(peekSymbol == '/'){
                gotoNextLine();
            } else break;
        }
//        if(currentSymbol == '#'){
//            gotoNextLine();
//            consumeWhiteSpaceAndComments(); // This done because, after going to next line
//        }
    }

    public void setToken() throws IOException {
        consumeWhiteSpaceAndComments();
        StringBuffer symbolInString = new StringBuffer();
        symbolInString.append(currentSymbol);
        if(currentSymbol == 255) {
            isEOF = true;
            currentToken = Token.EOF;
            return;
        }
        if(singleTokenMap.containsKey(symbolInString.toString())){
            currentToken = singleTokenMap.get(symbolInString.toString());
            gotoNextSymbol();
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
                gotoNextSymbol();
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
        if(Character.isAlphabetic(currentSymbol)){
            StringBuffer token = new StringBuffer();
            while (Character.isAlphabetic(currentSymbol) || Character.isDigit(currentSymbol)){
                token.append(currentSymbol);
                if(peekSymbol == '$') {
                    gotoNextSymbol();
                    break;
                }
                gotoNextSymbol();
            }
            if(keywordTokenMap.containsKey(token.toString())) currentToken = keywordTokenMap.get(token.toString());
            else currentToken = Token.IDEN; // else it is an identifier

            return;
        }
        if(Character.isDigit(currentSymbol)){
            StringBuffer token = new StringBuffer();
            while (Character.isDigit(currentSymbol)){
                token.append(currentSymbol);
                if(peekSymbol == '$') {
                    gotoNextSymbol();
                    break;
                }
                gotoNextSymbol();
            }
            System.out.println("Token is " + token.toString());
            currentToken = Token.NUMBER;
            return;
        }
        System.out.println("error symbol found is " + currentSymbol);
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
