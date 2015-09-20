package de.fosd.jdime.stats.parser;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class ParseResult extends ArrayList<Content> {

    private int mergedLinesOfCode;
    private int conflicts;
    private int conflictingLinesOfCode;

    public int getMergedLinesOfCode() {
        return mergedLinesOfCode;
    }

    public void setMergedLinesOfCode(int mergedLinesOfCode) {
        this.mergedLinesOfCode = mergedLinesOfCode;
    }

    public int getConflicts() {
        return conflicts;
    }

    public void setConflicts(int conflicts) {
        this.conflicts = conflicts;
    }

    public int getConflictingLinesOfCode() {
        return conflictingLinesOfCode;
    }

    public void setConflictingLinesOfCode(int conflictingLinesOfCode) {
        this.conflictingLinesOfCode = conflictingLinesOfCode;
    }

    @Override
    public String toString() {
        return String.join(System.lineSeparator(), stream().map(Content::toString).collect(Collectors.toList()));
    }
}
