package de.fosd.jdime.stats.parser;

import java.util.Scanner;
import java.util.regex.Pattern;

public final class Parser {

    private static final Pattern conflictStart = Pattern.compile("<<<<<<<.*");
    private static final Pattern conflictSep = Pattern.compile("=======");
    private static final Pattern conflictEnd = Pattern.compile(">>>>>>>.*");
    private static final Pattern emptyLine = Pattern.compile("\\s*");

    private static final Pattern lineComment = Pattern.compile("\\s*//.*");
    private static final Pattern blockComment1Line = Pattern.compile("\\s*/\\*.*?\\*/\\s*");
    private static final Pattern blockCommentStart = Pattern.compile("\\s*/\\*.*");
    private static final Pattern blockCommentEnd = Pattern.compile(".*?\\*/");

    private Parser() {}

    /**
     * Parses the given code to a list of {@link Content} objects and counts the merged and conflicting lines
     * and the number of conflicts.
     *
     * @param code the piece of code to be parsed
     * @return the parse result
     */
    public static ParseResult parse(String code) {
        Scanner s = new Scanner(code);

        ParseResult res = new ParseResult();

        int mergedLinesOfCode = 0;
        int conflicts = 0;
        int conflictingLinesOfCode = 0;
        int clocBeforeConflict = 0;

        boolean inConflict = false;
        boolean inLeft = true;
        boolean inComment = false;

        while (s.hasNextLine()) {
            String line = s.nextLine();
            boolean wasConflictMarker = false;

            if (matches(emptyLine, line) || matches(lineComment, line)) {
                // we ignore empty lines and line comments
            } else if (matches(blockCommentStart, line)) {

                if (!matches(blockComment1Line, line)) {
                    inComment = true;
                }
            } else if (matches(blockCommentEnd, line)) {

                inComment = false;
            } else if (matches(conflictStart, line)) {

                wasConflictMarker = true;
                inConflict = true;
                inLeft = true;
                clocBeforeConflict = conflictingLinesOfCode;
                conflicts++;
            } else if (matches(conflictSep, line)) {

                wasConflictMarker = true;
                inLeft = false;
            } else if (matches(conflictEnd, line)) {

                wasConflictMarker = true;
                inConflict = false;
                if (clocBeforeConflict == conflictingLinesOfCode) {
                    conflicts--; // the conflict only contained empty lines and comments
                }
            } else {

                if (!inComment) {
                    if (inConflict) {
                        conflictingLinesOfCode++;
                    } else {
                        mergedLinesOfCode++;
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

        res.setMergedLinesOfCode(mergedLinesOfCode);
        res.setConflicts(conflicts);
        res.setConflictingLinesOfCode(conflictingLinesOfCode);

        return res;
    }

    public static boolean matches(Pattern p, String line) {
        return p.matcher(line).matches();
    }
}
