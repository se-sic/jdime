package de.fosd.jdime.strdump.graphviz;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

abstract class GraphvizGraphBase implements GraphvizElement {

    private static final String INDENT_INC = "  ";

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
    public void dump(String indent, PrintWriter out) {
        String cIndent = indent + INDENT_INC;

        out.write(" {"); out.println();
        attrStatements.forEach(a -> {a.dump(cIndent, out); out.println();});
        attributes.forEach(a -> {a.dump(cIndent, out); out.println();});
        nodes.forEach(n -> {n.dump(cIndent, out); out.println();});
        edges.forEach(e -> {e.dump(cIndent, out); out.println();});
        subGraphs.forEach(s -> {s.dump(cIndent, out); out.println();});
        out.write('}');
    }
}
