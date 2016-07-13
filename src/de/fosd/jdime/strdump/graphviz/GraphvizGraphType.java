package de.fosd.jdime.strdump.graphviz;

/**
 * Enumeration of the valid graph types.
 */
public enum GraphvizGraphType {
    GRAPH("--"),
    DIGRAPH("->");

    GraphvizGraphType(String edgeOp) {
        this.edgeOp = edgeOp;
    }

    final String edgeOp;
}
