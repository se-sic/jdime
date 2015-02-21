/*
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2015 University of Passau, Germany
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 *
 * Contributors:
 *     Olaf Lessenich <lessenic@fim.uni-passau.de>
 */
package de.fosd.jdime.strategy;

import java.io.IOException;

import org.apache.commons.lang3.ClassUtils;
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

	private static final Logger LOG = Logger.getLogger(ClassUtils
			.getShortClassName(ASTNodeStrategy.class));
	/**
     *
     */
	private static Merge<ASTNodeArtifact> merge = null;

	/**
	 * TODO: high-level documentation
	 * @param operation
	 * @param context
	 *
	 * @throws IOException
	 * @throws InterruptedException
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
			merge = new Merge<>();
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
	public final void dumpTree(final ASTNodeArtifact artifact,
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
		StringBuilder sb = new StringBuilder();
		sb.append("digraph ast {").append(System.lineSeparator());
		sb.append("node [shape=ellipse];").append(System.lineSeparator());
		sb.append("nodesep=0.8;").append(System.lineSeparator());

		// nodes
		sb.append(artifact.dumpGraphvizTree(true));

		// footer
		sb.append("}");

		System.out.println(sb.toString());
	}

	@Override
	public void dumpFile(final ASTNodeArtifact artifact, final boolean graphical)
			throws IOException {
		System.out.println(artifact.prettyPrint());
	}
}
