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
package de.fosd.jdime.stats.parser;

import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Contains methods for parsing code (possibly containing conflict markers) resulting from a merge.
 */
public final class Parser {

    private static final Pattern conflictStart = Pattern.compile("<<<<<<<.*");
    private static final Pattern conflictSep = Pattern.compile("=======");
    private static final Pattern conflictEnd = Pattern.compile(">>>>>>>.*");
    private static final Pattern emptyLine = Pattern.compile("\\s*");

    private static final Pattern lineComment = Pattern.compile("\\s*//.*");
    private static final Pattern blockComment1Line = Pattern.compile("\\s*/\\*.*?\\*/\\s*");
    private static final Pattern blockCommentStart = Pattern.compile("\\s*/\\*.*");
    private static final Pattern blockCommentEnd = Pattern.compile(".*?\\*/");

    /**
     * Utility class.
     */
    private Parser() {}

    /**
     * Parses the given code to a list of {@link Content} objects and counts the merged and conflicting lines
     * and the number of conflicts. Comments and conflicts consisting only of commented out lines will be ignored.
     *
     * @param code
     *         the piece of code to be parsed
     * @return the parse result
     */
    public static ParseResult parse(String code) {
        Scanner s = new Scanner(code);
        ParseResult res = new ParseResult();

        int linesOfCode = 0;
        int conflicts = 0;
        int conflictingLinesOfCode = 0;
        int clocBeforeConflict = 0; // cloc = conflicting lines of code

        boolean inConflict = false;
        boolean inLeftComment = false; // whether we were in a comment when the left part of the conflict started
        boolean inLeft = true;
        boolean inComment = false;

        while (s.hasNextLine()) {
            String line = s.nextLine();
            boolean wasConflictMarker = false;

            if (!matches(emptyLine, line) && !matches(lineComment, line)) {
                if (matches(blockCommentStart, line)) {

                    if (!matches(blockComment1Line, line)) {
                        inComment = true;
                    }
                } else if (matches(blockCommentEnd, line)) {

                    inComment = false;
                } else if (matches(conflictStart, line)) {

                    wasConflictMarker = true;
                    inConflict = true;
                    inLeftComment = inComment;
                    inLeft = true;
                    clocBeforeConflict = conflictingLinesOfCode;
                    conflicts++;
                } else if (matches(conflictSep, line)) {

                    wasConflictMarker = true;
                    inComment = inLeftComment;
                    inLeft = false;
                } else if (matches(conflictEnd, line)) {

                    wasConflictMarker = true;
                    inConflict = false;
                    if (clocBeforeConflict == conflictingLinesOfCode) {
                        conflicts--; // the conflict only contained empty lines and comments
                    }
                } else {

                    if (!inComment) {
                        linesOfCode++;

                        if (inConflict) {
                            conflictingLinesOfCode++;
                        }
                    }
                }
            }

            if (!wasConflictMarker) {
                if (inConflict) {
                    res.addConflictingLine(line, inLeft);
                } else {
                    res.addMergedLine(line);
                }
            }
        }

        res.setLinesOfCode(linesOfCode);
        res.setConflicts(conflicts);
        res.setConflictingLinesOfCode(conflictingLinesOfCode);

        return res;
    }

    /**
     * Returns whether the given <code>Pattern</code> matches the <code>line</code>.
     *
     * @param p
     *         the <code>Pattern</code> to use
     * @param line
     *         the line to match against the pattern
     * @return true iff the <code>Pattern</code> matched
     */
    private static boolean matches(Pattern p, String line) {
        return p.matcher(line).matches();
    }
}
