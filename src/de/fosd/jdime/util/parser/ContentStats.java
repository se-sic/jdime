package de.fosd.jdime.util.parser;

public class ContentStats {

    int conflicts;

    int linesOfCode;
    int conflictingLinesOfCode;

    int chars;
    int conflictingChars;

    int tokens;
    int conflictingTokens;

    public int getConflicts() {
        return conflicts;
    }

    public int getLinesOfCode() {
        return linesOfCode;
    }

    public int getConflictingLinesOfCode() {
        return conflictingLinesOfCode;
    }

    public int getChars() {
        return chars;
    }

    public int getConflictingChars() {
        return conflictingChars;
    }

    public int getTokens() {
        return tokens;
    }

    public int getConflictingTokens() {
        return conflictingTokens;
    }
}
