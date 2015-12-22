package de.fosd.jdime.strdump;

import java.util.function.Function;

import de.fosd.jdime.common.Artifact;

/**
 * Implementations of this class dump <code>Artifact</code> (trees) to a <code>String</code> that can be retrieved
 * using the {@link #toString()} method.
 *
 * @param <T>
 *         the type of the <code>Artifact</code>
 */
public abstract class StringDumper<T extends Artifact<T>> {

    protected Function<T, String> getLabel;
    protected StringBuilder builder;
    protected T artifact;

    /**
     * Constructs a new <code>StringDumper</code> for the given <code>Artifact</code>.
     *
     * @param artifact
     *         the <code>Artifact</code> to dump to a <code>String</code>
     */
    public StringDumper(T artifact) {
        this.getLabel = Artifact::toString;
        this.builder = new StringBuilder();
        this.artifact = artifact;
    }

    /**
     * Appends the <code>String</code> representation of the <code>artifact</code> to the <code>builder</code>.
     */
    protected abstract void buildString();

    /**
     * Sets the function used to generate a label used for the <code>Artifacts</code> being dumped. Defaults to
     * {@link Artifact#toString()}.
     *
     * @param getLabel
     *         the new labeling function
     */
    public void setGetLabel(Function<T, String> getLabel) {
        this.getLabel = getLabel;
    }

    @Override
    public final String toString() {
        builder.delete(0, builder.length());
        buildString();

        return builder.toString();
    }
}
