package test.parser;

import main.edu.uci.compiler.model.BasicBlock;
import main.edu.uci.compiler.model.DominatorTree;
import main.edu.uci.compiler.parser.Parser;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Created by srikrishna on 2/22/17.
 */
public class DominatorRelationships {
    @Test
    public void testDominanceRelationships() {
        String resourcePath = "resources/programs";
        boolean noExceptionOccurred = true;

        try {
            //To Test one code at a time
            String fileName = resourcePath + "/test010.txt";
//             String fileName = resourcePath + "/big.txt";
//             String fileName = resourcePath + "/cell.txt";
            System.out.println("File name is " + fileName);
            Parser parser = new Parser(fileName);
            parser.computation();
            BasicBlock startBasicBlock = parser.getStartBasicBlock();
            List<BasicBlock> listOfAllBasicBlocks = startBasicBlock.getListOfAllBasicBlocks();
            System.out.println("list of basic blocks is " + listOfAllBasicBlocks.size());
            DominatorTree tree = new DominatorTree(listOfAllBasicBlocks, startBasicBlock);
            tree.printDominatorRelationships();
        } catch (IOException ex) {
            System.out.println("Exception is " + ex.getMessage());
            noExceptionOccurred = false;
        }
        assertTrue(noExceptionOccurred);
    }
}
