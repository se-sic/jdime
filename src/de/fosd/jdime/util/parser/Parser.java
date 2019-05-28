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

import org.extendj.parser.JavaParser;
import org.extendj.scanner.JavaScanner;
import org.extendj.scanner.Unicode;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static de.fosd.jdime.util.parser.Content.Conflict.*;

/**
 * Contains methods for parsing code (possibly containing conflict markers) resulting from a merge.
 */
public final class Parser {

    private static final Logger LOG = Logger.getLogger(Parser.class.getCanonicalName());

    static final Pattern emptyLine = Pattern.compile("\\s*");
    static final Pattern whitespace = Pattern.compile("\\s+");

    static final Pattern conflictStartPattern = Pattern.compile("^" + CONFLICT_START + "(?: .*$|$)");
    static final Pattern conflictSepPattern = Pattern.compile("^" + CONFLICT_DELIM + "$");
    static final Pattern conflictEndPattern = Pattern.compile("^" + CONFLICT_END + "(?: .*$|$)");

    static final Pattern lineComment = Pattern.compile("\\s*//.*");
    static final Pattern blockComment1Line = Pattern.compile("\\s*/\\*.*?\\*/\\s*");
    static final Pattern blockCommentStart = Pattern.compile("\\s*/\\*.*");
    static final Pattern blockCommentEnd = Pattern.compile(".*?\\*/");

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

        // Whether there was an error parsing a line to count its tokens.
        boolean tokensCounted = true;

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

                    String[] startAndLabel = line.split(" ");
                    if (startAndLabel.length == 2) {
                        res.setLeftLabel(startAndLabel[1]);
                    }
                } else if (matches(conflictSepPattern, line)) {

                    wasConflictMarker = true;
                    inComment = inLeftComment;
                    inLeft = false;
                } else if (matches(conflictEndPattern, line)) {

                    wasConflictMarker = true;
                    inConflict = false;
                    if (clocBeforeConflict == conflictingLinesOfCode) {
                        ((Conflict) res.getLast()).setFiltered();
                        conflicts--; // the conflict only contained empty lines and comments
                    }

                    String[] endAndLabel = line.split(" ");
                    if (endAndLabel.length == 2) {
                        res.setRightLabel(endAndLabel[1]);
                    }
                } else {

                    if (!inComment) {
                        linesOfCode++;

                        // We only count non-whitespace characters to normalize the results over linebased/structured.
                        int lineLength = whitespace.matcher(line).replaceAll("").length();
                        chars += lineLength;

                        int tokenCount = 0;

                        try {
                            tokenCount = getTokenCount(line);
                        } catch (beaver.Scanner.Exception e) {
                            LOG.log(Level.WARNING, e, () -> "Exception while parsing line '" + line + "' " +
                                                            "to count its tokens. ParseResult will show 0 tokens " +
                                                            "for the whole code fragment.");
                            tokensCounted = false;
                        }

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
                    res.addConflictingLine(line, inLeft, inComment);
                } else {
                    res.addMergedLine(line, inComment);
                }
            }
        }

        res.setConflicts(conflicts);
        res.setLinesOfCode(linesOfCode);
        res.setConflictingLinesOfCode(conflictingLinesOfCode);
        res.setChars(chars);
        res.setConflictingChars(conflictingChars);

        if (tokensCounted) {
            res.setTokens(tokens);
            res.setConflictingTokens(conflictingTokens);
        }

        return res;
    }

    private static class ZeroReader extends Reader {

        private Reader reader;

        public ZeroReader(Reader reader) {
            this.reader = reader;
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            int n = reader.read(cbuf, off, len);

            if (n != 0 || len <= 0) {
                return n;
            }

            if (off < cbuf.length) {
                int c = reader.read();

                if (c == -1) {
                    return -1;
                }

                cbuf[off] = (char) c;
                return 1;
            }

            throw new IOException("Offset " + off + " is outside the buffer.");
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }

    /**
     * Counts the number of tokens in the given line using {@link JavaScanner}.
     *
     * @param line
     *         the line whose tokens to count
     * @return the number of tokens in the line
     */
    private static int getTokenCount(String line) throws beaver.Scanner.Exception {
        int tokenCount = 0;

        try {
            JavaScanner scanner = new JavaScanner(new ZeroReader(new Unicode(new StringReader(line))));

            while (scanner.nextToken().getId() != JavaParser.Terminals.EOF) {
                tokenCount++;
            }
        } catch (IOException e) {
            throw new RuntimeException("JavaScanner threw an IOException while parsing '" + line + "'", e);
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
                String[] startAndLabel = line.split(" ");
                if (startAndLabel.length == 2) {
                    out.setLeftLabel(startAndLabel[1]);
                }
                if (pos == Position.AFTER_CONFLICT) {
                    while (!queue.isEmpty()) {
                        String queuedLine = queue.remove();
                        out.addConflictingLine(queuedLine, true, false);
                        out.addConflictingLine(queuedLine, false, false);
                    }
                }
                pos = Position.LEFT_SIDE;
            } else if (matches(conflictSepPattern, line)) {
                pos = Position.RIGHT_SIDE;
            } else if (matches(conflictEndPattern, line)) {
                String[] endAndLabel = line.split(" ");
                if (endAndLabel.length == 2) {
                    out.setRightLabel(endAndLabel[1]);
                }
                pos = Position.AFTER_CONFLICT;
            } else {
                switch (pos) {
                    case LEFT_SIDE:
                        out.addConflictingLine(line, true, false);
                        break;
                    case RIGHT_SIDE:
                        out.addConflictingLine(line, false, false);
                        break;
                    case AFTER_CONFLICT:
                        // lines containing only whitespaces are queued
                        // and later appended to either both sides or the common output
                        if (matches(emptyLine, line)) { queue.add(line); break; }
                        pos = Position.NO_CONFLICT;
                        // intentional fallthrough because the current line has to be appended
                        // if it's clear that we are done with the conflict
                    case NO_CONFLICT:
                        while (!queue.isEmpty()) { out.addMergedLine(queue.remove(), false); }
                        out.addMergedLine(line, false);
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
