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

/**
 * A wrapper around a line of code ({@link String}) containing additional info from the {@link Parser} that produced it.
 */
class LineOfCode {

    final String line;

    final boolean empty;
    final boolean comment;

    /**
     * Constructs a new {@link LineOfCode}.
     *
     * @param line    the actual line of code
     * @param comment whether the {@link Parser} considered this line to be part of a comment
     */
    LineOfCode(String line, boolean comment) {
        this.line = line;
        this.empty = Parser.emptyLine.matcher(line).matches();
        this.comment = comment;
    }

    /**
     * Returns the actual line of code.
     *
     * @return the line of code
     */
    String getLine() {
        return line;
    }

    /**
     * Returns whether this line is empty (consisting only of whitespace).
     *
     * @return whether the line is empty
     */
    public boolean isEmpty() {
        return empty;
    }

    /**
     * Returns whether this {@link LineOfCode} is considered to be part of a comment.
     *
     * @return whether the {@link LineOfCode} is commented out
     */
    boolean isComment() {
        return comment;
    }

    @Override
    public String toString() {
        return line;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LineOfCode that = (LineOfCode) o;

        if (comment != that.comment) return false;
        return line.equals(that.line);

    }

    @Override
    public int hashCode() {
        int result = line.hashCode();
        result = 31 * result + (comment ? 1 : 0);
        return result;
    }
}
