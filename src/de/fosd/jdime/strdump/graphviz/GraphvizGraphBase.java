package de.fosd.jdime.strdump.graphviz;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

abstract class GraphvizGraphBase implements GraphvizElement {

    protected final String id;

    protected final List<GraphvizAttributeStmt> attrStatements;
    protected final List<GraphvizAttribute> attributes;
    protected final List<GraphvizNode> nodes;
    protected final List<GraphvizEdge> edges;
    protected final List<GraphvizSubGraph> subGraphs;

    public GraphvizGraphBase(String id) {
        this.id = id;
        this.attrStatements = new ArrayList<>();
        this.attributes = new ArrayList<>();
        this.nodes = new ArrayList<>();
        this.edges = new ArrayList<>();
        this.subGraphs = new ArrayList<>();
    }

    @Override
    public void dump(PrintWriter out) {
        out.write(" {"); out.println();
        attrStatements.forEach(a -> {a.dump(out); out.println();});
        attributes.forEach(a -> {a.dump(out); out.println();});
        nodes.forEach(n -> {n.dump(out); out.println();});
        edges.forEach(e -> {e.dump(out); out.println();});
        subGraphs.forEach(s -> {s.dump(out); out.println();});
        out.write('}');
    }
}
