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

import de.fosd.jdime.common.FileArtifact;
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
public class DirectoryStrategy extends MergeStrategy<FileArtifact> {

	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(DirectoryStrategy.class);
	
	/**
	 * 
	 */
	private static Merge<FileArtifact> merge = null;

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
		assert (context.isRecursive()) : "Recursive merging needs to "
				+ "be enabled in order to merge directories. "
				+ "Use '-r' or see '-help'!";

		MergeTriple<FileArtifact> triple = operation.getMergeTriple();

		assert (triple.isValid());

		assert (triple.getLeft() instanceof FileArtifact);
		assert (triple.getBase() instanceof FileArtifact);
		assert (triple.getRight() instanceof FileArtifact);

		FileArtifact left = triple.getLeft();
		FileArtifact base = triple.getBase();
		FileArtifact right = triple.getRight();

		FileArtifact[] revisions = { left, base, right };

		for (FileArtifact dir : revisions) {
			assert ((dir.exists() && dir.isDirectory()) || dir.isEmptyDummy());
		}
		
		if (merge == null) {
			merge = new Merge<FileArtifact>();
		}
		
		if (LOG.isTraceEnabled()) {
			LOG.trace("merge(operation, context)");
		}
		
		merge.merge(operation, context);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final Stats createStats() {
		return new Stats(new String[] { "directories", "files" });
	}

	@Override
	public final String toString() {
		return "directory";
	}

	@Override
	public final String getStatsKey(final FileArtifact artifact) {
		return artifact.isDirectory() ? "directories" : "files";
	}

	@Override
	public final void dump(final FileArtifact artifact, final boolean graphical)
			throws IOException {
		throw new NotYetImplementedException();
	}

}
