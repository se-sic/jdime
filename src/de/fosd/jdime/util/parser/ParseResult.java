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

import de.fosd.jdime.stats.CodeStatistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collector;
import java.util.stream.Collectors;

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
    private CodeStatistics stats;

    private String leftLabel = ConflictContent.DEFAULT_LABEL;
    private String rightLabel = ConflictContent.DEFAULT_LABEL;

    /**
     * Constructs an empty {@link ParseResult}.
     */
    public ParseResult() {
        this.statsHash = Optional.empty();
    }

    /**
     * Returns the summary statistics for all {@link Content} instances in this {@link ParseResult}.
     *
     * @return the summed up statistics
     */
    public CodeStatistics getStats() {
        int statsHash = hashCode();

        if (!this.statsHash.isPresent() || this.statsHash.get() != statsHash) {
            this.stats = stream().reduce(new CodeStatistics(), (cs, c) -> cs.add(c.getStats(), cs), CodeStatistics::add);
            this.statsHash = Optional.of(statsHash);
        }

        return this.stats;
    }

    /**
     * Sets the label for the left side of any {@link ConflictContent} in this {@link ParseResult}.
     *
     * @param leftLabel the label for the left side
     */
    public void setLeftLabel(String leftLabel) {
        this.leftLabel = leftLabel;
    }

    /**
     * Sets the label for the left side of any {@link ConflictContent} in this {@link ParseResult}.
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
        MergedContent lines;

        if (isEmpty()) {
            lines = new MergedContent();
            add(lines);
        } else {
            Content content = get(size() - 1);

            if (!content.isConflict()) {
                lines = (MergedContent) content;
            } else {
                lines = new MergedContent();
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
        ConflictContent conflict;

        if (isEmpty()) {
            conflict = new ConflictContent();
            add(conflict);
        } else {
            Content content = get(size() - 1);

            if (content.isConflict()) {
                conflict = (ConflictContent) content;
            } else {
                conflict = new ConflictContent();
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
     * {@inheritDoc}
     * <br><br>
     * The {@link ConflictContent Conflicts} in this {@link ParseResult} will be labeled using the labels set via
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
     * sides of any {@link ConflictContent} elements in this {@link ParseResult}.
     *
     * @param leftLabel  the label for the left side of any conflict in the {@link ParseResult}
     * @param rightLabel the label for the right side of any conflict in the {@link ParseResult}
     * @return a {@link String} representing this {@link ParseResult}
     */
    public String toString(String leftLabel, String rightLabel) {
        return stream().map(c -> c.toString(leftLabel, rightLabel)).collect(Collectors.joining(System.lineSeparator()));
    }
}
