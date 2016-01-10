package de.fosd.jdime.strdump;

import java.util.function.Function;

import de.fosd.jdime.common.Artifact;

/**
 * Implementations of this class dump <code>Artifact</code> (trees) to a <code>String</code>.
 */
public interface StringDumper {

    /**
     * Dumps the given <code>artifact</code> to a <code>String</code>.
     *
     * @param artifact
     *         the artifact to dump
     * @param getLabel
     *         the function to use for producing labels for artifacts
     * @param <T>
     *         the type of the artifact
     * @return the <code>String</code> representation
     */
    <T extends Artifact<T>> String dump(Artifact<T> artifact, Function<Artifact<T>, String> getLabel);
}
