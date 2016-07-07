package de.fosd.jdime.strdump.graphviz;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GraphvizAttributeList implements GraphvizElement {

    private final List<GraphvizAttribute> attributes;

    public GraphvizAttributeList() {
        this.attributes = new ArrayList<>();
    }

    @Override
    public void dump(PrintWriter out) {
        out.write('[');

        for (Iterator<GraphvizAttribute> it = attributes.iterator(); it.hasNext();) {
            it.next().dump(out);

            if (it.hasNext()) {
                out.write(", ");
            }
        }

        out.write(']');
    }
}
