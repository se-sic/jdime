package de.fosd.jdime.strdump.graphviz;

import java.io.PrintWriter;

/**
 * Representation of a DOT language attribute of the form "ID = ID".
 */
public final class GraphvizAttribute implements GraphvizElement {

    private final String lhs;
    private final String rhs;

    /**
     * Constructs a new <code>GraphvizAttribute</code> with the given left- and right-hand sides.
     *
     * @param lhs
     *         the left-hand side
     * @param rhs
     *         the right-hand side
     */
    GraphvizAttribute(String lhs, String rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public void dump(String indent, PrintWriter out) {

        if (!indent.isEmpty()) {
            out.write(indent);
        }

        out.printf("\"%s\"=\"%s\"", lhs, rhs);
    }
}
