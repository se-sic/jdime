package de.fosd.jdime.strdump.graphviz;

import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicLong;

public class GraphvizGraph extends GraphvizGraphBase {

    private static final String ROOT_ID = "ROOT";

    private final AtomicLong nextId;

    private final boolean strict;
    private final GraphvizGraphType type;

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

    @Override
    String nextId() {
        return String.valueOf(nextId.getAndIncrement());
    }

    @Override
    GraphvizGraphType getType() {
        return type;
    }

    @Override
    GraphvizGraph getRootGraph() {
        return this;
    }
}
