package de.fosd.jdime.strdump.graphviz;

import java.io.PrintWriter;

public class GraphvizNode extends GraphvizStatement {

    private final String id;

    public GraphvizNode(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public void dump(String indent, PrintWriter out) {

        if (!indent.isEmpty()) {
            out.write(indent);
        }

        out.write(id);
        super.dump(out);
    }
}
