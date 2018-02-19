package de.fosd.jdime.util;

import de.fosd.jdime.config.merge.MergeContext;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.regex.Pattern;

public class ConflictOptimizer {
    private ConflictOptimizer() {}

    private static final Pattern conflictStart = Pattern.compile("^" + MergeContext.conflictStart + ".*");
    private static final Pattern conflictSep = Pattern.compile("^" + MergeContext.conflictSep);
    private static final Pattern conflictEnd = Pattern.compile("^" + MergeContext.conflictEnd + ".*");
    private static final Pattern emptyLine = Pattern.compile("\\s*");

    private enum Position {
        NO_CONFLICT, LEFT_SIDE, RIGHT_SIDE, AFTER_CONFLICT
    }

    /**
     * Merges subsequent conflicts.
     *
     * @param in merge result that should be optimized w.r.t. conflicts
     * @return optimized merge result
     */
    public static String mergeSubsequentConflicts(String in) {
        Scanner s = new Scanner(in);
        StringBuilder out = new StringBuilder();

        Position pos = Position.NO_CONFLICT;
        List<String> left = new LinkedList<>();
        List<String> right = new LinkedList<>();
        Queue<String> queue = new LinkedList<>();

        while (s.hasNextLine()) {
            String line = s.nextLine();

            if (matches(conflictStart, line)) {
                if (pos == Position.AFTER_CONFLICT) {
                    while (!queue.isEmpty()) {
                        String queuedLine = queue.remove();
                        left.add(queuedLine);
                        right.add(queuedLine);
                    }
                }
                pos = Position.LEFT_SIDE;
            } else if (matches(conflictSep, line)) {
                pos = Position.RIGHT_SIDE;
            } else if (matches(conflictEnd, line)) {
                pos = Position.AFTER_CONFLICT;
            } else {
                switch (pos) {
                    case LEFT_SIDE:
                        left.add(line + System.lineSeparator());
                        break;
                    case RIGHT_SIDE:
                        right.add(line + System.lineSeparator());
                        break;
                    case AFTER_CONFLICT:
                        // lines containing only whitespaces are queued
                        // and later appended to either both sides or the common output
                        if (matches(emptyLine, line)) { queue.add(line + System.lineSeparator()); break; }
                        pos = Position.NO_CONFLICT;
                    case NO_CONFLICT:
                        if (pos == Position.NO_CONFLICT && (!left.isEmpty() || !right.isEmpty())) {
                            // -> print the previous conflict
                            out.append(MergeContext.conflictStart + System.lineSeparator());
                            for (String l : left) { out.append(l); }
                            out.append(MergeContext.conflictSep + System.lineSeparator());
                            for (String r : right) { out.append(r); }
                            out.append(MergeContext.conflictEnd + System.lineSeparator());

                            left.clear();
                            right.clear();
                        }
                        while (!queue.isEmpty()) { out.append(queue.remove()); }
                        out.append(line + System.lineSeparator());
                        break;
                }
            }
        }

        return out.toString();
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
