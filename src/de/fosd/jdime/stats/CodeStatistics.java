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
package de.fosd.jdime.stats;

import de.fosd.jdime.strategy.MergeStrategy;
import de.fosd.jdime.util.parser.Parser;

import java.io.PrintStream;

/**
 * Statistics about (a segment of) Code produced by one of the {@link MergeStrategy MergeStrategies}. This is produced
 * by the {@link Parser}.
 */
public final class CodeStatistics {

    private int conflicts;

    private int linesOfCode;
    private int conflictingLinesOfCode;

    private int chars;
    private int conflictingChars;

    private int tokens;
    private int conflictingTokens;

    /**
     * Constructs a new, empty {@link CodeStatistics} instance.
     */
    public CodeStatistics() { }

    /**
     * Copies the given {@link CodeStatistics}.
     *
     * @param toCopy the {@link CodeStatistics} to copy
     */
    public CodeStatistics(CodeStatistics toCopy) {
        this.conflicts = toCopy.conflicts;

        this.linesOfCode = toCopy.linesOfCode;
        this.conflictingLinesOfCode = toCopy.conflictingLinesOfCode;

        this.chars = toCopy.chars;
        this.conflictingChars = toCopy.conflictingChars;

        this.tokens = toCopy.tokens;
        this.conflictingTokens = toCopy.conflictingTokens;
    }

    /**
     * Gets the number of conflicts.
     *
     * @return the number of conflicts
     */
    public int getConflicts() {
        return conflicts;
    }

    /**
     * Sets the number of conflicts.
     *
     * @param conflicts the number of conflicts
     */
    public void setConflicts(int conflicts) {
        this.conflicts = conflicts;
    }

    /**
     * Gets the number of lines of code.
     *
     * @return the the number of lines of code
     */
    public int getLinesOfCode() {
        return linesOfCode;
    }

    /**
     * Sets the number of lines of code.
     *
     * @param linesOfCode the number of lines of code
     */
    public void setLinesOfCode(int linesOfCode) {
        this.linesOfCode = linesOfCode;
    }

    /**
     * Gets the number of conflicting lines of code.
     *
     * @return the number of conflicting lines of code
     */
    public int getConflictingLinesOfCode() {
        return conflictingLinesOfCode;
    }

    /**
     * Sets the number of conflicting lines of code.
     *
     * @param conflictingLinesOfCode the number of conflicting lines of code
     */
    public void setConflictingLinesOfCode(int conflictingLinesOfCode) {
        this.conflictingLinesOfCode = conflictingLinesOfCode;
    }

    /**
     * Gets the number of chars.
     *
     * @return the number of chars
     */
    public int getChars() {
        return chars;
    }

    /**
     * Sets the number of chars.
     *
     * @param chars the number of chars
     */
    public void setChars(int chars) {
        this.chars = chars;
    }

    /**
     * Gets the number of conflicting chars.
     *
     * @return the number of conflicting chars
     */
    public int getConflictingChars() {
        return conflictingChars;
    }

    /**
     * Sets the number of conflicting chars.
     *
     * @param conflictingChars the number of conflicting chars
     */
    public void setConflictingChars(int conflictingChars) {
        this.conflictingChars = conflictingChars;
    }

    /**
     * Gets the number of tokens.
     *
     * @return the number of tokens
     */
    public int getTokens() {
        return tokens;
    }

    /**
     * Sets the number of tokens.
     *
     * @param tokens the tokens
     */
    public void setTokens(int tokens) {
        this.tokens = tokens;
    }

    /**
     * Gets the number of conflicting tokens.
     *
     * @return the number of conflicting tokens
     */
    public int getConflictingTokens() {
        return conflictingTokens;
    }

    /**
     * Sets the number of conflicting tokens.
     *
     * @param conflictingTokens the number of conflicting tokens
     */
    public void setConflictingTokens(int conflictingTokens) {
        this.conflictingTokens = conflictingTokens;
    }

    /**
     * Adds the given {@link CodeStatistics} to this {@link CodeStatistics} and returns the result in a new
     * {@link CodeStatistics} instance.
     *
     * @param other the {@link CodeStatistics} to add
     * @return the result (a new {@link CodeStatistics} instance
     */
    public CodeStatistics add(CodeStatistics other) {
        return add(other, new CodeStatistics());
    }

    /**
     * Adds the given {@link CodeStatistics} to this {@link CodeStatistics} and returns the result in the
     * {@link CodeStatistics} {@code result}. It is safe to use {@code other} here.
     *
     * @param other the {@link CodeStatistics} to add
     * @param result the {@link CodeStatistics} to store the result in
     * @return the {@link CodeStatistics} instance given via the second parameter containing the result
     */
    public CodeStatistics add(CodeStatistics other, CodeStatistics result) {
        result.setConflicts(conflicts + other.conflicts);

        result.setLinesOfCode(linesOfCode + other.linesOfCode);
        result.setConflictingLinesOfCode(conflictingLinesOfCode + other.conflictingLinesOfCode);

        result.setChars(chars + other.chars);
        result.setConflictingChars(conflictingChars + other.conflictingChars);

        result.setTokens(tokens + other.tokens);
        result.setConflictingTokens(conflictingTokens + other.conflictingTokens);

        return result;
    }

    /**
     * Writes a human readable representation of this {@link CodeStatistics} object to the given
     * <code>PrintStream</code>. Each line will be prepended by the given <code>indent</code>.
     *
     * @param ps
     *         the <code>PrintStream</code> to write to
     * @param indent
     *         the indentation to use
     */
    public void print(PrintStream ps, String indent) {
        ps.print(indent); ps.print("Conflicts:                 "); ps.println(conflicts);

        ps.print(indent); ps.print("Lines of Code:             "); ps.println(linesOfCode);
        ps.print(indent); ps.print("Conflicting Lines of Code: "); ps.println(conflictingLinesOfCode);

        ps.print(indent); ps.print("Chars:                     "); ps.println(chars);
        ps.print(indent); ps.print("Conflicting Chars:         "); ps.println(conflictingChars);

        ps.print(indent); ps.print("Tokens:                    "); ps.println(tokens);
        ps.print(indent); ps.print("Conflicting Tokens:        "); ps.println(conflictingTokens);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CodeStatistics that = (CodeStatistics) o;

        if (conflicts != that.conflicts) return false;
        if (linesOfCode != that.linesOfCode) return false;
        if (conflictingLinesOfCode != that.conflictingLinesOfCode) return false;
        if (chars != that.chars) return false;
        if (conflictingChars != that.conflictingChars) return false;
        if (tokens != that.tokens) return false;
        return conflictingTokens == that.conflictingTokens;
    }

    @Override
    public int hashCode() {
        int result = conflicts;
        result = 31 * result + linesOfCode;
        result = 31 * result + conflictingLinesOfCode;
        result = 31 * result + chars;
        result = 31 * result + conflictingChars;
        result = 31 * result + tokens;
        result = 31 * result + conflictingTokens;
        return result;
    }
}
