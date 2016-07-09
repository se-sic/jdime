package de.fosd.jdime.strdump.graphviz;

import java.io.PrintWriter;

public class GraphvizAttributeStmt extends GraphvizStatement {

    private final GraphvizAttributeStmtType type;

    public GraphvizAttributeStmt(GraphvizAttributeStmtType type) {
        this.type = type;
    }

    @Override
    public void dump(String indent, PrintWriter out) {

        if (!indent.isEmpty()) {
            out.write(indent);
        }

        out.write(type.name().toLowerCase()); out.write(' ');
        super.dump(indent, out);
    }
}
