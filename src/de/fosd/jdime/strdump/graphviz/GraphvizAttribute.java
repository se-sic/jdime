package de.fosd.jdime.strdump.graphviz;

import java.io.PrintWriter;

public class GraphvizAttribute implements GraphvizElement {

    private final String lhs;
    private final String rhs;

    public GraphvizAttribute(String lhs, String rhs) {
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
