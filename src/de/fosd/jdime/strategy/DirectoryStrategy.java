/*******************************************************************************
 * Copyright (C) 2013, 2014 Olaf Lessenich.
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
 *******************************************************************************/
package de.fosd.jdime.strategy;

import java.io.IOException;

import org.apache.commons.lang3.ClassUtils;
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

	private static final Logger LOG = Logger.getLogger(ClassUtils
			.getShortClassName(DirectoryStrategy.class));
	private static Merge<FileArtifact> merge = null;

	/**
	 * TODO: high-level documentation
	 *
	 * @param operation
	 * @param context
	 *
	 * @throws IOException
	 * @throws InterruptedException
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
	public final void dumpTree(final FileArtifact artifact,
			final boolean graphical) throws IOException {
		throw new NotYetImplementedException("TODO: print directory tree");
	}

	@Override
	public void dumpFile(final FileArtifact artifact, final boolean graphical)
			throws IOException {
		throw new NotYetImplementedException("TODO: print content of all files");
	}
}
