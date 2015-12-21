package de.fosd.jdime.strdump;

import java.util.Map;
import java.util.Set;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.Revision;
import de.fosd.jdime.matcher.Color;
import de.fosd.jdime.matcher.Matching;

/**
 * Dumps the given <code>Artifact</code> tree as indented plaintext.
 *
 * @param <T>
 *         the type of the <code>Artifact</code>
 */
public class PlaintextTreeDump<T extends Artifact<T>> extends StringDumper<T> {

    /**
     * Constructs a new <code>StringDumper</code> for the given <code>Artifact</code>.
     *
     * @param artifact
     *         the <code>Artifact</code> to dump to a <code>String</code>
     */
    public PlaintextTreeDump(T artifact) {
        super(artifact);
    }

    /**
     * Constructs a new <code>StringDumper</code> for the given <code>Artifact</code>.
     *
     * @param artifact
     *         the <code>Artifact</code> to dump to a <code>String</code>
     * @param findRoot
     *         whether to dump the root of the tree <code>artifact</code> is part of
     */
    public PlaintextTreeDump(T artifact, boolean findRoot) {
        super(findRoot ? findRoot(artifact) : artifact);
    }

    /**
     * Returns the root of the tree <code>artifact</code> is part of.
     *
     * @param artifact
     *         the <code>Artifact</code> whose trees root is to be returned
     * @param <T>
     *         the type of the <code>Artifact</code>
     * @return the root of the tree
     */
    private static <T extends Artifact<T>> T findRoot(T artifact) {
        T current = artifact;

        while (current.getParent() != null) {
            current = current.getParent();
        }

        return current;
    }

    /**
     * Appends the plain-text tree representation of the given <code>artifact</code> and its children to the
     * <code>builder</code>.
     *
     * @param artifact
     *         the <code>Artifact</code> to dump
     * @param indent
     *         the indentation for the current level
     */
    private void dumpTree(T artifact, String indent) {
        String ls = System.lineSeparator();
        Matching<T> m = null;

        if (!artifact.isConflict() && artifact.hasMatches()) {
            String color = "";

            for (Map.Entry<Revision, Matching<T>> entry : artifact.getMatches().entrySet()) {
                m = entry.getValue();
                color = m.getHighlightColor().toShell();
            }

            builder.append(color);
        }

        if (artifact.isConflict()) {
            builder.append(Color.RED.toShell());
            builder.append(indent).append("(").append(artifact.getId()).append(") ");
            builder.append(getLabel.apply(artifact));
            builder.append(ls);
            builder.append(Color.RED.toShell());
            builder.append("<<<<<<< ");
            builder.append(ls);

            T left = artifact.getLeft();
            T right = artifact.getRight();

            // children
            if (left != null) {
                dumpTree(left, indent);
            }

            builder.append(Color.RED.toShell());
            builder.append("======= ");
            builder.append(ls);

            // children
            if (right != null) {
                dumpTree(right, indent);
            }

            builder.append(Color.RED.toShell());
            builder.append(">>>>>>> ");
            builder.append(Color.DEFAULT.toShell());
            builder.append(ls);
        } else if (artifact.isChoice()) {
            Set<String> conditions = artifact.getVariants().keySet();

            builder.append(Color.RED.toShell());
            builder.append(indent).append("(").append(artifact.getId()).append(") ");
            builder.append(getLabel.apply(artifact));
            builder.append(ls);

            for (String condition : conditions) {
                builder.append(Color.RED.toShell());
                builder.append("#ifdef ").append(condition);
                builder.append(ls);

                // children
                T variant = artifact.getVariants().get(condition);

                if (variant != null) {
                    dumpTree(variant, indent);
                }

                builder.append(Color.RED.toShell());
                builder.append("#endif");
                builder.append(Color.DEFAULT.toShell());
                builder.append(ls);

            }
        } else {
            builder.append(indent).append("(").append(artifact.getId()).append(") ");
            builder.append(this);

            if (artifact.hasMatches()) {
                builder.append(" <=> (").append(m.getMatchingArtifact(artifact).getId()).append(")");
                builder.append(Color.DEFAULT.toShell());
            }

            builder.append(ls);

            // children
            for (T child : artifact.getChildren()) {
                dumpTree(child, indent + "  ");
            }
        }
    }

    @Override
    protected void buildString() {
        dumpTree(artifact, "");
    }
}
