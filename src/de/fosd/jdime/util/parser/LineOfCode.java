package de.fosd.jdime.util.parser;

class LineOfCode {

    final String line;

    final boolean empty;
    final boolean comment;

    LineOfCode(String line, boolean comment) {
        this.line = line;
        this.empty = Parser.emptyLine.matcher(line).matches();
        this.comment = comment;
    }

    String getLine() {
        return line;
    }

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
