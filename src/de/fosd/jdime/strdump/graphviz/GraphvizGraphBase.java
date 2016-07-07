package de.fosd.jdime.strdump.graphviz;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

abstract class GraphvizGraphBase implements GraphvizElement {

    protected final String id;

    protected final List<GraphvizNode> nodes;
    protected final List<GraphvizEdge> edges;
    protected final List<GraphvizAttributeStmt> attributes;
    protected final List<GraphvizSubGraph> subGraphs;

    public GraphvizGraphBase(String id) {
        this.id = id;
        this.nodes = new ArrayList<>();
        this.edges = new ArrayList<>();
        this.attributes = new ArrayList<>();
        this.subGraphs = new ArrayList<>();
    }

    protected abstract void dumpGraphHeader(PrintWriter out);

    @Override
    public void dump(PrintWriter out) {
        dumpGraphHeader(out); out.printf(" {%n");
        nodes.forEach(n -> n.dump(out));
        edges.forEach(e -> e.dump(out));
        attributes.forEach(a -> a.dump(out));
        subGraphs.forEach(s -> {s.dump(out); out.printf("%n");});
        out.write('}');
    }
}
