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

import java.util.Optional;

/**
 * The <code>Parser</code> generates a list of <code>Content</code> instances that represent the parts that the parsed
 * piece of code was split up into. A <code>Content</code> implementation may simply represent a list of lines
 * of code or a conflict that was found in the code.
 */
public abstract class Content {

    private final boolean isConflict;

    private Optional<Integer> statsHash;
    private CodeStatistics stats;

    /**
     * Constructs a new <code>Content</code> piece.
     *
     * @param isConflict
     *         whether this <code>Content</code> is a <code>Conflict</code>
     */
    Content(boolean isConflict) {
        this.isConflict = isConflict;
        this.statsHash = Optional.empty();
    }

    /**
     * Returns whether this <code>Content</code> piece is a <code>Conflict</code>.
     *
     * @return true iff this is a <code>Conflict</code>
     */
    public boolean isConflict() {
        return isConflict;
    }

    /**
     * Returns the {@link CodeStatistics} accumulated over the lines that are part of this {@link Content}.
     *
     * @return the {@link CodeStatistics} for this {@link Content}
     */
    public CodeStatistics getStats() {
        int statsHash = hashCode();

        if (!this.statsHash.isPresent() || this.statsHash.get() != statsHash) {
            this.stats = Parser.calcStats(this);
            this.statsHash = Optional.of(statsHash);
        }

        return this.stats;
    }

    @Override
    public abstract String toString();

    /**
     * Returns a {@link String} representation of this piece of {@link Content}. The labels will
     * be used by {@link ConflictContent} elements to identify their sides.
     *
     * @param leftLabel  the label for the left side of a {@link ConflictContent}
     * @param rightLabel the label for the right side of a {@link ConflictContent}
     * @return a <code>String</code> representing this piece of <code>Content</code>
     */
    public abstract String toString(String leftLabel, String rightLabel);

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);
}
