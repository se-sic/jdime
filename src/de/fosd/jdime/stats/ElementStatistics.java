package de.fosd.jdime.stats;

public class ElementStatistics {

    private int total;
    private int numAdded;
    private int numMerged;
    private int numDeleted;
    private int numChanged;
    private int numOccurInConflict;

    public ElementStatistics() {
        this.total = 0;
        this.numAdded = 0;
        this.numMerged = 0;
        this.numDeleted = 0;
        this.numChanged = 0;
        this.numOccurInConflict = 0;
    }

    public void add(ElementStatistics other) {
        total += other.total;
        numAdded += other.numAdded;
        numMerged += other.numMerged;
        numDeleted += other.numDeleted;
        numChanged += other.numChanged;
        numOccurInConflict += other.numOccurInConflict;
    }

    public void incrementTotal(int by) {
        total += by;
    }

    public void incrementNumAdded(int by) {
        numAdded += by;
    }

    public void incrementNumMerged(int by) {
        numMerged += by;
    }

    public void incrementNumDeleted(int by) {
        numDeleted += by;
    }

    public void incrementNumChanged(int by) {
        numChanged += by;
    }

    public void incrementNumOccurInConflic(int by) {
        numOccurInConflict += by;
    }

    public void incrementTotal() {
        incrementTotal(1);
    }

    public void incrementNumAdded() {
        incrementNumAdded(1);
    }

    public void incrementNumMerged() {
        incrementNumMerged(1);
    }

    public void incrementNumDeleted() {
        incrementNumDeleted(1);
    }

    public void incrementNumChanged() {
        incrementNumChanged(1);
    }

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
}
