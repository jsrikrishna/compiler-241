package test.edu.uci.compiler.Parser;

import main.edu.uci.compiler.parser.Parser;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Created by srikrishna on 3/6/17.
 */
public class NumberOfInstructionsTest {
    @Test
    public void testParser() {
        String resourcePath = "resources/programs";
        boolean noExceptionOccurred = true;

        try {
            //To Test one code at a time
            String fileName = resourcePath + "/test009.txt";
//            String fileName = resourcePath + "/big.txt";
//            String fileName = resourcePath + "/cell.txt";
            System.out.println("File name is " + fileName);
            Parser parser_no_cp = new Parser(fileName);
            parser_no_cp.computation();
            parser_no_cp.printNumberOfInstructions();
        } catch (IOException ex) {
            System.out.println("Exception is " + ex.getMessage());
            noExceptionOccurred = false;
        }
        assertTrue(noExceptionOccurred);
    }
}
