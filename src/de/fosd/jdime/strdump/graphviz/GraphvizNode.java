package de.fosd.jdime.strdump.graphviz;

import java.io.PrintWriter;

/**
 * Representation of a DOT language graph node.
 */
public final class GraphvizNode extends GraphvizStatement {

    private final String id;

    /**
     * Constructs a new <code>GraphvizNode</code> with the given ID.
     *
     * @param id the ID for this <code>GraphvizNode</code>
     */
    GraphvizNode(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public void dump(String indent, PrintWriter out) {

        if (!indent.isEmpty()) {
            out.write(indent);
        }

        out.write(id);
        super.dump("", out);
    }

    @Override
    public GraphvizNode attribute(String lhs, String rhs) {
        super.attribute(lhs, rhs);
        return this;
    }
}
