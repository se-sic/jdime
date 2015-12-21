package de.fosd.jdime.strdump;

import de.fosd.jdime.common.Artifact;

/**
 * Dumps the given <code>Artifact</code> in Graphviz format.
 *
 * @param <T>
 *         the type of the <code>Artifact</code>
 * @see <href link="http://www.graphviz.org/">Graphviz</href>
 */
public class GraphvizTreeDump<T extends Artifact<T>> extends StringDumper<T> {

    /**
     * Constructs a new <code>StringDumper</code> for the given <code>Artifact</code>.
     *
     * @param artifact
     *         the <code>Artifact</code> to dump to a <code>String</code>
     */
    public GraphvizTreeDump(T artifact) {
        super(artifact);
    }

    /**
     * Appends the dot-format representation of the given <code>artifact</code> and its children to the
     * <code>builder</code>.
     *
     * @param artifact
     *         the <code>Artifact</code> to dump
     * @param includeNumbers
     *         whether to include the node number in the label
     * @param virtualcount
     *         recursive count of the nodes
     */
    private void dumpGraphvizTree(T artifact, boolean includeNumbers, int virtualcount) {
        boolean isConflict = artifact.isConflict();
        String ls = System.lineSeparator();

        if (isConflict || artifact.isChoice()) {
            
            // insert virtual node
            String virtualId = "\"c" + virtualcount + "\"";
            String virtualLabel = isConflict ? "\"Conflict\"" : "\"Choice\"";
            String virtualColor = isConflict ? "red" : "blue";
            
            builder.append(virtualId);
            builder.append("[label=").append(virtualLabel);
            builder.append(", fillcolor = ").append(virtualColor);
            builder.append(", style = filled]").append(ls);

            if (isConflict) {
                T left = artifact.getLeft();
                T right = artifact.getRight();

                // left alternative
                dumpGraphvizTree(left, includeNumbers, virtualcount);
                builder.append(virtualId).append("->").append(getGraphvizId(left)).
                        append("[label=\"").append(left.getRevision()).append("\"]").append(";").append(ls);

                // right alternative
                dumpGraphvizTree(right, includeNumbers, virtualcount);
                builder.append(virtualId).append("->").append(getGraphvizId(right)).
                        append("[label=\"").append(right.getRevision()).append("\"]").append(";").append(ls);
            } else {

                // choice node
                for (String condition : artifact.getVariants().keySet()) {
                    T variant = artifact.getVariants().get(condition);

                    dumpGraphvizTree(variant, includeNumbers, virtualcount);
                    builder.append(virtualId).append("->").append(getGraphvizId(variant)).
                            append("[label=\"").append(condition).append("\"]").append(";").append(ls);
                }
            }
        } else {
            builder.append(getGraphvizId(artifact)).append("[label=\"");

            // node label
            if (includeNumbers) {
                builder.append("(").append(artifact.getNumber()).append(") ");
            }

            builder.append(getLabel.apply(artifact));

            builder.append("\"");

            if (artifact.hasMatches()) {
                builder.append(", fillcolor = green, style = filled");
            }

            builder.append("];");
            builder.append(ls);

            // children
            for (T child : artifact.getChildren()) {
                String childId = getGraphvizId(child);

                if (child.isConflict() || child.isChoice()) {
                    virtualcount++;
                    childId = "\"c" + virtualcount + "\"";
                }

                dumpGraphvizTree(child, includeNumbers, virtualcount);

                // edge
                builder.append(getGraphvizId(artifact)).append("->").append(childId).append(";").append(ls);
            }
        }
    }

    private String getGraphvizId(T artifact) {
        return "\"" + getLabel.apply(artifact) + "\"";
    }
    
    @Override
    protected void buildString() {
        String ls = System.lineSeparator();

        builder.append("digraph ast {").append(ls);
        builder.append("node [shape=ellipse];").append(ls);
        builder.append("nodesep=0.8;").append(ls);

        dumpGraphvizTree(artifact, true, 0);

        builder.append("}").append(ls);
    }
}
