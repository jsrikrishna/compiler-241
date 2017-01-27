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
    private int currentNumber; // last number encountered
    private int identifierId; // the last identifier encountered and it is 0-based value
    private String currentIdentifier;
    // Need to maintain a table of unique identifiers
    private ArrayList<String> listOfIdentifiers;
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
        listOfIdentifiers = new ArrayList<>();
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

    public String getCurrentIdentifier(){
        // May need to do null check
        return currentIdentifier;
    }

    public int getIdentifierId(){
        return identifierId;
    }

    public int getCurrentNumber(){
        return currentNumber;
    }

    /*
    Identifier Table Methods
     */
    public String IdToString(int id){
        // if(id < 0 || id > identifierId) return null; // May be throw an exception, Not needed because .get method does it already
        return listOfIdentifiers.get(id);
    }

    public int StringToId(String identifier){
        if(identifier.isEmpty()) return -1; // May be throw an exception
        return listOfIdentifiers.indexOf(identifier);
    }

    private void gotoNextSymbol() throws IOException {
        if(currentSymbol == 255) isEOF = true;
        else {
            currentSymbol = reader.getCurrentSymbol();
            peekSymbol = reader.peekSymbol();
        }
    }

    private void skipNextAndMove() throws  IOException {
        gotoNextSymbol();
        gotoNextSymbol();
    }

    private void gotoNextLine() throws IOException {
        reader.gotoNextLine();
        gotoNextSymbol();
    }

    private void consumeWhiteSpaces() throws IOException {
        while (isWhiteSpace(currentSymbol) || currentSymbol == '\t' || currentSymbol == '#') {
            /*
            If there are spaces or tabs after going to next line once we encounter #,
            then we need to keep this in loop
             */
            if(currentSymbol == '#') gotoNextLine();
            else {
                currentSymbol = reader.getCurrentSymbol();
                peekSymbol = reader.peekSymbol();
            }
        }
    }

    private void consumeComments() throws IOException {
        while (currentSymbol == '/' && peekSymbol == '/') gotoNextLine();
    }

    private void consumeWhiteSpaceAndComments() throws IOException {
        while (true){
            if(isWhiteSpace(currentSymbol) || currentSymbol == '\t' || currentSymbol == '#') consumeWhiteSpaces();
            else if (currentSymbol == '/' && peekSymbol == '/') consumeComments();
            else break;
        }
    }

    private void endOfFile(){
        isEOF = true;
        currentToken = Token.EOF;
        return;
    }

    private void setToken() throws IOException {
        consumeWhiteSpaceAndComments();
        StringBuffer symbolInString = new StringBuffer();
        symbolInString.append(currentSymbol);

        if(currentSymbol == 255) {
            endOfFile();
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
        if(currentSymbol == '>'){
            if(peekSymbol == '='){
                skipNextAndMove();
                currentToken = Token.GEQ;
                return;
            }
            else{
                gotoNextSymbol();
                currentToken = Token.GTR;
                return;
            }

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
            handleIdentifierOrKeyWord();
            return;
        }
        if(Character.isDigit(currentSymbol)){
            handleNumbers();
            return;
        }
        System.out.println("error symbol found is " + currentSymbol);
        currentToken = Token.ERROR;
        return;
    }

    private void handleIdentifierOrKeyWord() throws IOException {
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
        else {
            currentToken = Token.IDEN; // else it is an identifier
            currentIdentifier = token.toString();
            if(listOfIdentifiers.indexOf(token.toString()) != -1){
                identifierId = listOfIdentifiers.indexOf(token.toString());
            } else {
                listOfIdentifiers.add(token.toString());
                identifierId = listOfIdentifiers.size() - 1;
            }
        }
        return;
    }

    private void handleNumbers() throws IOException {
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
        currentNumber = Integer.parseInt(token.toString());
        return;
    }

    private boolean isAlphabet(char currentSymbol){
        return (currentSymbol >= 'a' && currentSymbol <= 'z') || (currentSymbol >= 'A' && currentSymbol <= 'Z');
    }

    private boolean isDigit(char currentSymbol){
        return (currentSymbol >= '0' && currentSymbol <= '9');
    }

    public boolean isWhiteSpace(char currentSymbol){
        return Character.isWhitespace(currentSymbol) || Character.isSpaceChar(currentSymbol);
    }
}
