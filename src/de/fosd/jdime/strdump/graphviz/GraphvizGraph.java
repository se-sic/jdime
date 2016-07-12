package de.fosd.jdime.strdump.graphviz;

import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Representation of a DOT language root graph.
 *
 * @see <a href="http://www.graphviz.org/">http://www.graphviz.org/</a>
 */
public final class GraphvizGraph extends GraphvizGraphBase {

    private static final String ROOT_ID = "ROOT";

    private final AtomicLong nextId;

    private final boolean strict;
    private final GraphvizGraphType type;

    /**
     * Constructs a new <code>GraphvizGraph</code> of the given <code>type</code>.
     *
     * @param strict
     *         whether the graph is to be 'strict' (without multi-edges) (note: this only adds a keyword to the
     *         output, adding of edges is unrestricted)
     * @param type
     *         the type of graph to be created
     */
    public GraphvizGraph(boolean strict, GraphvizGraphType type) {
        super(ROOT_ID);
        this.nextId = new AtomicLong();
        this.type = type;
        this.strict = strict;
    }

    @Override
    public void dump(String indent, PrintWriter out) {

        if (!indent.isEmpty()) {
            out.write(indent);
        }

        out.printf("%s%s \"%s\"", strict ? "strict " : "", type.name().toLowerCase(), id);
        super.dump(indent, out);
    }

    /**
     * Returns the next unique ID to be used in this <code>GraphvizGraph</code> and its sub-graphs.
     *
     * @return the next ID
     */
    String nextId() {
        return String.valueOf(nextId.getAndIncrement());
    }

    /**
     * Returns the <code>GraphvizGraphType</code> of this <code>GraphvizGraph</code>.
     *
     * @return the type of this graph
     */
    GraphvizGraphType getType() {
        return type;
    }

    @Override
    GraphvizGraph getRootGraph() {
        return this;
    }
}
