package de.fosd.jdime.strdump.graphviz;

import java.io.PrintWriter;

public class GraphvizSubGraph extends GraphvizGraphBase {

    public GraphvizSubGraph(String id) {
        super(id);
    }

    @Override
    protected void dumpGraphHeader(PrintWriter out) {
        out.printf("subgraph %s", id);
    }
}
