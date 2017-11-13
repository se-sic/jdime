/**
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2017 University of Passau, Germany
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 *
 * Contributors:
 *     Olaf Lessenich <lessenic@fim.uni-passau.de>
 *     Georg Seibt <seibt@fim.uni-passau.de>
 */
package de.fosd.jdime.stats;

import java.io.PrintStream;

/**
 * A statistics container for general statistics about the trees involved in a merge.
 */
public class MergeStatistics {

    /**
     * A chunk is a group of continuous changes occurring one after another.
     */
    private int numChunks;
    private float avgChunkSize;

    private int maxASTDepth;

    /**
     * The maximum number of children a node of the AST has.
     */
    private int maxNumChildren;

    /**
     * Constructs a new <code>MergeStatistics</code> object.
     */
    public MergeStatistics() {
        this.numChunks = 0;
        this.avgChunkSize = 0;
        this.maxASTDepth = 0;
        this.maxNumChildren = 0;
    }

    /**
     * Copy constructor.
     *
     * @param toCopy
     *         the <code>MergeStatistics</code> to copy
     */
    public MergeStatistics(MergeStatistics toCopy) {
        this.numChunks = toCopy.numChunks;
        this.avgChunkSize = toCopy.avgChunkSize;
        this.maxASTDepth = toCopy.maxASTDepth;
        this.maxNumChildren = toCopy.maxNumChildren;
    }

    /**
     * Returns the number of chunks.
     *
     * @return the number of chunks
     */
    public int getNumChunks() {
        return numChunks;
    }

    /**
     * Sets the number of chunks to the new value.
     *
     * @param numChunks
     *         the new number of chunks
     */
    public void setNumChunks(int numChunks) {
        this.numChunks = numChunks;
    }

    /**
     * Returns the average size of the chunks.
     *
     * @return the average size of the chunks
     */
    public float getAvgChunkSize() {
        return avgChunkSize;
    }

    /**
     * Sets the average size of the chunks to the new value
     *
     * @param avgChunkSize
     *         the new average size of the chunks
     */
    public void setAvgChunkSize(float avgChunkSize) {
        this.avgChunkSize = avgChunkSize;
    }

    /**
     * Returns the maximum depth of the AST.
     *
     * @return the maximum depth of the AST
     */
    public int getMaxASTDepth() {
        return maxASTDepth;
    }

    /**
     * Sets the maximum depth of the AST to the new value.
     *
     * @param maxASTDepth
     *         the new maximum AST depth
     */
    public void setMaxASTDepth(int maxASTDepth) {
        this.maxASTDepth = maxASTDepth;
    }

    /**
     * Returns the number of chunks.
     *
     * @return the number of chunks
     */
    public int getMaxNumChildren() {
        return maxNumChildren;
    }

    /**
     * Sets the maximum number of children to the new value.
     *
     * @param maxNumChildren
     *         the new maximum number of children
     */
    public void setMaxNumChildren(int maxNumChildren) {
        this.maxNumChildren = maxNumChildren;
    }

    /**
     * Adds the values in the given <code>MergeStatistics</code> to <code>this</code>. The <code>avgChunkSize</code>
     * will be recalculated, <code>maxASTDepth</code> and <code>maxNumChildren</code> will be determined using
     * {@link Math#max(double, double)}.
     *
     * @param other
     *         the <code>MergeStatistics</code> to add
     */
    public void add(MergeStatistics other) {
        float combinedSumSize = avgChunkSize * numChunks + other.avgChunkSize * other.numChunks;

        numChunks += other.numChunks;
        avgChunkSize = combinedSumSize / numChunks;

        maxASTDepth = Math.max(maxASTDepth, other.maxASTDepth);
        maxNumChildren = Math.max(maxNumChildren, other.maxNumChildren);
    }

    /**
     * Writes a human readable representation of this <code>MergeStatistics</code> object to the given
     * <code>PrintStream</code>. Each line will be prepended by the given <code>indent</code>.
     *
     * @param ps
     *         the <code>PrintStream</code> to write to
     * @param indent
     *         the indentation to use
     */
    public void print(PrintStream ps, String indent) {
        ps.print(indent); ps.print("Chunks:                  "); ps.println(numChunks);
        ps.print(indent); ps.print("Avg. Chunk Size:         "); ps.println(avgChunkSize);
        ps.print(indent); ps.print("Max. Tree Depth:         "); ps.println(maxASTDepth);
        ps.print(indent); ps.print("Max. Number of Children: "); ps.println(maxNumChildren);
    }
}
