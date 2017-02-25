package test.edu.uci.compiler.copyPropagation;

import main.edu.uci.compiler.parser.Parser;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Created by srikrishna on 2/25/17.
 */
public class CopyPropagationTestAll {
    static int index = 1;

    @Test
    public void testParser() {
        String resourcePath = "resources/programs";
        boolean noExceptionOccurred = true;

        for (int i = 1; i <= 31; i++) {
            try {
                String fileName = resourcePath + "/test0" + generateProgramName() + ".txt";
                System.out.println("fileName - " + fileName);
                Parser parser_cp = new Parser(fileName);
                parser_cp.computation();
                parser_cp.doCopyPropagation();
                parser_cp.generateCFG(true, false);
            } catch (IOException ex) {
                System.out.println("Exception is " + ex.getMessage());
                noExceptionOccurred = false;
            }
            assertTrue(noExceptionOccurred);
        }
    }

    public String generateProgramName() {
        StringBuffer s = new StringBuffer();
        if (index < 10) {
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
