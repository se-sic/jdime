package de.fosd.jdime.strdump.graphviz;

import java.io.PrintWriter;

/**
 * Representation of a DOT language edge of the form "ID (-- | ->) ID".
 */
public final class GraphvizEdge extends GraphvizStatement {

    private final GraphvizGraphType type;
    private final GraphvizNode from;
    private final GraphvizNode to;

    /**
     * Constructs a new <code>GraphvizEdge</code> between the two given <code>GraphvizNode</code>s.
     *
     * @param type
     *         the type of the <code>GraphvizGraph</code> containing the edge.
     * @param from
     *         the starting node
     * @param to
     *         the destination node
     */
    GraphvizEdge(GraphvizGraphType type, GraphvizNode from, GraphvizNode to) {
        this.type = type;
        this.from = from;
        this.to = to;
    }

    @Override
    public void dump(String indent, PrintWriter out) {

        if (!indent.isEmpty()) {
            out.write(indent);
        }

        out.printf("%s %s %s", from.getId(), type.edgeOp, to.getId());
        super.dump("", out);
    }

    @Override
    public GraphvizEdge attribute(String lhs, String rhs) {
        super.attribute(lhs, rhs);
        return this;
    }
}
