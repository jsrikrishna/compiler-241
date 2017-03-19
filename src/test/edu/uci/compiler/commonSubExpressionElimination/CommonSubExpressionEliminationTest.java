package test.edu.uci.compiler.commonSubExpressionElimination;

import main.edu.uci.compiler.parser.Parser;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Created by srikrishna on 2/24/17.
 */
public class CommonSubExpressionEliminationTest {
    @Test
    public void testParser() {
        String resourcePath = "resources/programs";
        boolean noExceptionOccurred = true;

        try {
            //To Test one code at a time
            String fileName = resourcePath + "/test027.txt";
//             String fileName = resourcePath + "/big.txt";
//            String fileName = resourcePath + "/cell.txt";
            System.out.println("File name is " + fileName);
            Parser cseParser = new Parser(fileName);
            cseParser.computation();
            cseParser.doCopyPropagation();
            cseParser.doCommonSubExpressionElimination();
            cseParser.printDomVCG();
            cseParser.printCFG(true, true);
        } catch (IOException ex) {
            System.out.println("Exception is " + ex.getMessage());
            noExceptionOccurred = false;
        }
        assertTrue(noExceptionOccurred);
    }
}
