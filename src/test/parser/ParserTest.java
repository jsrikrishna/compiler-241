package test.parser;

import main.edu.uci.compiler.model.Token;
import main.edu.uci.compiler.parser.Parser;
import org.junit.Test;
import test.parser.model.TokenizeException;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Created by srikrishna on 2/1/17.
 */
public class ParserTest {
    static int index = 1;
    @Test
    public void testParser() {
        String resourcePath = "resources/programs";
        boolean noExceptionOccurred = true;

//        try {
//            //To Test one code at a time
//            String fileName = resourcePath + "/test0" + generateProgramName() + ".txt";
////             String fileName = resourcePath + "/big.txt";
////             String fileName = resourcePath + "/cell.txt";
//            System.out.println("File name is " + fileName);
//            Parser parser = new Parser(fileName);
//            parser.computation();
//        } catch (IOException ex){
//            System.out.println("Exception is " + ex.getMessage());
//            noExceptionOccurred = false;
//        }
//        assertTrue(noExceptionOccurred);

        for (int i = 1; i <= 30; i++) {
            try {
                String fileName = resourcePath + "/test0" + generateProgramName() + ".txt";
                System.out.println("fileName - " + fileName);
                Parser parser = new Parser(fileName);
                parser.computation();
                noExceptionOccurred = true;
            } catch (IOException  ex) {
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
