package de.fosd.jdime.strdump.graphviz;

public enum GraphvizGraphType {
    GRAPH("--"),
    DIGRAPH("->");

    GraphvizGraphType(String edgeOp) {
        this.edgeOp = edgeOp;
    }

    final String edgeOp;
}
