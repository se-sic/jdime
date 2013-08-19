/*******************************************************************************
 * Copyright (c) 2013 Olaf Lessenich.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Olaf Lessenich - initial API and implementation
 ******************************************************************************/
/**
 * 
 */
package de.fosd.jdime.strategy;

import java.io.IOException;

import org.apache.log4j.Logger;

import de.fosd.jdime.common.ASTNodeArtifact;
import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeTriple;
import de.fosd.jdime.common.NotYetImplementedException;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.matcher.ASTMatcher;
import de.fosd.jdime.matcher.Matching;
import de.fosd.jdime.stats.Stats;

/**
 * Performs a structured merge.
 * 
 * @author Olaf Lessenich
 * 
 */
public class StructuredStrategy extends MergeStrategy<FileArtifact> {

	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger
			.getLogger(StructuredStrategy.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.strategy.MergeStrategy#merge(
	 * de.fosd.jdime.common.operations.MergeOperation,
	 * de.fosd.jdime.common.MergeContext)
	 */
	@Override
	public final void merge(final MergeOperation<FileArtifact> operation,
			final MergeContext context) throws IOException,
			InterruptedException {

		assert (operation != null);
		assert (context != null);

		MergeTriple<FileArtifact> triple = operation.getMergeTriple();

		assert (triple != null);
		assert (triple.isValid()) : "The merge triple is not valid!";
		assert (triple.getLeft() instanceof FileArtifact);
		assert (triple.getBase() instanceof FileArtifact);
		assert (triple.getRight() instanceof FileArtifact);

		assert (triple.getLeft().exists() && !triple.getLeft().isDirectory());
		assert ((triple.getBase().exists() && !triple.getBase().isDirectory()) || triple
				.getBase().isEmptyDummy());
		assert (triple.getRight().exists() && !triple.getRight().isDirectory());

		context.resetStreams();

		FileArtifact target = null;

		if (operation.getTarget() != null) {
			assert (operation.getTarget() instanceof FileArtifact);
			target = (FileArtifact) operation.getTarget();
			assert (!target.exists() || target.isEmpty()) : "Would be overwritten: "
					+ target;
		}

		// ASTNodeArtifacts are created from the input files.
		// Then, a ASTNodeStrategy can be applied.
		// The Result is pretty printed and can be written into the output file.

		ASTNodeArtifact left, base, right;

		left = new ASTNodeArtifact(triple.getLeft());
		base = new ASTNodeArtifact(triple.getBase());
		right = new ASTNodeArtifact(triple.getRight());

		ASTNodeArtifact targetNode = left.createEmptyDummy();

		MergeTriple<ASTNodeArtifact> nodeTriple = new MergeTriple<ASTNodeArtifact>(
				triple.getMergeType(), left, base, right);

		if (!base.isEmptyDummy()) {
			// 3-way merge

			// diff base left
			Matching m = ASTMatcher.match(base, left);
			ASTMatcher.storeMatching(m);

			// diff base right

		}

		// diff left right
		diff(left, right);

		MergeOperation<ASTNodeArtifact> astMergeOp = new MergeOperation<ASTNodeArtifact>(
				nodeTriple, targetNode);

		astMergeOp.apply(context);

		if (context.hasErrors()) {
			System.err.println(context.getStdErr());
		}

		// write output
		if (target != null) {
			assert (target.exists());
			target.write(context.getStdIn());
		}

		// FIXME: remove me when implementation is complete!
		throw new NotYetImplementedException(
				"StructuredStrategy: Implement me!");
	}

	private final Matching diff(ASTNodeArtifact left, ASTNodeArtifact right) {
		ASTMatcher.reset();
		LOG.trace(left.getRevision() + ".size = " + left.getTreeSize());
		LOG.trace(right.getRevision() + ".size = " + right.getTreeSize());
		LOG.debug("Compute match(" + left.getRevision() + ", "
				+ right.getRevision() + ")");
		Matching m = ASTMatcher.match(left, right);
		LOG.debug("match(" + left.getRevision() + ", " + right.getRevision()
				+ ") = " + m.getScore());
		LOG.trace(ASTMatcher.getLog());
		LOG.debug("Store matching information within nodes.");
		ASTMatcher.storeMatching(m);
		
		if (LOG.isTraceEnabled()) {
			LOG.trace(left.getRevision() + ".dumpTree():");
			System.out.println(left.dumpTree());
			System.out.println();
			LOG.trace(right.getRevision() + ".dumpTree():");
			System.out.println(right.dumpTree());
		}
		return m;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.strategy.MergeStrategy#toString()
	 */
	@Override
	public final String toString() {
		return "structured";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.strategy.StatsInterface#createStats()
	 */
	@Override
	public final Stats createStats() {
		return new Stats(new String[] { "directories", "files", "nodes" });
	}

	@Override
	public final String getStatsKey(final FileArtifact artifact) {
		// FIXME: remove me when implementation is complete!
		throw new NotYetImplementedException(
				"StructuredStrategy: Implement me!");

	}

	@Override
	public final void dump(final FileArtifact artifact, final boolean graphical)
			throws IOException {
		new ASTNodeStrategy().dump(new ASTNodeArtifact(artifact), graphical);
	}

}
