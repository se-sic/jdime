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

    public MergeStatistics() {
        this.numChunks = 0;
        this.avgChunkSize = 0;
        this.maxASTDepth = 0;
        this.maxNumChildren = 0;
    }

    public int getNumChunks() {
        return numChunks;
    }

    public void setNumChunks(int numChunks) {
        this.numChunks = numChunks;
    }

    public float getAvgChunkSize() {
        return avgChunkSize;
    }

    public void setAvgChunkSize(float avgChunkSize) {
        this.avgChunkSize = avgChunkSize;
    }

    public int getMaxASTDepth() {
        return maxASTDepth;
    }

    public void setMaxASTDepth(int maxASTDepth) {
        this.maxASTDepth = maxASTDepth;
    }

    public int getMaxNumChildren() {
        return maxNumChildren;
    }

    public void setMaxNumChildren(int maxNumChildren) {
        this.maxNumChildren = maxNumChildren;
    }

    public void add(MergeStatistics other) {
        float combinedSumSize = avgChunkSize * numChunks + other.avgChunkSize * other.numChunks;

        numChunks += other.numChunks;
        avgChunkSize = combinedSumSize / numChunks;

        maxASTDepth = Math.max(maxASTDepth, other.maxASTDepth);
        maxNumChildren = Math.max(maxNumChildren, other.maxNumChildren);
    }
}
