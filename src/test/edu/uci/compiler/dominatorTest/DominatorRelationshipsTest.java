package test.edu.uci.compiler.dominatorTest;

import main.edu.uci.compiler.parser.Parser;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Created by srikrishna on 2/22/17.
 */
public class DominatorRelationshipsTest {
    @Test
    public void testDominanceRelationships() {
        String resourcePath = "resources/programs";
        boolean noExceptionOccurred = true;

        try {
            //To Test one code at a time
            String fileName = resourcePath + "/test009.txt";
//             String fileName = resourcePath + "/big.txt";
//             String fileName = resourcePath + "/cell.txt";
            System.out.println("File name is " + fileName);
            Parser parser = new Parser(fileName);
            parser.computation();
            parser.printDomVCG();
        } catch (IOException ex) {
            System.out.println("Exception is " + ex.getMessage());
            noExceptionOccurred = false;
        }
        assertTrue(noExceptionOccurred);
    }
}
