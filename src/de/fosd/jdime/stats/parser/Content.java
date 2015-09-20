package de.fosd.jdime.stats.parser;

import java.util.ArrayList;
import java.util.List;

public abstract class Content {

    public static class Merged extends Content {

        private List<String> lines;

        public Merged() {
            super(false);
            this.lines = new ArrayList<>();
        }

        public void add(String line) {
            lines.add(line);
        }

        @Override
        public String toString() {
            return String.join(System.lineSeparator(), lines);
        }
    }

    public static class Conflict extends Content {

        private static final String CONFLICT_START = "<<<<<<<";
        private static final String CONFLICT_DELIM = "=======";
        private static final String CONFLICT_END = ">>>>>>>";

        private List<String> leftLines;
        private List<String> rightLines;

        public Conflict() {
            super(true);
            this.leftLines = new ArrayList<>();
            this.rightLines = new ArrayList<>();
        }

        public void add(Conflict other) {
            leftLines.addAll(other.leftLines);
            rightLines.addAll(other.rightLines);
        }

        public void addLeft(String line) {
            leftLines.add(line);
        }

        public void addRight(String line) {
            rightLines.add(line);
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();

            b.append(CONFLICT_START);
            b.append(String.join(System.lineSeparator(), leftLines));
            b.append(CONFLICT_DELIM);
            b.append(String.join(System.lineSeparator(), rightLines));
            b.append(CONFLICT_END);

            return b.toString();
        }
    }

    protected boolean isConflict;

    public Content(boolean isConflict) {
        this.isConflict = isConflict;
    }

    public boolean isConflict() {
        return isConflict;
    }

    @Override
    public abstract String toString();
}
