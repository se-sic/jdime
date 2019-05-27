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
import java.util.logging.Logger;

/**
 * The <code>Parser</code> generates a list of <code>Content</code> instances that represent the parts that the parsed
 * piece of code was split up into. A <code>Content</code> implementation may simply represent a list of lines
 * of code or a conflict that was found in the code.
 */
public abstract class Content {

    private static final Logger LOG = Logger.getLogger(Content.class.getCanonicalName());

    /**
     * A list of lines of code that were not part of a conflict.
     */
    public static class Merged extends Content {

        private List<String> lines;

        /**
         * Constructs a new <code>Merged</code> instance.
         */
        Merged() {
            super(false);
            this.lines = new ArrayList<>();
        }

        /**
         * Adds a line of code to this <code>Merged</code> instance.
         *
         * @param line
         *         the line to add
         */
        public void add(String line) {
            lines.add(line);
        }

        @Override
        public String toString() {
            return String.join(System.lineSeparator(), lines);
        }

        @Override
        public String toString(String leftLabel, String rightLabel) {
            return toString();
        }
    }

    /**
     * A two sided conflict.
     */
    public static class Conflict extends Content {

        public static final short MARKER_SIZE = 7;
        public static final String CONFLICT_START = new String(new char[MARKER_SIZE]).replace("\0", "<");
        public static final String CONFLICT_DELIM = new String(new char[MARKER_SIZE]).replace("\0", "=");
        public static final String CONFLICT_END = new String(new char[MARKER_SIZE]).replace("\0", ">");
        public static final String DEFAULT_LABEL = "UNLABELED";

        private List<String> leftLines;
        private List<String> rightLines;

        /**
         * This is true if the {@link Parser} determined that this {@link Conflict} consists only of lines
         * that are empty or comments / documentation. These lines are not included in the AST and as such can not
         * produce conflicts in structured strategies but may conflict in the linebased strategy. Including these
         * conflicts would therefore put the linebased strategy at a disadvantage.
         */
        private boolean filtered;

        /**
         * Constructs a new <code>Conflict</code> instance.
         */
        Conflict() {
            super(true);
            this.leftLines = new ArrayList<>();
            this.rightLines = new ArrayList<>();
        }

        /**
         * Adds the left and right side lines of the given <code>Conflict</code> to this <code>Conflict</code>s
         * left and right lines.
         *
         * @param other
         *         the <code>Conflict</code> to add
         */
        public void add(Conflict other) {
            leftLines.addAll(other.leftLines);
            rightLines.addAll(other.rightLines);
        }

        /**
         * Adds a line to the left side of this <code>Conflict</code>.
         *
         * @param line
         *         the line to add
         */
        public void addLeft(String line) {
            leftLines.add(line);
        }

        /**
         * Adds a line to the right side of this <code>Conflict</code>.
         *
         * @param line
         *         the line to add
         */
        public void addRight(String line) {
            rightLines.add(line);
        }

        /**
         * Returns true if the conflict is empty, i.e., both sides of the conflict are empty.
         *
         * @return true iff both sides of the conflict are empty
         */
        public boolean isEmpty() {
            return leftLines.isEmpty() && rightLines.isEmpty();
        }

        /**
         * Clears both sides of the conflict.
         */
        public void clear() {
            leftLines.clear();
            rightLines.clear();
        }

        /**
         * Returns whether this conflict was filtered by the {@link Parser}.
         *
         * @return whether this {@link Conflict} was filtered
         * @see #filtered
         */
        public boolean isFiltered() {
            return filtered;
        }

        /**
         * Filters this {@link Conflict}.
         *
         * @see #filtered
         */
        public void setFiltered() {
            setFiltered(true);
        }

        /**
         * Sets whether this {@link Conflict} was filtered.
         *
         * @param filtered whether the {@link Conflict} is filtered
         * @see #filtered
         */
        public void setFiltered(boolean filtered) {
            this.filtered = filtered;
        }

        @Override
        public String toString() {
            return toString(DEFAULT_LABEL, DEFAULT_LABEL);
        }

        @Override
        public String toString(String leftLabel, String rightLabel) {
            String ls = System.lineSeparator();
            StringBuilder b = new StringBuilder();

            b.append(CONFLICT_START).append(" ").append(leftLabel).append(ls);
            if (!leftLines.isEmpty()) {
                b.append(String.join(ls, leftLines)).append(ls);
            }
            b.append(CONFLICT_DELIM).append(ls);
            if (!rightLines.isEmpty()) {
                b.append(String.join(ls, rightLines)).append(ls);
            }
            b.append(CONFLICT_END).append(" ").append(rightLabel);

            return b.toString();
        }
    }

    private final boolean isConflict;

    /**
     * Constructs a new <code>Content</code> piece.
     *
     * @param isConflict
     *         whether this <code>Content</code> is a <code>Conflict</code>
     */
    private Content(boolean isConflict) {
        this.isConflict = isConflict;
    }

    /**
     * Returns whether this <code>Content</code> piece is a <code>Conflict</code>.
     *
     * @return true iff this is a <code>Conflict</code>
     */
    public boolean isConflict() {
        return isConflict;
    }

    @Override
    public abstract String toString();

    /**
     * Returns a {@link String} representation of this piece of {@link Content}. The labels will
     * be used by {@link Conflict} elements to identify their sides.
     *
     * @param leftLabel  the label for the left side of a {@link Conflict}
     * @param rightLabel the label for the right side of a {@link Conflict}
     * @return a <code>String</code> representing this piece of <code>Content</code>
     */
    public abstract String toString(String leftLabel, String rightLabel);
}
