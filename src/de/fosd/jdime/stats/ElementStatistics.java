package de.fosd.jdime.stats;

import java.io.PrintStream;

public class ElementStatistics {

    private int total;
    private int numAdded;
    private int numMerged;
    private int numMatched;
    private int numDeleted;
    private int numChanged;
    private int numOccurInConflict;

    public ElementStatistics() {
        this.total = 0;
        this.numAdded = 0;
        this.numMerged = 0;
        this.numMatched = 0;
        this.numDeleted = 0;
        this.numChanged = 0;
        this.numOccurInConflict = 0;
    }

    public void add(ElementStatistics other) {
        total += other.total;
        numAdded += other.numAdded;
        numMerged += other.numMerged;
        numMatched += other.numMatched;
        numDeleted += other.numDeleted;
        numChanged += other.numChanged;
        numOccurInConflict += other.numOccurInConflict;
    }

    /**
     * Increments the total by the given amount.
     *
     * @param by the amount to add
     */
    public void incrementTotal(int by) {
        total += by;
    }

    /**
     * Increments the added count by the given amount.
     *
     * @param by the amount to add
     */
    public void incrementNumAdded(int by) {
        numAdded += by;
    }

    /**
     * Increments the merged count by the given amount.
     *
     * @param by the amount to add
     */
    public void incrementNumMerged(int by) {
        numMerged += by;
    }

    /**
     * Increments the matched count by the given amount.
     *
     * @param by the amount to add
     */
    public void incrementNumMatched(int by) {
        numMatched += by;
    }

    /**
     * Increments the deleted count by the given amount.
     *
     * @param by the amount to add
     */
    public void incrementNumDeleted(int by) {
        numDeleted += by;
    }

    /**
     * Increments the changed count by the given amount.
     *
     * @param by the amount to add
     */
    public void incrementNumChanged(int by) {
        numChanged += by;
    }

    /**
     * Increments the occur in conflict count by the given amount.
     *
     * @param by the amount to add
     */
    public void incrementNumOccurInConflic(int by) {
        numOccurInConflict += by;
    }

    /**
     * Increments the total by 1.
     */
    public void incrementTotal() {
        incrementTotal(1);
    }

    /**
     * Increments the added count by 1.
     */
    public void incrementNumAdded() {
        incrementNumAdded(1);
    }

    /**
     * Increments the merged count by 1.
     */
    public void incrementNumMerged() {
        incrementNumMerged(1);
    }

    /**
     * Increments the matched count by 1.
     */
    public void incrementNumMatched() {
        incrementNumMatched(1);
    }

    /**
     * Increments the deleted count by 1.
     */
    public void incrementNumDeleted() {
        incrementNumDeleted(1);
    }

    /**
     * Increments the changed count by 1.
     */
    public void incrementNumChanged() {
        incrementNumChanged(1);
    }

    /**
     * Increments the occur in conflict count by 1.
     */
    public void incrementNumOccurInConflic() {
        incrementNumOccurInConflic(1);
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getNumAdded() {
        return numAdded;
    }

    public void setNumAdded(int numAdded) {
        this.numAdded = numAdded;
    }

    public int getNumMerged() {
        return numMerged;
    }

    public void setNumMerged(int numMerged) {
        this.numMerged = numMerged;
    }

    public int getNumMatched() {
        return numMatched;
    }

    public void setNumMatched(int numMatched) {
        this.numMatched = numMatched;
    }

    public int getNumDeleted() {
        return numDeleted;
    }

    public void setNumDeleted(int numDeleted) {
        this.numDeleted = numDeleted;
    }

    public int getNumChanged() {
        return numChanged;
    }

    public void setNumChanged(int numChanged) {
        this.numChanged = numChanged;
    }

    public int getNumOccurInConflict() {
        return numOccurInConflict;
    }

    public void setNumOccurInConflict(int numOccurInConflict) {
        this.numOccurInConflict = numOccurInConflict;
    }

    public void print(PrintStream os, String indent) {
        os.print(indent); os.print("Total:      "); os.println(total);
        os.print(indent); os.print("Added:      "); os.println(numAdded);
        os.print(indent); os.print("Merged:     "); os.println(numMerged);
        os.print(indent); os.print("Matched:    "); os.println(numMatched);
        os.print(indent); os.print("Deleted:    "); os.println(numDeleted);
        os.print(indent); os.print("Changed:    "); os.println(numChanged);
        os.print(indent); os.print("InConflict: "); os.println(numOccurInConflict);
    }
}
