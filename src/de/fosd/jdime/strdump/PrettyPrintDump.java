package de.fosd.jdime.strdump;

import java.util.function.Function;

import de.fosd.jdime.common.Artifact;

/**
 * Dumps an <code>Artifact</code> using its {@link Artifact#prettyPrint()} method.
 */
public class PrettyPrintDump implements StringDumper {

    @Override
    public <T extends Artifact<T>> String dump(Artifact<T> artifact, Function<Artifact<T>, String> getLabel) {
        return artifact.prettyPrint();
    }
}
