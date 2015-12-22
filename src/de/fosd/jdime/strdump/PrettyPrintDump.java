package de.fosd.jdime.strdump;

import de.fosd.jdime.common.Artifact;

/**
 * Dumps an <code>Artifact</code> using its {@link Artifact#prettyPrint()} method.
 *
 * @param <T>
 *         the type of the <code>Artifact</code>
 */
public class PrettyPrintDump<T extends Artifact<T>> extends StringDumper<T> {

    /**
     * Constructs a new <code>StringDumper</code> for the given <code>Artifact</code>.
     *
     * @param artifact
     *         the <code>Artifact</code> to dump to a <code>String</code>
     */
    public PrettyPrintDump(T artifact) {
        super(artifact);
    }

    @Override
    protected void buildString() {
        builder.append(artifact.prettyPrint());
    }
}
