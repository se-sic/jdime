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

import org.extendj.parser.JavaParser;
import org.extendj.scanner.JavaScanner;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Contains methods for parsing code (possibly containing conflict markers) resulting from a merge.
 */
public final class Parser {

    private static final Pattern conflictStartPattern = Pattern.compile("^" + Content.Conflict.CONFLICT_START + ".*");
    private static final Pattern conflictSepPattern = Pattern.compile("^" + Content.Conflict.CONFLICT_DELIM);
    private static final Pattern conflictEndPattern = Pattern.compile("^" + Content.Conflict.CONFLICT_END + ".*");
    private static final Pattern emptyLine = Pattern.compile("\\s*");

    private static final Pattern whitespace = Pattern.compile("\\s+");
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

        int conflicts = 0;

        int chars = 0;
        int conflictingChars = 0;

        int tokens = 0;
        int conflictingTokens = 0;

        int linesOfCode = 0;
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
                } else if (matches(conflictStartPattern, line)) {

                    wasConflictMarker = true;
                    inConflict = true;
                    inLeftComment = inComment;
                    inLeft = true;
                    clocBeforeConflict = conflictingLinesOfCode;
                    conflicts++;
                } else if (matches(conflictSepPattern, line)) {

                    wasConflictMarker = true;
                    inComment = inLeftComment;
                    inLeft = false;
                } else if (matches(conflictEndPattern, line)) {

                    wasConflictMarker = true;
                    inConflict = false;
                    if (clocBeforeConflict == conflictingLinesOfCode) {
                        conflicts--; // the conflict only contained empty lines and comments
                    }
                } else {

                    if (!inComment) {
                        linesOfCode++;

                        // We only count non-whitespace characters to normalize the results over linebased/structured.
                        int lineLength = whitespace.matcher(line).replaceAll("").length();
                        chars += lineLength;

                        int tokenCount = getTokenCount(line);
                        tokens += tokenCount;

                        if (inConflict) {
                            conflictingLinesOfCode++;
                            conflictingChars += lineLength;
                            conflictingTokens += tokenCount;
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

        res.setConflicts(conflicts);
        res.setLinesOfCode(linesOfCode);
        res.setConflictingLinesOfCode(conflictingLinesOfCode);
        res.setChars(chars);
        res.setConflictingChars(conflictingChars);
        res.setTokens(tokens);
        res.setConflictingTokens(conflictingTokens);

        return res;
    }

    /**
     * Counts the number of tokens in the given line using {@link JavaScanner}.
     *
     * @param line
     *         the line whose tokens to count
     * @return the number of tokens in the line
     */
    private static int getTokenCount(String line) {
        int tokenCount = 0;

        try {
            JavaScanner scanner = new JavaScanner(new StringReader(line));

            try {
                while (scanner.nextToken().getId() != JavaParser.Terminals.EOF) {
                    tokenCount++;
                }
            } catch (beaver.Scanner.Exception e) {
                // TODO: replace this with warning? add 'unknown' tokens to count?
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return tokenCount;
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

    /**
     * Merges subsequent conflicts.
     *
     * @param in merge result that should be optimized w.r.t. conflicts
     * @return optimized merge result
     */
    @SuppressWarnings("fallthrough")
    public static String mergeSubsequentConflicts(String in) {
        Scanner s = new Scanner(in);
        ParseResult out = new ParseResult();

        Position pos = Position.NO_CONFLICT;
        Queue<String> queue = new LinkedList<>();

        while (s.hasNextLine()) {
            String line = s.nextLine();

            if (matches(conflictStartPattern, line)) {
                if (pos == Position.AFTER_CONFLICT) {
                    while (!queue.isEmpty()) {
                        String queuedLine = queue.remove();
                        out.addConflictingLine(queuedLine, true);
                        out.addConflictingLine(queuedLine, false);
                    }
                }
                pos = Position.LEFT_SIDE;
            } else if (matches(conflictSepPattern, line)) {
                pos = Position.RIGHT_SIDE;
            } else if (matches(conflictEndPattern, line)) {
                pos = Position.AFTER_CONFLICT;
            } else {
                switch (pos) {
                    case LEFT_SIDE:
                        out.addConflictingLine(line, true);
                        break;
                    case RIGHT_SIDE:
                        out.addConflictingLine(line, false);
                        break;
                    case AFTER_CONFLICT:
                        // lines containing only whitespaces are queued
                        // and later appended to either both sides or the common output
                        if (matches(emptyLine, line)) { queue.add(line); break; }
                        pos = Position.NO_CONFLICT;
                        // intentional fallthrough because the current line has to be appended
                        // if it's clear that we are done with the conflict
                    case NO_CONFLICT:
                        while (!queue.isEmpty()) { out.addMergedLine(queue.remove()); }
                        out.addMergedLine(line);
                        break;
                }
            }
        }

        return out.toString();
    }

    private enum Position {
        NO_CONFLICT, LEFT_SIDE, RIGHT_SIDE, AFTER_CONFLICT
    }
}
