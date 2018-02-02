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

/**
 * Abstract superclass for all classes representing DOT language statements (ending in a ';').
 */
abstract class GraphvizStatement implements GraphvizElement {

    protected final GraphvizGraphBase graph;
    protected final GraphvizAttributeList attributes;

    /**
     * Constructs a new <code>GraphvizStatement</code>.
     *
     * @param graph
     *         the Graphviz graph containing this <code>GraphvizStatement</code>
     */
    GraphvizStatement(GraphvizGraphBase graph) {
        this.graph = graph;
        this.attributes = new GraphvizAttributeList();
    }

    @Override
    public void dump(String indent, PrintWriter out) {

        if (!indent.isEmpty()) {
            out.write(indent);
        }

        attributes.dump(" ", out);
        out.write(';');
    }

    /**
     * Constructs a new <code>GraphvizAttribute</code> with the given left- and right-hand sides and adds it to this
     * <code>GraphvizStatement</code>.
     *
     * @param lhs
     *         the left-hand side
     * @param rhs
     *         the right-hand side
     * @return <code>this</code>
     */
    public GraphvizStatement attribute(String lhs, String rhs) {
        attributes.attribute(lhs, rhs);
        return this;
    }

    /**
     * Returns the Graphviz graph containing this <code>GraphvizStatement</code>.
     *
     * @return the Graphviz graph containing this <code>GraphvizStatement</code>
     */
    public GraphvizGraphBase getGraph() {
        return graph;
    }
}
