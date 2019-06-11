package de.fosd.jdime.util.parser;

/**
 * A wrapper around a line of code ({@link String}) containing additional info from the {@link Parser} that produced it.
 */
class LineOfCode {

    final String line;

    final boolean empty;
    final boolean comment;

    /**
     * Constructs a new {@link LineOfCode}.
     *
     * @param line    the actual line of code
     * @param comment whether the {@link Parser} considered this line to be part of a comment
     */
    LineOfCode(String line, boolean comment) {
        this.line = line;
        this.empty = Parser.emptyLine.matcher(line).matches();
        this.comment = comment;
    }

    /**
     * Returns the actual line of code.
     *
     * @return the line of code
     */
    String getLine() {
        return line;
    }

    /**
     * Returns whether this line is empty (consisting only of whitespace).
     *
     * @return whether the line is empty
     */
    public boolean isEmpty() {
        return empty;
    }

    /**
     * Returns whether this {@link LineOfCode} is considered to be part of a comment.
     *
     * @return whether the {@link LineOfCode} is commented out
     */
    boolean isComment() {
        return comment;
    }

    @Override
    public String toString() {
        return line;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LineOfCode that = (LineOfCode) o;

        if (comment != that.comment) return false;
        return line.equals(that.line);

    }

    @Override
    public int hashCode() {
        int result = line.hashCode();
        result = 31 * result + (comment ? 1 : 0);
        return result;
    }
}
