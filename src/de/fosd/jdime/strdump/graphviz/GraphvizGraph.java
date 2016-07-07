package de.fosd.jdime.strdump.graphviz;

import java.io.PrintWriter;

public class GraphvizGraph extends GraphvizGraphBase {

    private final boolean strict;
    private final GraphvizGraphType type;

    public GraphvizGraph(boolean strict, GraphvizGraphType type, String id) {
        super(id);
        this.type = type;
        this.strict = strict;
    }

    @Override
    protected void dumpGraphHeader(PrintWriter out) {
        out.printf("%s %s \"%s\"", strict ? "strict" : "", type.name().toLowerCase(), id);
    }
}
