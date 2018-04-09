/**
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2017 University of Passau, Germany
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
        public String toString(String fstId, String... ids) {
            return toString();
        }
    }

    /**
     * A two sided conflict.
     */
    public static class Conflict extends Content {

        public static final String CONFLICT_START = "<<<<<<<";
        public static final String CONFLICT_DELIM = "=======";
        public static final String CONFLICT_END = ">>>>>>>";

        private List<String> leftLines;
        private List<String> rightLines;

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

        @Override
        public String toString() {
            String ls = System.lineSeparator();
            StringBuilder b = new StringBuilder();

            b.append(CONFLICT_START).append(ls);
            if (!leftLines.isEmpty()) {
                b.append(String.join(ls, leftLines)).append(ls);
            }
            b.append(CONFLICT_DELIM).append(ls);
            if (!rightLines.isEmpty()) {
                b.append(String.join(ls, rightLines)).append(ls);
            }
            b.append(CONFLICT_END);

            return b.toString();
        }

        @Override
        public String toString(String fstId, String... ids) {

            if (fstId != null && ids == null || ids.length < 1) {
                LOG.warning("Insufficient identifiers for constructing a detailed conflict representation.");
                return toString();
            }

            StringBuilder b = new StringBuilder();
            String ls = System.lineSeparator();

            b.append(CONFLICT_START).append(" ").append(fstId).append(ls);
            if (!leftLines.isEmpty()) {
                b.append(String.join(ls, leftLines)).append(ls);
            }
            b.append(CONFLICT_DELIM).append(ls);
            if (!rightLines.isEmpty()) {
                b.append(String.join(ls, rightLines)).append(ls);
            }
            b.append(CONFLICT_END).append(" ").append(ids[0]);

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
     * Returns a <code>String</code> representation of this piece of <code>Content</code>. The identifiers will
     * be used by the implementations to identify their parts (for example <code>Conflict</code> will use the first
     * two identifiers for marking the two sides of the conflict).
     *
     * @param fstId
     *         to first identifier to use
     * @param ids
     *         the other identifiers to use
     * @return a <code>String</code> representing this piece of <code>Content</code>
     */
    public abstract String toString(String fstId, String... ids);
}
