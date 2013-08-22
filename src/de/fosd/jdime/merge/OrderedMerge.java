/**
 * 
 */
package de.fosd.jdime.merge;

import java.io.IOException;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.operations.MergeOperation;

/**
 * @author Olaf Lessenich
 * 
 * @param <T> type of artifact
 */
public class OrderedMerge<T extends Artifact<T>> implements MergeInterface<T> {

	@Override
	public final void merge(final MergeOperation<T> operation, 
			final MergeContext context)
			throws IOException, InterruptedException {
		
	}

}
