package de.fosd.jdime.strdump.graphviz;

import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicLong;

public class GraphvizGraph extends GraphvizGraphBase {

    private final AtomicLong id;

    private final boolean strict;
    private final GraphvizGraphType type;

    public GraphvizGraph(boolean strict, GraphvizGraphType type, String id) {
        super(id);
        this.id = new AtomicLong();
        this.type = type;
        this.strict = strict;
    }

    @Override
    public void dump(String indent, PrintWriter out) {

        if (!indent.isEmpty()) {
            out.write(indent);
        }

        out.printf("%s %s \"%s\"", strict ? "strict" : "", type.name().toLowerCase(), id);
        super.dump(out);
    }

    @Override
    String nextId() {
        return String.valueOf(id.getAndIncrement());
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
