/**
 * 
 */
package de.fosd.jdime.strategy;

import java.io.IOException;

import org.apache.log4j.Logger;

import de.fosd.jdime.common.ASTNodeArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeTriple;
import de.fosd.jdime.common.NotYetImplementedException;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.merge.Merge;
import de.fosd.jdime.stats.Stats;

/**
 * @author Olaf Lessenich
 * 
 */
public class ASTNodeStrategy extends MergeStrategy<ASTNodeArtifact> {
	
	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger
			.getLogger(ASTNodeStrategy.class);
	
	/**
	 * 
	 */
	private static Merge<ASTNodeArtifact> merge = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.strategy.MergeInterface#merge(
	 * de.fosd.jdime.common.operations .MergeOperation,
	 * de.fosd.jdime.common.MergeContext)
	 */
	@Override
	public final void merge(final MergeOperation<ASTNodeArtifact> operation,
			final MergeContext context) throws IOException,
			InterruptedException {
		assert (operation != null);
		assert (context != null);

		MergeTriple<ASTNodeArtifact> triple = operation.getMergeTriple();

		assert (triple.isValid());

		ASTNodeArtifact left = triple.getLeft();
		ASTNodeArtifact base = triple.getBase();
		ASTNodeArtifact right = triple.getRight();
		ASTNodeArtifact target = operation.getTarget();

		ASTNodeArtifact[] revisions = { left, base, right };

		for (ASTNodeArtifact node : revisions) {
			assert (node.exists());
		}

		assert (target != null);
		
		if (merge == null) {
			merge = new Merge<ASTNodeArtifact>();
		}
		
		if (LOG.isTraceEnabled()) {
			LOG.trace("merge(operation, context)");
		}
		
		merge.merge(operation, context);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.strategy.MergeStrategy#toString()
	 */
	@Override
	public final String toString() {
		return "astnode";
	}

	@Override
	public final Stats createStats() {
		return new Stats(new String[] { "nodes" });
	}

	@Override
	public final String getStatsKey(final ASTNodeArtifact artifact) {
		// FIXME: remove me when implementation is complete
		throw new NotYetImplementedException("ASTNodeStrategy: Implement me!");
	}

	@Override
	public final void dump(final ASTNodeArtifact artifact,
			final boolean graphical) throws IOException {
		if (graphical) {
			dumpGraphVizTree(artifact);
		} else {
			System.out.println(artifact.dumpTree());
		}
	}

	/**
	 * @param artifact
	 *            artifact that should be printed
	 */
	private void dumpGraphVizTree(final ASTNodeArtifact artifact) {
		// header
		StringBuffer sb = new StringBuffer();
		sb.append("digraph ast {" + System.lineSeparator());
		sb.append("node [shape=ellipse];" + System.lineSeparator());
		sb.append("nodesep=0.8;" + System.lineSeparator());

		// nodes
		sb.append(artifact.dumpGraphvizTree(true));

		// footer
		sb.append("}");

		System.out.println(sb.toString());
	}
}
