package de.fosd.jdime.strdump.graphviz;

import java.io.PrintWriter;

/**
 * Representation of a DOT language attribute statement of the form "(graph | node | edge) [ID = ID, ID = ID, ... ]".
 */
public final class GraphvizAttributeStmt extends GraphvizStatement {

    private final GraphvizAttributeStmtType type;

    GraphvizAttributeStmt(GraphvizAttributeStmtType type) {
        this.type = type;
    }

    @Override
    public void dump(String indent, PrintWriter out) {

        if (!indent.isEmpty()) {
            out.write(indent);
        }

        out.write(type.name().toLowerCase());
        super.dump("", out);
    }

    @Override
    public GraphvizAttributeStmt attribute(String lhs, String rhs) {
        super.attribute(lhs, rhs);
        return this;
    }
}
