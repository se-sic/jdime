package de.fosd.jdime.strdump.graphviz;

import java.io.PrintWriter;

abstract class GraphvizStatement implements GraphvizElement {

    protected final GraphvizAttributeList attributes;

    public GraphvizStatement() {
        this.attributes = new GraphvizAttributeList();
    }

    @Override
    public void dump(PrintWriter out) {
        attributes.dump(out);
        out.printf(";%n");
    }
}
