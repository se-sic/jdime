package de.fosd.jdime.stats.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public abstract class Content {

    private static final Logger LOG = Logger.getLogger(Content.class.getCanonicalName());

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

        @Override
        public String toString(String fstId, String... ids) {
            return toString();
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

        @Override
        public String toString(String fstId, String... ids) {

            if (fstId != null && ids == null || ids.length < 1) {
                LOG.warning("Insufficient identifiers for constructing a detailed conflict representation.");
                return toString();
            }

            StringBuilder b = new StringBuilder();

            b.append(CONFLICT_START).append(" ").append(fstId);
            b.append(String.join(System.lineSeparator(), leftLines));
            b.append(CONFLICT_DELIM);
            b.append(String.join(System.lineSeparator(), rightLines));
            b.append(CONFLICT_END).append(" ").append(ids[0]);

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

    /**
     * Returns a <code>String</code> representation of this piece of <code>Content</code>. The identifiers will
     * be used by the implementations to identify their parts (for example <code>Conflict</code> will use the first
     * two identifiers for marking the two sides of the conflict).
     *
     * @param fstId
     *         to first identifier to use
     * @param ids
     *         the other identifiers to use
     * @return a <code>String</code> representing this piece of <code>Content</code>
     */
    public abstract String toString(String fstId, String... ids);
}
