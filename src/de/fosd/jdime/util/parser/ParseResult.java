/**
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2018 University of Passau, Germany
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 *
 * Contributors:
 *     Olaf Lessenich <lessenic@fim.uni-passau.de>
 *     Georg Seibt <seibt@fim.uni-passau.de>
 */
package de.fosd.jdime.util.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static de.fosd.jdime.util.parser.Content.*;

/**
 * A <code>List</code> of <code>Content</code> instances resulting from a run of {@link Parser#parse(String)}.
 * In addition to the pieces of content the lines of code, the number of lines of code in conflicts and the number
 * of conflicts are stored. The {@link #toString()} and {@link #toString(String, String)} methods produce the
 * concatenation of the <code>String</code> representations of the <code>Content</code> pieces.
 */
public class ParseResult extends ArrayList<Content> {

    private static final Logger LOG = Logger.getLogger(ParseResult.class.getCanonicalName());
    private static final long serialVersionUID = 1L;

    /**
     * The last {@link #hashCode()} for which the statistics below were calculated. This is used for lazy
     * (re)initialization of these attributes.
     */
    private Optional<Integer> statsHash;

    private int conflicts;

    private int linesOfCode;
    private int conflictingLinesOfCode;

    private int chars;
    private int conflictingChars;

    private int tokens;
    private int conflictingTokens;

    private String leftLabel = Conflict.DEFAULT_LABEL;
    private String rightLabel = Conflict.DEFAULT_LABEL;

    public ParseResult() {
        this.statsHash = Optional.empty();
    }

    /**
     * Returns the number of conflicts.
     *
     * @return the number of conflicts
     */
    public int getConflicts() {
        calc();
        return conflicts;
    }

    /**
     * Returns the lines of code.
     *
     * @return the lines of code
     */
    public int getLinesOfCode() {
        calc();
        return linesOfCode;
    }

    /**
     * Returns the conflicting lines of code.
     *
     * @return the conflicting lines of code
     */
    public int getConflictingLinesOfCode() {
        calc();
        return conflictingLinesOfCode;
    }

    /**
     * Returns the number of non-whitespace characters.
     *
     * @return the number of characters
     */
    public int getChars() {
        calc();
        return chars;
    }

    /**
     * Returns the number of non-whitespace characters in conflict.
     *
     * @return the number of non-whitespace characters in conflict
     */
    public int getConflictingChars() {
        calc();
        return conflictingChars;
    }

    /**
     * Returns the number of tokens.
     *
     * @return the number of tokens
     */
    public int getTokens() {
        calc();
        return tokens;
    }

    /**
     * Returns the number of tokens in conflict.
     *
     * @return the number of tokens in conflict
     */
    public int getConflictingTokens() {
        calc();
        return conflictingTokens;
    }

    /**
     * Sets the label for the left side of any {@link Conflict} in this {@link ParseResult}.
     *
     * @param leftLabel the label for the left side
     */
    public void setLeftLabel(String leftLabel) {
        this.leftLabel = leftLabel;
    }

    /**
     * Sets the label for the left side of any {@link Conflict} in this {@link ParseResult}.
     *
     * @param rightLabel the label for the right side
     */
    public void setRightLabel(String rightLabel) {
        this.rightLabel = rightLabel;
    }

    /**
     * Adds a merged line (one that is not in a conflict) to the <code>ParseResult</code>.
     *
     * @param line
     *         the line to add
     */
    public void addMergedLine(String line, boolean comment) {
        Merged lines;

        if (isEmpty()) {
            lines = new Merged();
            add(lines);
        } else {
            Content content = get(size() - 1);

            if (!content.isConflict()) {
                lines = (Merged) content;
            } else {
                lines = new Merged();
                add(lines);
            }
        }

        lines.add(line, comment);
    }

    /**
     * Adds a line that is part of a conflict to the <code>ParseResult</code>.
     *
     * @param line    the line to add
     * @param left    true iff the line is part of the left side of the conflict (otherwise it is part of the right side)
     * @param comment whether the line is part of a comment
     */
    public void addConflictingLine(String line, boolean left, boolean comment) {
        Conflict conflict;

        if (isEmpty()) {
            conflict = new Conflict();
            add(conflict);
        } else {
            Content content = get(size() - 1);

            if (content.isConflict()) {
                conflict = (Conflict) content;
            } else {
                conflict = new Conflict();
                add(conflict);
            }
        }

        if (left) {
            conflict.addLeft(line, comment);
        } else {
            conflict.addRight(line, comment);
        }
    }

    /**
     * Calculates the values of the statistics field if necessary.
     */
    private void calc() {
        int statsHash = hashCode();

        if (this.statsHash.isPresent() && this.statsHash.get() == statsHash) {
            return;
        }

        int conflicts = 0;

        int linesOfCode = 0;
        int conflictingLinesOfCode = 0;

        int chars = 0;
        int conflictingChars = 0;

        int tokens = 0;
        int conflictingTokens = 0;


        for (Content part : this) {
            List<LineOfCode> lines = new ArrayList<>();

            if (part.isConflict()) {
                Conflict conflict = (Conflict) part;

                if (!conflict.isFiltered()) {
                    conflicts += 1;
                }

                lines.addAll(conflict.getLeftLines());
                lines.addAll(conflict.getRightLines());
            } else {
                Merged merged = (Merged) part;

                lines.addAll(merged.getLines());
            }

            for (LineOfCode line : lines) {

                if (line.empty || line.comment) {
                    continue;
                }

                linesOfCode += 1;

                // We only count non-whitespace characters to normalize the results over linebased/structured.
                int dChars = Parser.whitespace.matcher(line.line).replaceAll("").length();
                chars += dChars;

                int dTokens = 0;

                try {
                    dTokens = Parser.getTokenCount(line.line);
                } catch (beaver.Scanner.Exception e) {
                    LOG.log(Level.WARNING, e, () -> "Exception while parsing line '" + line + "' " +
                            "to count its tokens. ParseResult will record 0 tokens for the line.");
                }

                tokens += dTokens;

                if (part.isConflict()) {
                    conflictingLinesOfCode += 1;
                    conflictingChars += dChars;
                    conflictingTokens += dTokens;
                }
            }
        }

        this.statsHash = Optional.of(statsHash);

        this.conflicts = conflicts;

        this.linesOfCode = linesOfCode;
        this.conflictingLinesOfCode = conflictingLinesOfCode;

        this.chars = chars;
        this.conflictingChars = conflictingChars;

        this.tokens = tokens;
        this.conflictingTokens = conflictingTokens;
    }

    /**
     * {@inheritDoc}
     * <br><br>
     * The {@link Conflict Conflicts} in this {@link ParseResult} will be labeled using the labels set via
     * {@link #setLeftLabel(String)} and {@link #setRightLabel(String)}.
     *
     * @see Content#toString(String, String)
     */
    @Override
    public String toString() {
        return toString(leftLabel, rightLabel);
    }

    /**
     * Returns a {@link String} representation of this {@link ParseResult}. The given labels will be applied to the
     * sides of any {@link Conflict} elements in this {@link ParseResult}.
     *
     * @param leftLabel  the label for the left side of any conflict in the {@link ParseResult}
     * @param rightLabel the label for the right side of any conflict in the {@link ParseResult}
     * @return a {@link String} representing this {@link ParseResult}
     */
    public String toString(String leftLabel, String rightLabel) {
        return stream().map(c -> c.toString(leftLabel, rightLabel)).collect(Collectors.joining(System.lineSeparator()));
    }
}
