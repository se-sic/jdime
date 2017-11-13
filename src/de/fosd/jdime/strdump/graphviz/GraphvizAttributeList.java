/**
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2017 University of Passau, Germany
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 *
 * Contributors:
 *     Olaf Lessenich <lessenic@fim.uni-passau.de>
 *     Georg Seibt <seibt@fim.uni-passau.de>
 */
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
