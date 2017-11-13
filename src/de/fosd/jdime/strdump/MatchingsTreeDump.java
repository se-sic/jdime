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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.artifact.Artifacts;
import de.fosd.jdime.matcher.matching.Matching;
import de.fosd.jdime.matcher.matching.Matchings;
import de.fosd.jdime.strdump.graphviz.GraphvizEdge;
import de.fosd.jdime.strdump.graphviz.GraphvizGraph;
import de.fosd.jdime.strdump.graphviz.GraphvizNode;
import de.fosd.jdime.strdump.graphviz.GraphvizSubGraph;

import static de.fosd.jdime.strdump.graphviz.GraphvizAttributeStmtType.EDGE;
import static de.fosd.jdime.strdump.graphviz.GraphvizAttributeStmtType.NODE;
import static de.fosd.jdime.strdump.graphviz.GraphvizGraphType.GRAPH;

public class MatchingsTreeDump {

    /**
     * Converts the two <code>Artifact</code> trees being matched by <code>matchings</code> to a
     * <code>GraphvizGraph</code> visualizing the trees and the matchings between them. It is assumed that all left
     * and right sides of all <code>Matching</code>s are from the same (left and right) tree.
     *
     * @param matchings
     *         the <code>Matchings</code> to convert
     * @param <T>
     *         the type of the <code>Artifact</code>s
     * @return the resulting <code>GraphvizGraph</code> that can be dumped
     */
    public <T extends Artifact<T>> GraphvizGraph toGraphvizGraph(Matchings<T> matchings) {

        if (matchings.isEmpty()) {
            return new GraphvizGraph(false, GRAPH);
        }

        T lRoot, rRoot;

        {
            Matching<T> first = matchings.iterator().next();

            lRoot = Artifacts.root(first.getLeft());
            rRoot = Artifacts.root(first.getRight());
        }

        return toGraphvizGraph(matchings, lRoot, rRoot);
    }

    /**
     * Converts the two given <code>Artifact</code> trees and the matchings between elements of the trees to a
     * <code>GraphvizGraph</code> visualizing them.
     *
     * @param matchings
     *         the <code>Matchings</code> between elements of the two trees
     * @param lRoot
     *         the root of the left tree
     * @param rRoot
     *         the root of the right tree
     * @param <T>
     *         the type of the <code>Artifact</code>s
     * @return the resulting <code>GraphvizGraph</code> that can be dumped
     */
    public <T extends Artifact<T>> GraphvizGraph toGraphvizGraph(Matchings<T> matchings, T lRoot, T rRoot) {
        GraphvizGraph graph = new GraphvizGraph(false, GRAPH);

        graph.attributeStmt(NODE).attribute("shape", "box");
        graph.attribute("rankdir", "TB");
        graph.attribute("compound", "true");

        Map<T, GraphvizNode> lNodes = addSubGraph(lRoot, "Left", graph);
        Map<T, GraphvizNode> rNodes = addSubGraph(rRoot, "Right", graph);

        GraphvizNode lRootNode = lNodes.get(lRoot);
        GraphvizNode rRootNode = rNodes.get(rRoot);
        GraphvizEdge align = graph.edge(lRootNode, rRootNode);

        align.attribute("ltail", lRootNode.getGraph().getId());
        align.attribute("lhead", rRootNode.getGraph().getId());
        align.attribute("style", "invis");
        align.attribute("constraint", "false");

        for (Matching<T> matching : matchings) {
            GraphvizNode from = lNodes.get(matching.getLeft());
            GraphvizNode to = rNodes.get(matching.getRight());

            if (from == null || to == null) {
                continue;
            }

            graph.edge(from, to).attribute("constraint", "false");
        }

        return graph;
    }

    /**
     * Adds a clustered sub-graph containing nodes for all for all artifacts in the tree under <code>root</code> and
     * appropriate edges between them.
     *
     * @param root
     *         the root of the tree to add as a <code>GraphvizSubGraph</code>
     * @param label
     *         the label for the <code>GraphvizSubGraph</code>
     * @param graph
     *         the <code>GraphvizGraph</code> to add the <code>GraphvizSubGraph</code> to
     * @param <T>
     *         the type of the <code>Artifact</code>s
     * @return the <code>GraphvizNode</code>s that were added for the tree under <code>root</code>
     */
    private <T extends Artifact<T>> Map<T, GraphvizNode> addSubGraph(T root, String label, GraphvizGraph graph) {
        GraphvizSubGraph subGraph = graph.subGraphCluster();

        subGraph.attribute("label", label);
        subGraph.attributeStmt(EDGE).attribute("color", toHexString(0, 0, 0, 100));

        List<T> bfs = Artifacts.bfs(root);
        Map<T, GraphvizNode> nodes = new HashMap<>();

        for (T artifact : bfs) {
            GraphvizNode node = subGraph.node();

            nodes.put(artifact, node);
            node.attribute("label", artifact.toString());
        }

        for (T artifact : bfs) {
            GraphvizNode fromNode = nodes.get(artifact);

            for (T c : artifact.getChildren()) {
                subGraph.edge(fromNode, nodes.get(c));
            }
        }

        // now we force dot to respect the order of the children
        for (T artifact : bfs) {
            List<T> ch = artifact.getChildren();

            if (ch.size() > 1) {
                GraphvizSubGraph oSubGraph = subGraph.subGraph();

                oSubGraph.attributeStmt(EDGE).attribute("style", "invis");
                oSubGraph.attribute("rank", "same");

                Iterator<T> it = ch.iterator();

                GraphvizNode last = nodes.get(it.next());

                while (it.hasNext()) {
                    GraphvizNode next = nodes.get(it.next());

                    oSubGraph.edge(last, next);
                    last = next;
                }
            }
        }

        return nodes;
    }

    /**
     * Constructs a hex color <code>String</code> from the given color components.
     *
     * @param r
     *         the red component
     * @param g
     *         the green component
     * @param b
     *         the blue component
     * @param a
     *         the alpha component
     * @return the hex color <code>String</code>
     */
    private String toHexString(int r, int g, int b, int a) {
        return String.format("#%02x%02x%02x%02x", r, g, b, a);
    }
}
