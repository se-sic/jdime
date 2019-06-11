package de.fosd.jdime.util.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A list of lines of code that were not part of a conflict.
 */
public class MergedContent extends Content {

    private List<LineOfCode> lines;

    /**
     * Constructs a new <code>Merged</code> instance.
     */
    MergedContent() {
        super(false);
        this.lines = new ArrayList<>();
    }

    /**
     * Adds a line of code to this <code>Merged</code> instance.
     *
     * @param line    the line to add
     * @param comment whether the line is part of a comment
     */
    public void add(String line, boolean comment) {
        lines.add(new LineOfCode(line, comment));
    }

    /**
     * Returns an unmodifiable view of the lines of this merged part of code.
     *
     * @return the lines
     */
    public List<LineOfCode> getLines() {
        return Collections.unmodifiableList(lines);
    }

    @Override
    public String toString() {
        return lines.stream().map(LineOfCode::getLine).collect(Collectors.joining(System.lineSeparator()));
    }

    @Override
    public String toString(String leftLabel, String rightLabel) {
        return toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MergedContent merged = (MergedContent) o;
        return lines.equals(merged.lines);
    }

    @Override
    public int hashCode() {
        return lines.hashCode();
    }
}
