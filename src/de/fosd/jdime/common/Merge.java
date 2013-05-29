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
package de.fosd.jdime.common;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import de.fosd.jdime.Main;
import de.fosd.jdime.engine.EngineNotFoundException;
import de.fosd.jdime.engine.MergeEngine;

/**
 * @author Olaf Lessenich
 * 
 */
public final class Merge {

	/**
	 * 
	 */
	private Merge() {
	}

	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(Merge.class);

	/**
	 * Performs a merge on files or directories.
	 * 
	 * @param mergeType
	 *            type of merge
	 * @param engine
	 *            merge engine
	 * @param inputArtifacts
	 *            input files
	 * @return list of merge reports
	 * @throws EngineNotFoundException
	 *             if merge engine cannot be found
	 * @throws IOException
	 *             IOException
	 * @throws InterruptedException
	 *             InterruptedException
	 * @throws UnsupportedMergeTypeException
	 *             UnsupportedMergeTypeException
	 */
	public static List<MergeReport> merge(final MergeType mergeType,
			final MergeEngine engine, final ArtifactList inputArtifacts)
			throws EngineNotFoundException, IOException, InterruptedException,
			UnsupportedMergeTypeException {
		LOG.setLevel(Main.getLogLevel());
		LOG.debug(Merge.class.getName());
		LOG.debug(mergeType.name() + " merge will be performed.");

		List<MergeReport> reports = new LinkedList<MergeReport>();
		OperationList operations = calculateOperations(mergeType,
				inputArtifacts, 0, "");

		for (Operation operation : operations) {
			if (LOG.isDebugEnabled()) {
				LOG.debug(operation);
			}
			if (operation instanceof MergeOperation) {
				// FIXME: maybe this can be done nicer with a visitor pattern
				reports.add(engine.merge(mergeType,
						((MergeOperation) operation).getMergeTriple()));
			} else {
				// TODO
				throw new UnsupportedOperationException();
			}
		}

		return reports;
	}

	/**
	 * Extracts the operations that are needed for the merge.
	 * 
	 * @param mergeType
	 *            type of merge
	 * @param inputArtifacts
	 *            input files
	 * @param depth
	 *            recursion depth
	 * @param commonPath
	 *            common path for the merged file
	 * @return list of operations
	 * @throws FileNotFoundException
	 *             FileNotFoundException
	 * @throws UnsupportedMergeTypeException
	 *             UnsupportedMergeTypeException
	 */
	private static OperationList calculateOperations(final MergeType mergeType,
			final ArtifactList inputArtifacts, final int depth,
			final String commonPath) throws FileNotFoundException,
			UnsupportedMergeTypeException {
		OperationList operations = new OperationList();

		Artifact left, base, right;

		if (mergeType == MergeType.TWOWAY) {
			left = inputArtifacts.get(0);
			base = Artifact.createEmptyArtifact();
			right = inputArtifacts.get(1);
		} else if (mergeType == MergeType.THREEWAY) {
			left = inputArtifacts.get(0);
			base = inputArtifacts.get(1);
			right = inputArtifacts.get(2);
		} else {
			throw new UnsupportedMergeTypeException();
		}

		boolean isDirectory = inputArtifacts.get(0).isDirectory();

		if (!isDirectory) {
			// easiest case: files only - just add a merge operation for them!
			if (LOG.isDebugEnabled()) {
				LOG.debug("Input artifacts are files.");
			}
			MergeTriple triple = new MergeTriple(left, base, right);
			operations.add(new MergeOperation(mergeType, triple));
		} else {
			// we are merging directories. we need to apply the standard
			// three-way merge rules to the content of the directories.
			
			/* TODO/FIXME: The following code is no good.
			 * Better would be:
			 * 	1. hashmap (key: relative path; value: bitset for revisions)
			 *  2. operationlist from hashmap
			 *  3. visiting operations  
			 */
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("Input artifacts are directories.");
			}

			if (mergeType == MergeType.TWOWAY) {
				;
			} else if (mergeType == MergeType.THREEWAY) {
				assert (left.isDirectory() && base.isDirectory() && right
						.isDirectory());

				ArtifactList leftContent = left.getContent();
				ArtifactList baseContent = base.getContent();
				ArtifactList rightContent = right.getContent();

				if (LOG.isDebugEnabled()) {
					LOG.debug("Traversing the content of base revision:");
				}

				for (Artifact artifact : baseContent) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("\t* "
								+ Artifact.computeRelativePath(artifact, base));
					}

					if (leftContent.containsRelative(artifact)) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("\t\t(found in left)");
						}
						if (rightContent.containsRelative(artifact)) {
							if (LOG.isDebugEnabled()) {
								LOG.debug("\t\t(found in right)");
							}
						}
					}
				}
			}

			// TODO
			throw new UnsupportedOperationException();
		}

		return operations;
	}
}
