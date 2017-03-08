package test.edu.uci.compiler.liveRangeAnalysis;

import main.edu.uci.compiler.parser.Parser;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Created by srikrishna on 3/7/17.
 */
public class LiveRangeTest {
    @Test
    public void testParser() {
        String resourcePath = "resources/programs";
        boolean noExceptionOccurred = true;

        try {
            //To Test one code at a time
            String fileName = resourcePath + "/test007.txt";
//             String fileName = resourcePath + "/big.txt";
//            String fileName = resourcePath + "/cell.txt";
            System.out.println("File name is " + fileName);
            Parser lraParser = new Parser(fileName);
            lraParser.computation();
            lraParser.doCopyPropagation();
            lraParser.doCommonSubExpressionElimination();
            lraParser.printDomVCG();
            lraParser.printCFG(true, true);
            lraParser.doLiveRangeAnalysis();
        } catch (IOException ex) {
            System.out.println("Exception is " + ex.getMessage());
            noExceptionOccurred = false;
        }
        assertTrue(noExceptionOccurred);
    }
}
