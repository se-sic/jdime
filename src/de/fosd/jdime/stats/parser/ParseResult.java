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

    /**
     * Returns a <code>String</code> representation of this <code>ParseResult</code>. The identifiers will
     * be used by the <code>Content</code> implementations to identify their parts (for example <code>Conflict</code>
     * will use the first two identifiers for marking the two sides of the conflict).
     *
     * @param fstId
     *         the first identifier to use
     * @param ids
     *         the other identifiers to use
     * @return a <code>String</code> representing this <code>ParseResult</code>
     */
    public String toString(String fstId, String... ids) {
        return String.join(System.lineSeparator(), stream().map(c -> c.toString(fstId, ids)).collect(Collectors.toList()));
    }
}
