package de.fosd.jdime.strdump.graphviz;

import java.io.PrintWriter;

public class GraphvizSubGraph extends GraphvizGraphBase {

    public GraphvizSubGraph(String id) {
        super(id);
    }

    @Override
    public void dump(String indent, PrintWriter out) {

        if (!indent.isEmpty()) {
            out.write(indent);
        }

        out.printf("subgraph \"%s\"", id);
        super.dump(out);
    }
}
