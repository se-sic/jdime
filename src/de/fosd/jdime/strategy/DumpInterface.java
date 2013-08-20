package de.fosd.jdime.strategy;

import java.io.IOException;

import de.fosd.jdime.common.Artifact;

/**
 * @author Olaf Lessenich
 *
 * @param <T>
 */
public interface DumpInterface<T extends Artifact<T>> {
	/**
	 * @param artifact artifact to dump
	 * @param graphical output option
	 * @throws IOException 
	 */
	void dump(final T artifact, final boolean graphical) throws IOException;
}
