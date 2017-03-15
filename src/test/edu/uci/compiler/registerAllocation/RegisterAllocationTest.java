package test.edu.uci.compiler.registerAllocation;

import main.edu.uci.compiler.parser.Parser;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Created by srikrishna on 3/13/17.
 */
public class RegisterAllocationTest {
    @Test
    public void testParser() {
        String resourcePath = "resources/programs";
        boolean noExceptionOccurred = true;

        try {
            //To Test one code at a time
            String fileName = resourcePath + "/test031.txt";
//             String fileName = resourcePath + "/big.txt";
//            String fileName = resourcePath + "/cell.txt";
            System.out.println("File name is " + fileName);
            Parser ra = new Parser(fileName);
            ra.computation();
            ra.doCopyPropagation();
            ra.doCommonSubExpressionElimination();
            ra.printDomVCG();
            ra.printCFG(true, true);
            ra.doLiveRangeAnalysis();
            ra.doRegisterAllocation(fileName);
        } catch (IOException ex) {
            System.out.println("Exception is " + ex.getMessage());
            noExceptionOccurred = false;
        }
        assertTrue(noExceptionOccurred);
    }
}
