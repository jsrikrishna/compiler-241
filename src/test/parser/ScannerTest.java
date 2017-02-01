package test.parser;

import main.edu.uci.compiler.model.Token;
import main.edu.uci.compiler.parser.Scanner;
import org.junit.Test;
import test.parser.model.TokenizeException;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Created by srikrishna on 1/25/17.
 */
public class ScannerTest {
    static int index = 1;
    @Test
    public void testScanner() {
        generateProgramName();
        String resourcePath = "resources/programs";
        boolean noExceptionOccurred;

        for (int i = 1; i <= 30; i++) {
            try {
                String fileName = resourcePath + "/test0" + generateProgramName() + ".txt";
                Scanner s = new Scanner(fileName);
                Token str;
                while (!s.isEOF()) {
                    str = s.getToken();
                    if (str == Token.ERROR)
                        throw new TokenizeException("in file " + fileName);
                }
                noExceptionOccurred = true;
            } catch (IOException | TokenizeException ex) {
                System.out.println("Exception is " + ex.getMessage());
                noExceptionOccurred = false;
            }
            assertTrue(noExceptionOccurred);
        }
    }

    public String generateProgramName(){
        StringBuffer s = new StringBuffer();
        if(index < 10){
            s.append(0);
            s.append(index);
        } else {
            s.append(index);
        }
//        System.out.println(s.toString());
        ++index;
        return s.toString();
    }
}
