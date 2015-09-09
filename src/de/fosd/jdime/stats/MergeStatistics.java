package de.fosd.jdime.stats;

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

    public void add(MergeStatistics other) {
        float combinedSumSize = avgChunkSize * numChunks + other.avgChunkSize * other.numChunks;

        numChunks += other.numChunks;
        avgChunkSize = combinedSumSize / numChunks;

        maxASTDepth = Math.max(maxASTDepth, other.maxASTDepth);
        maxNumChildren = Math.max(maxNumChildren, other.maxNumChildren);
    }
}
