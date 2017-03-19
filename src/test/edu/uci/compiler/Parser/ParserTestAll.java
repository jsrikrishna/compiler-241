package test.edu.uci.compiler.Parser;

import main.edu.uci.compiler.parser.Parser;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Created by srikrishna on 2/1/17.
 */
public class ParserTestAll {
    static int index = 1;

    @Test
    public void testParser() {
        String resourcePath = "resources/programs";
        boolean noExceptionOccurred = true;

        for (int i = 1; i <= 31; i++) {
            try {
                String fileName = resourcePath + "/test0" + generateProgramName() + ".txt";
                System.out.println("fileName - " + fileName);
                Parser parser = new Parser(fileName);
                parser.computation();
                parser.printCFG(false, false);
                noExceptionOccurred = true;
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
