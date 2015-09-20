package de.fosd.jdime.stats.parser;

import java.util.Scanner;
import java.util.regex.Pattern;

public final class Parser {

    private static final Pattern conflictStart = Pattern.compile("<<<<<<<.*");
    private static final Pattern conflictSep = Pattern.compile("=======");
    private static final Pattern conflictEnd = Pattern.compile(">>>>>>>.*");
    private static final Pattern emptyLine = Pattern.compile("\\s*");

    private static final Pattern lineComment = Pattern.compile("\\s*//.*");
    private static final Pattern blockComment1Line = Pattern.compile("\\s*/\\*.*\\*/\\s*");

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

        boolean inConflict = false;
        boolean inComment = false;

        while (s.hasNextLine()) {
            String line = s.nextLine();

            if (emptyLine.matcher(line).matches()) {
                continue;
            }

            if (conflictStart.matcher(line).matches()) {
                conflicts++;
                inConflict = true;
            } else if (conflictEnd.matcher(line).matches()) {
                inConflict = false;
            } else if (!conflictSep.matcher(line).matches()){

                if (!inComment) {
                    if (inConflict) {
                        conflictingLinesOfCode++;
                    } else {
                        mergedLinesOfCode++;
                    }
                }


            }
        }

        res.setMergedLinesOfCode(mergedLinesOfCode);
        res.setConflicts(conflicts);
        res.setConflictingLinesOfCode(conflictingLinesOfCode);

        return res;
    }
}
