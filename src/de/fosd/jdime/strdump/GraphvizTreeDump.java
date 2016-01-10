package de.fosd.jdime.strdump;

import java.util.function.Function;

import de.fosd.jdime.common.Artifact;

/**
 * Dumps the given <code>Artifact</code> tree in Graphviz format.
 *
 * @see <href link="http://www.graphviz.org/">Graphviz</href>
 */
public class GraphvizTreeDump implements StringDumper {

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
    private <T extends Artifact<T>> void dumpGraphvizTree(StringBuilder builder, Artifact<T> artifact,
                                                          Function<Artifact<T>, String> getLabel,
                                                          boolean includeNumbers, int virtualcount) {

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
                dumpGraphvizTree(builder, left, getLabel, includeNumbers, virtualcount);
                builder.append(virtualId).append("->").append(getGraphvizId(left, getLabel)).
                        append("[label=\"").append(left.getRevision()).append("\"]").append(";").append(ls);

                // right alternative
                dumpGraphvizTree(builder, right, getLabel, includeNumbers, virtualcount);
                builder.append(virtualId).append("->").append(getGraphvizId(right, getLabel)).
                        append("[label=\"").append(right.getRevision()).append("\"]").append(";").append(ls);
            } else {

                // choice node
                for (String condition : artifact.getVariants().keySet()) {
                    T variant = artifact.getVariants().get(condition);

                    dumpGraphvizTree(builder, variant, getLabel, includeNumbers, virtualcount);
                    builder.append(virtualId).append("->").append(getGraphvizId(variant, getLabel)).
                            append("[label=\"").append(condition).append("\"]").append(";").append(ls);
                }
            }
        } else {
            builder.append(getGraphvizId(artifact, getLabel)).append("[label=\"");

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
                String childId = getGraphvizId(child, getLabel);

                if (child.isConflict() || child.isChoice()) {
                    virtualcount++;
                    childId = "\"c" + virtualcount + "\"";
                }

                dumpGraphvizTree(builder, child, getLabel, includeNumbers, virtualcount);

                // edge
                builder.append(getGraphvizId(artifact, getLabel)).append("->").append(childId).append(";").append(ls);
            }
        }
    }

    private <T extends Artifact<T>> String getGraphvizId(Artifact<T> artifact, Function<Artifact<T>, String> getLabel) {
        return "\"" + getLabel.apply(artifact) + "\"";
    }

    @Override
    public <T extends Artifact<T>> String dump(Artifact<T> artifact, Function<Artifact<T>, String> getLabel) {
        StringBuilder builder = new StringBuilder();
        String ls = System.lineSeparator();

        builder.append("digraph ast {").append(ls);
        builder.append("node [shape=ellipse];").append(ls);
        builder.append("nodesep=0.8;").append(ls);

        dumpGraphvizTree(builder, artifact, getLabel, true, 0);

        builder.append("}").append(ls);

        return builder.toString();
    }
}
