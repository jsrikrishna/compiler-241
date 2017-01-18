package main.edu.uci.compiler.parser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by srikrishna on 1/18/17.
 */

/*
Reader class
1. Get current Symbol
2. Peek Next Symbol
3. Move to Next Symbol
4. Consume Line (if is is Empty)
 */
public class Reader {
    public BufferedReader reader;
    private String line;
    private char currentSymbol;
    private int currentSymbolIndex;
    private int lineLength;
    private int lineNumber;
    private boolean isEOF;

    Reader(String fileName){
        try {
            reader = new BufferedReader(new FileReader(fileName));
            currentSymbolIndex = 0;
            lineLength = 0;
            lineNumber = 0;
            isEOF = false;
        } catch (FileNotFoundException e){
            e.printStackTrace();
        }
    }

    public char getCurrentSymbol() throws IOException {
        if(isEOF) return 255;
        if(lineNumber == 0 || currentSymbolIndex >= lineLength){
            line = reader.readLine();
            while (line != null && line.trim().isEmpty() ) line = reader.readLine();
            if(line == null){
                isEOF = true;
                closeFile();
                return 255; // 255 is token number for EOF
            }
            lineNumber++;
            lineLength = line.length();
            currentSymbolIndex = 0;
        }
        currentSymbol = line.charAt(currentSymbolIndex);
        currentSymbolIndex++;
        return currentSymbol;
    }

    public char peekSymbol(){
        if(isEOF) return 255;
        if(lineNumber == 0 || currentSymbol >= lineLength) return '$';
        return line.charAt(currentSymbolIndex);
    }

    public void closeFile() throws IOException{
        reader.close();
    }
}
