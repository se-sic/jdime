package de.fosd.jdime.stats;

public class ElementStatistics {

    private int total;
    private int numAdded;
    private int numDeleted;
    private int numChanged;
    private int numOccurInConflict;

    public ElementStatistics() {
        this.total = 0;
        this.numAdded = 0;
        this.numDeleted = 0;
        this.numChanged = 0;
        this.numOccurInConflict = 0;
    }
}
