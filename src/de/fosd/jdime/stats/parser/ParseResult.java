package de.fosd.jdime.stats.parser;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class ParseResult extends ArrayList<Content> {

    private int linesOfCode;
    private int conflicts;
    private int conflictingLinesOfCode;

    public int getLinesOfCode() {
        return linesOfCode;
    }

    public void setLinesOfCode(int mergedLinesOfCode) {
        this.linesOfCode = mergedLinesOfCode;
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

    public void addMergedLine(String line) {
        Content.Merged lines;

        if (isEmpty()) {
            lines = new Content.Merged();
            add(lines);
        } else {
            Content content = get(size() - 1);

            if (!content.isConflict) {
                lines = (Content.Merged) content;
            } else {
                lines = new Content.Merged();
                add(lines);
            }
        }

        lines.add(line);
    }

    public void addConflictingLine(String line, boolean left) {
        Content.Conflict conflict;

        if (isEmpty()) {
            conflict = new Content.Conflict();
            add(conflict);
        } else {
            Content content = get(size() - 1);

            if (content.isConflict) {
                conflict = (Content.Conflict) content;
            } else {
                conflict = new Content.Conflict();
                add(conflict);
            }
        }

        if (left) {
            conflict.addLeft(line);
        } else {
            conflict.addRight(line);
        }
    }

    @Override
    public String toString() {
        return String.join(System.lineSeparator(), stream().map(Content::toString).collect(Collectors.toList()));
    }
}
