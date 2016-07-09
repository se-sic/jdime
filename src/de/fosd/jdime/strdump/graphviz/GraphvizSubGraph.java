package de.fosd.jdime.strdump.graphviz;

import java.io.PrintWriter;

public class GraphvizSubGraph extends GraphvizGraphBase {

    private final GraphvizGraph rootGraph;

    public GraphvizSubGraph(GraphvizGraph rootGraph, String id) {
        super(id);
        this.rootGraph = rootGraph;
    }

    @Override
    public void dump(String indent, PrintWriter out) {

        if (!indent.isEmpty()) {
            out.write(indent);
        }

        out.printf("subgraph \"%s\"", id);
        super.dump(indent, out);
    }

    @Override
    String nextId() {
        return rootGraph.nextId();
    }

    @Override
    GraphvizGraphType getType() {
        return rootGraph.getType();
    }

    @Override
    GraphvizGraph getRootGraph() {
        return rootGraph;
    }
}
