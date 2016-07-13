package de.fosd.jdime.strdump.graphviz;

import java.io.PrintWriter;

/**
 * Representation of a DOT language sub-graph.
 *
 * @see <a href="http://www.graphviz.org/">http://www.graphviz.org/</a>
 */
public final class GraphvizSubGraph extends GraphvizGraphBase {

    private final GraphvizGraph rootGraph;

    GraphvizSubGraph(GraphvizGraph rootGraph, String id) {
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
    GraphvizGraph getRootGraph() {
        return rootGraph;
    }
}
