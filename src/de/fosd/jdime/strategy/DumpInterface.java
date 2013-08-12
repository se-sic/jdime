package de.fosd.jdime.strategy;

import java.io.FileNotFoundException;
import java.io.IOException;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.operations.MergeOperation;

/**
 * @author Olaf Lessenich
 *
 * @param <T>
 */
public interface DumpInterface<T extends Artifact<T>> {
	/**
	 * @param artifact
	 * @throws IOException 
	 */
	void dump(final T artifact) throws IOException;
}
