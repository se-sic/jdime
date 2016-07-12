package de.fosd.jdime.strdump.graphviz;

import java.io.PrintWriter;

/**
 * Abstract superclass for all classes representing DOT language statements (ending in a ';').
 */
abstract class GraphvizStatement implements GraphvizElement {

    protected final GraphvizAttributeList attributes;

    /**
     * Constructs a new <code>GraphvizStatement</code>.
     */
    GraphvizStatement() {
        this.attributes = new GraphvizAttributeList();
    }

    @Override
    public void dump(String indent, PrintWriter out) {

        if (!indent.isEmpty()) {
            out.write(indent);
        }

        attributes.dump(" ", out);
        out.write(';');
    }

    /**
     * Constructs a new <code>GraphvizAttribute</code> with the given left- and right-hand sides and adds it to this
     * <code>GraphvizStatement</code>.
     *
     * @param lhs
     *         the left-hand side
     * @param rhs
     *         the right-hand side
     * @return <code>this</code>
     */
    public GraphvizStatement attribute(String lhs, String rhs) {
        attributes.attribute(lhs, rhs);
        return this;
    }
}
