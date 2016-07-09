package de.fosd.jdime.strdump.graphviz;

import java.io.PrintWriter;

public class GraphvizEdge extends GraphvizStatement {

    private final GraphvizGraphType type;
    private final GraphvizNode from;
    private final GraphvizNode to;

    public GraphvizEdge(GraphvizGraphType type, GraphvizNode from, GraphvizNode to) {
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
        super.dump(indent, out);
    }
}
