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
import static de.fosd.jdime.strdump.graphviz.GraphvizGraphType.DIGRAPH;

public class MatchingsTreeDump {

    public <T extends Artifact<T>> GraphvizGraph toGraphvizGraph(Matchings<T> matchings) {
        GraphvizGraph graph = new GraphvizGraph(false, DIGRAPH);

        if (matchings.isEmpty()) {
            return graph;
        }

        T lRoot, rRoot;

        {
            Matching<T> first = matchings.iterator().next();

            lRoot = Artifacts.root(first.getLeft());
            rRoot = Artifacts.root(first.getRight());
        }

        graph.attributeStmt(NODE).attribute("shape", "box");
        graph.attribute("rankdir", "TB");

        Map<T, GraphvizNode> lNodes = addSubGraph(lRoot, "Left", graph);
        Map<T, GraphvizNode> rNodes = addSubGraph(rRoot, "Right", graph);

        for (Matching<T> matching : matchings) {
            GraphvizNode from = lNodes.get(matching.getLeft());
            GraphvizNode to = rNodes.get(matching.getRight());

            graph.edge(from, to).attribute("constraint", "false");
        }

        return graph;
    }

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

    private String toHexString(int r, int g, int b, int a) {
        return String.format("#%02x%02x%02x%02x", r, g, b, a);
    }
}
