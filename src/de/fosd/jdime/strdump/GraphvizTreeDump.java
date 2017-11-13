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
package de.fosd.jdime.strdump;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.strdump.graphviz.GraphvizEdge;
import de.fosd.jdime.strdump.graphviz.GraphvizGraph;
import de.fosd.jdime.strdump.graphviz.GraphvizNode;

import static de.fosd.jdime.strdump.graphviz.GraphvizAttributeStmtType.NODE;
import static de.fosd.jdime.strdump.graphviz.GraphvizGraphType.DIGRAPH;

/**
 * Dumps the given <code>Artifact</code> tree in Graphviz format.
 *
 * @see <a href="http://www.graphviz.org/">Graphviz</a>
 */
public class GraphvizTreeDump implements StringDumper {

    /**
     * Constructs the appropriate objects to represent the tree under <code>Artifact</code> using the
     * <code>GraphvizGraph</code>.
     *
     * @param artifact
     *         the root of the tree to construct using <code>graph</code>
     * @param getLabel
     *         the function to use for producing labels for artifacts
     * @param graph
     *         the <code>GraphvizGraph</code> to be used for constructing objects to represent the
     *         <code>Artifact</code> tree
     * @param <T>
     *         the type of the <code>Artifact</code>
     * @return the <code>GraphvizNode</code> constructed to represent the <code>artifact</code> itself
     */
    private <T extends Artifact<T>> GraphvizNode constructGraph(Artifact<T> artifact,
                                                                Function<Artifact<T>, String> getLabel,
                                                                GraphvizGraph graph) {

        GraphvizNode node = graph.node();
        boolean isConflict = artifact.isConflict();

        if (isConflict || artifact.isChoice()) {
            String label = isConflict ? "Conflict" : "Choice";
            String color = isConflict ? "red" : "blue";

            node.attribute("label", label).attribute("fillcolor", color).attribute("style", "filled");

            if (isConflict) {

                for (T side : Arrays.asList(artifact.getLeft(), artifact.getRight())) {
                    GraphvizNode conflictSideNode = constructGraph(side, getLabel, graph);
                    GraphvizEdge edge = graph.edge(node, conflictSideNode);

                    edge.attribute("label", side.getRevision().toString());
                }
            } else {

                for (Map.Entry<String, T> entry : artifact.getVariants().entrySet()) {
                    String condition = entry.getKey();
                    T variant = entry.getValue();

                    GraphvizNode variantNode = constructGraph(variant, getLabel, graph);
                    GraphvizEdge edge = graph.edge(node, variantNode);

                    edge.attribute("label", condition);
                }
            }
        } else {
            String label = String.format("(%d) %s", artifact.getNumber(), getLabel.apply(artifact));

            node.attribute("label", label);

            if (artifact.hasMatches()) {
                node.attribute("fillcolor", "green").attribute("style", "filled");
            }

            for (T child : artifact.getChildren()) {
                GraphvizNode childNode = constructGraph(child, getLabel, graph);

                graph.edge(node, childNode);
            }
        }

        return node;
    }

    @Override
    public <T extends Artifact<T>> String dump(Artifact<T> artifact, Function<Artifact<T>, String> getLabel) {
        GraphvizGraph graph = new GraphvizGraph(false, DIGRAPH);

        graph.attributeStmt(NODE).attribute("shape", "ellipse");
        graph.attribute("nodesep", "0.8");

        constructGraph(artifact, getLabel, graph);

        return graph.dump();
    }
}
