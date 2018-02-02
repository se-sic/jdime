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
 * Representation of a DOT language graph node.
 */
public final class GraphvizNode extends GraphvizStatement {

    private final String id;

    /**
     * Constructs a new <code>GraphvizNode</code> with the given ID.
     *
     * @param graph
     *         the Graphviz graph containing this <code>GraphvizStatement</code>
     * @param id
     *         the ID for this <code>GraphvizNode</code>
     */
    GraphvizNode(GraphvizGraphBase graph, String id) {
        super(graph);
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
        super.dump("", out);
    }

    @Override
    public GraphvizNode attribute(String lhs, String rhs) {
        super.attribute(lhs, rhs);
        return this;
    }
}
