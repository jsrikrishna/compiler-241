
package main.edu.uci.compiler.model;


import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;




public class DepthFirstPreorderIterator implements Iterator<BasicBlock>
{
    /**
     * @param roots the caller's root(s) of the flowgraph.
     * There should be only one start block, but multiple roots
     * are tolerated to work around fuzzy successor logic to 
     * exception handlers.
     */
    public DepthFirstPreorderIterator(Collection<? extends BasicBlock> roots)
    {
        this.toDo.addAll(roots);
    }

    /**
     * The to-be-visited stack of blocks.
     */
    Stack<BasicBlock> toDo = new Stack<BasicBlock>();

    /**
     *  The set of edges already traversed.
     */
    Set<Edge> visitedEdges = new HashSet<Edge>();


    @Override
    public boolean hasNext()
    {
        return !toDo.isEmpty();
    }

    @Override
    public BasicBlock next()
    {
        if (!hasNext())
            throw new NoSuchElementException();

        BasicBlock next = toDo.pop();
        pushSuccessors(next);
        return next;
    }

    /**
     * Traverse any previously-untraversed edges
     * by adding the destination block to the to-do stack.
     * @param b the current block.
     */
    private void pushSuccessors(BasicBlock b)
    {
        for (BasicBlock succ_block : b.getChildren())
            if (visitedEdges.add(new Edge(b, succ_block)))
                toDo.push(succ_block);
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Edge is used to detect edges previously traversed.
     * It implements composite hash and equality operations
     * so it can be used as a key in a hashed collection.
     */
    private static class Edge
    {
        private BasicBlock from;
        private BasicBlock to;

        Edge(BasicBlock from, BasicBlock to)
        {
            this.from = from;
            this.to = to;
        }

        private static final int PRIME_MULTIPLIER = 7057;

        /**
         * Generate a composite hash code so that an Edge can be
         * used in a hashed container.
         * @return the composite hash code of the from/to vertices.
         */
        @Override
        public int hashCode()
        {
            return (from.hashCode() * PRIME_MULTIPLIER) + to.hashCode();
        }

        /**
         * Use the vertices to determine equality of an Edge so it
         * can be used in a hashed container.
         * @param other the other object to compare.
         * @return true iff other is an Edge, and both Edges' from/to
         * vertices match their corresponding field.
         */
        @Override
        public boolean equals(Object other)
        {
            if (other == this)
            {
                return true;
            }
            else if (other instanceof Edge)
            {
                Edge otherEdge = (Edge)other;
                return from == otherEdge.from && to == otherEdge.to;
            }
            return false;
        }
    }
}