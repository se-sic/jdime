package de.fosd.jdime.strdump.graphviz;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Representation of a DOT language attribute list of the form "[ID = ID, ID = ID, ... ]".
 */
public final class GraphvizAttributeList implements GraphvizElement {

    private final List<GraphvizAttribute> attributes;

    /**
     * Constructs a new empty <code>GraphvizAttributeList</code>.
     */
    GraphvizAttributeList() {
        this.attributes = new ArrayList<>();
    }

    @Override
    public void dump(String indent, PrintWriter out) {

        if (attributes.isEmpty()) {
            return;
        }

        if (!indent.isEmpty()) {
            out.write(indent);
        }

        out.write('[');

        for (Iterator<GraphvizAttribute> it = attributes.iterator(); it.hasNext();) {
            it.next().dump(out);

            if (it.hasNext()) {
                out.write(", ");
            }
        }

        out.write(']');
    }

    /**
     * Constructs a new <code>GraphvizAttribute</code> with the given left- and right-hand sides, adds it to this
     * <code>GraphvizAttributeList</code> and returns the created <code>GraphvizAttribute</code>.
     *
     * @param lhs
     *         the left-hand side
     * @param rhs
     *         the right-hand side
     * @return the created <code>GraphvizAttribute</code>
     */
    public GraphvizAttribute attribute(String lhs, String rhs) {
        GraphvizAttribute attr = new GraphvizAttribute(lhs, rhs);

        attributes.add(attr);
        return attr;
    }
}
