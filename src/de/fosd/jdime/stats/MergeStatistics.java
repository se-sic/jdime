package de.fosd.jdime.stats;

public class MergeStatistics {

    /**
     * A chunk is a group of continuous changes occurring one after another.
     */
    private int numChunks;
    private int avgChunkSize;

    private int astDepth;

    /**
     * The maximum number of children a node of the AST has.
     */
    private int maxNumChildren;
}
