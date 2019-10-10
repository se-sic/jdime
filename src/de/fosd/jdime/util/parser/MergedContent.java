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
