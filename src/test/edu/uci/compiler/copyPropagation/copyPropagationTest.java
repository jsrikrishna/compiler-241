package test.edu.uci.compiler.copyPropagation;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

import main.edu.uci.compiler.parser.Parser;


public class copyPropagationTest {
    @Test
    public void testParser() {
        String resourcePath = "resources/programs";
        boolean noExceptionOccurred = true;

        try {
            //To Test one code at a time
            String fileName = resourcePath + "/test009.txt";
//             String fileName = resourcePath + "/big.txt";
//            String fileName = resourcePath + "/cell.txt";
            System.out.println("File name is " + fileName);
            Parser parser_cp = new Parser(fileName);
            parser_cp.computation();
            parser_cp.doCopyPropagation();
            parser_cp.generateCFG(true);
        } catch (IOException ex) {
            System.out.println("Exception is " + ex.getMessage());
            noExceptionOccurred = false;
        }
        assertTrue(noExceptionOccurred);
    }
}

