package de.fosd.jdime.strdump.graphviz;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract superclass for the <code>GraphvizGraph</code> and <code>GraphvizSubGraph</code> containing all
 * shared attributes and the methods for constructing elements of the graph.
 */
abstract class GraphvizGraphBase implements GraphvizElement {

    private static final String INDENT_INC = "  ";
    private static final String CLUSTER_PREFIX = "cluster";

    protected final String id;

    protected final List<GraphvizAttributeStmt> attrStatements;
    protected final List<GraphvizAttribute> attributes;
    protected final List<GraphvizNode> nodes;
    protected final List<GraphvizEdge> edges;
    protected final List<GraphvizSubGraph> subGraphs;

    /**
     * Constructs a new <code>GraphvizGraphBase</code> with the given graph ID.
     *
     * @param id
     *         the ID for this graph
     */
    GraphvizGraphBase(String id) {
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

        out.write(" {");
        out.println();

        attrStatements.forEach(a -> {
            a.dump(cIndent, out);
            out.println();
        });

        attributes.forEach(a -> {
            a.dump(cIndent, out);
            out.write(';');
            out.println();
        });

        nodes.forEach(n -> {
            n.dump(cIndent, out);
            out.println();
        });

        edges.forEach(e -> {
            e.dump(cIndent, out);
            out.println();
        });

        subGraphs.forEach(s -> {
            s.dump(cIndent, out);
            out.println();
        });

        out.write(indent);
        out.write('}');
    }

    /**
     * Returns the root <code>GraphvizGraph</code> of the object structure this <code>GraphvizGraphBase</code> is a
     * part of.
     *
     * @return the root <code>GraphvizGraph</code>
     */
    abstract GraphvizGraph getRootGraph();

    /**
     * Creates a new <code>GraphvizAttributeStmt</code>, adds it to this graph and returns it.
     *
     * @param type
     *         the <code>GraphvizAttributeStmtType</code> for the <code>GraphvizAttributeStmt</code>
     * @return the newly created <code>GraphvizAttributeStmt</code>
     */
    public GraphvizAttributeStmt attributeStmt(GraphvizAttributeStmtType type) {
        GraphvizAttributeStmt attrStmt = new GraphvizAttributeStmt(type);

        attrStatements.add(attrStmt);
        return attrStmt;
    }

    /**
     * Creates a new <code>GraphvizAttribute</code>, adds it to this graph and returns it.
     *
     * @param lhs
     *         the left-hand side for the attribute
     * @param rhs
     *         the right-hand side for the attribute
     * @return the newly created <code>GraphvizAttribute</code>
     */
    public GraphvizAttribute attribute(String lhs, String rhs) {
        GraphvizAttribute attr = new GraphvizAttribute(lhs, rhs);

        attributes.add(attr);
        return attr;
    }

    /**
     * Creates a new <code>GraphvizNode</code>, adds it to this graph and returns it.
     *
     * @return the newly created <code>GraphvizNode</code>
     */
    public GraphvizNode node() {
        GraphvizNode node = new GraphvizNode(getRootGraph().nextId());

        nodes.add(node);
        return node;
    }

    /**
     * Creates a new <code>GraphvizEdge</code>, adds it to this graph and returns it.
     *
     * @param from
     *         the starting node
     * @param to
     *         the destination node
     * @return the newly created <code>GraphvizEdge</code>
     */
    public GraphvizEdge edge(GraphvizNode from, GraphvizNode to) {
        GraphvizEdge edge = new GraphvizEdge(getRootGraph().getType(), from, to);

        edges.add(edge);
        return edge;
    }

    /**
     * Creates a new <code>GraphvizSubGraph</code>, adds it to this graph and returns it.
     *
     * @return the newly created <code>GraphvizSubGraph</code>
     */
    public GraphvizSubGraph subGraph() {
        return subGraph(getRootGraph().nextId());
    }

    /**
     * Creates a new <code>GraphvizSubGraph</code>, adds it to this graph and returns it. The ID of the created
     * sub-graph will start with 'cluster' which will cause some layout algorithms to treat it differently.
     *
     * @return the newly created <code>GraphvizSubGraph</code>
     */
    public GraphvizSubGraph subGraphCluster() {
        return subGraph(CLUSTER_PREFIX + getRootGraph().nextId());
    }

    /**
     * Creates a new <code>GraphvizSubGraph</code>, adds it to this graph and returns it.
     *
     * @param id
     *         the ID for the sub-graph
     * @return the newly created <code>GraphvizSubGraph</code>
     */
    private GraphvizSubGraph subGraph(String id) {
        GraphvizSubGraph subGraph = new GraphvizSubGraph(getRootGraph(), id);

        subGraphs.add(subGraph);
        return subGraph;
    }
}
