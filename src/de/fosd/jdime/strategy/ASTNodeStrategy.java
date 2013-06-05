/**
 * 
 */
package de.fosd.jdime.strategy;

import java.io.IOException;

import de.fosd.jdime.common.ASTNodeArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.NotYetImplementedException;
import de.fosd.jdime.common.operations.MergeOperation;

/**
 * @author Olaf Lessenich
 * 
 */
public class ASTNodeStrategy extends MergeStrategy<ASTNodeArtifact> {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.fosd.jdime.strategy.MergeInterface#merge(
	 * de.fosd.jdime.common.operations
	 * .MergeOperation, de.fosd.jdime.common.MergeContext)
	 */
	@Override
	public final void merge(final MergeOperation<ASTNodeArtifact> operation,
			final MergeContext context) 
					throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
		throw new NotYetImplementedException(
				"ASTNodeStrategy: Implement me!");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.strategy.MergeStrategy#toString()
	 */
	@Override
	public final String toString() {
		// TODO Auto-generated method stub
		return "astnode";
	}

}
