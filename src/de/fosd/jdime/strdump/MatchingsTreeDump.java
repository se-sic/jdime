package de.fosd.jdime.strdump;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.Artifacts;
import de.fosd.jdime.matcher.matching.Matching;
import de.fosd.jdime.matcher.matching.Matchings;
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

        Map<T, GraphvizNode> lNodes = addSubGraph(lRoot, "Left", graph);
        Map<T, GraphvizNode> rNodes = addSubGraph(rRoot, "Right", graph);

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
            node.attribute("label", artifact.getId());
        }

        for (T artifact : bfs) {
            GraphvizNode fromNode = nodes.get(artifact);

            for (T c : artifact.getChildren()) {
                subGraph.edge(fromNode, nodes.get(c));
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
