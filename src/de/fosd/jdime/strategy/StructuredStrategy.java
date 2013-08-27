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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.log4j.Logger;

import de.fosd.jdime.common.ASTNodeArtifact;
import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeTriple;
import de.fosd.jdime.common.NotYetImplementedException;
import de.fosd.jdime.common.operations.MergeOperation;
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

	private static String errorlog = "/home/lessenic/jdime-errors.log";

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

		if (LOG.isInfoEnabled()) {
			LOG.info("Merging: " + triple.getLeft().getPath() + " "
					+ triple.getBase().getPath() + " "
					+ triple.getRight().getPath());
		}

		left = new ASTNodeArtifact(triple.getLeft());
		base = new ASTNodeArtifact(triple.getBase());
		right = new ASTNodeArtifact(triple.getRight());

		// Output tree
		// Program program = new Program();
		// program.state().reset();
		// ASTNodeArtifact targetNode = new ASTNodeArtifact(program);
		ASTNodeArtifact targetNode = ASTNodeArtifact.createProgram(left);
		targetNode.setRevision(left.getRevision());
		targetNode.forceRenumbering();

		if (LOG.isTraceEnabled()) {
			LOG.trace("target.dumpTree(:");
			System.out.println(targetNode.dumpTree());
		}

		MergeTriple<ASTNodeArtifact> nodeTriple = new MergeTriple<ASTNodeArtifact>(
				triple.getMergeType(), left, base, right);

		MergeOperation<ASTNodeArtifact> astMergeOp = new MergeOperation<ASTNodeArtifact>(
				nodeTriple, targetNode);

		if (LOG.isTraceEnabled()) {
			LOG.trace("ASTMOperation.apply(context)");
		}

		try {
			astMergeOp.apply(context);

			if (LOG.isTraceEnabled()) {
				LOG.trace("Structured merge finished.");
				LOG.trace("target.dumpTree():");
				System.out.println(targetNode.dumpTree());

				LOG.trace("Pretty-printing left:");
				System.out.println(left.prettyPrint());
				LOG.trace("Pretty-printing right:");
				System.out.println(right.prettyPrint());
				LOG.trace("Pretty-printing merge:");
				if (context.isQuiet()) {
					System.out.println(targetNode.prettyPrint());
				}
			}

			context.append(targetNode.prettyPrint());

			if (context.hasErrors()) {
				System.err.println(context.getStdErr());
			}

			// write output
			if (target != null) {
				assert (target.exists());
				target.write(context.getStdIn());
			}

		} catch (Exception e) {
			File errorfile = new File(errorlog);
			assert (errorfile.exists());
			PrintWriter errorprinter = new PrintWriter(new BufferedWriter(new FileWriter(errorfile, true)));
			errorprinter.println(e.toString()+ ": " + triple.getLeft().getPath() + " "
					+ triple.getBase().getPath() + " "
					+ triple.getRight().getPath());
			errorprinter.close();
			System.err.println(e.toString()+ ": " + triple.getLeft().getPath() + " "
					+ triple.getBase().getPath() + " "
					+ triple.getRight().getPath());

		}

		// FIXME: remove me when implementation is complete!
		// throw new NotYetImplementedException(
		// "StructuredStrategy: Implement me!");
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
