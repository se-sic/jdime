package de.fosd.jdime.strdump.graphviz;

import java.io.PrintWriter;

abstract class GraphvizStatement implements GraphvizElement {

    protected final GraphvizAttributeList attributes;

    public GraphvizStatement() {
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

    public GraphvizStatement attribute(String lhs, String rhs) {
        attributes.attribute(lhs, rhs);
        return this;
    }
}
