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

import java.io.File;
import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import de.fosd.jdime.Main;
import de.fosd.jdime.common.operations.AddOperation;
import de.fosd.jdime.common.operations.DeleteOperation;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.common.operations.Operation;
import de.fosd.jdime.common.operations.OperationList;
import de.fosd.jdime.engine.EngineNotFoundException;
import de.fosd.jdime.engine.MergeEngine;

/**
 * @author Olaf Lessenich
 * 
 */
public final class Merge {

	/**
	 * Silencing checkstyle.
	 */
	private static final int ZERO = 0;

	/**
	 * Silencing checkstyle.
	 */
	private static final int ONE = 1;

	/**
	 * Silencing checkstyle.
	 */
	private static final int TWO = 2;

	/**
	 * Silencing checkstyle.
	 */
	private static final int THREE = 3;

	/**
	 * 
	 */
	private static final int LEFTPOS = 0;

	/**
	 * 
	 */
	private static final int BASEPOS = 1;

	/**
	 * 
	 */
	private static final int RIGHTPOS = 2;

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
	 * @param output
	 *            output artifact
	 * @return list of merge reports
	 * @throws EngineNotFoundException
	 *             if merge engine cannot be found
	 * @throws IOException
	 *             IOException
	 * @throws InterruptedException
	 *             InterruptedException
	 * @throws UnsupportedMergeTypeException
	 *             UnsupportedMergeTypeException
	 * @throws NotYetImplementedException
	 *             NotYetImplementedException
	 */
	public static List<MergeReport> merge(final MergeType mergeType,
			final MergeEngine engine, final ArtifactList inputArtifacts,
			final Artifact output) throws EngineNotFoundException, IOException,
			InterruptedException, UnsupportedMergeTypeException,
			NotYetImplementedException {
		LOG.setLevel(Main.getLogLevel());
		LOG.debug(Merge.class.getName());
		LOG.debug(mergeType.name() + " merge will be performed.");

		List<MergeReport> reports = new LinkedList<MergeReport>();
		OperationList operations = calculateOperations(mergeType, engine,
				inputArtifacts, output);

		for (Operation operation : operations) {
			if (LOG.isDebugEnabled()) {
				LOG.debug(operation.description());
			}
			reports.add(operation.apply());

		}

		return reports;
	}

	/**
	 * Extracts the operations that are needed for the merge.
	 * 
	 * @param mergeType
	 *            type of merge
	 * @param engine
	 *            merge engine
	 * @param inputArtifacts
	 *            input files
	 * @param output
	 *            output
	 * @return list of operations
	 * @throws UnsupportedMergeTypeException
	 *             UnsupportedMergeTypeException
	 * @throws IOException If an input output exception occurs
	 */
	private static OperationList calculateOperations(final MergeType mergeType,
			final MergeEngine engine, final ArtifactList inputArtifacts,
			final Artifact output) throws UnsupportedMergeTypeException, 
			IOException {
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

		left.setRevision(new Revision("left"));
		base.setRevision(new Revision("base"));
		right.setRevision(new Revision("right"));

		Artifact[] revisions = { left, base, right };

		boolean isDirectory = left.isDirectory();

		if (!isDirectory) {
			// To merge files, we just have to create a merge operation.

			if (LOG.isDebugEnabled()) {
				LOG.debug("Input artifacts are files.");
			}

			MergeTriple triple = new MergeTriple(left, base, right);
			Artifact.createFile(output, false);
			operations
					.add(new MergeOperation(mergeType, triple, engine, output));
		} else {
			// To merge directories, we need to apply the standard three-way
			// merge rules to the content of the directories.

			for (Artifact artifact : revisions) {
				assert (artifact.isDirectory() || artifact.isEmptyDummy());
			}

			if (LOG.isDebugEnabled()) {
				LOG.debug("Input artifacts are directories.");
			}

			HashMap<String, BitSet> filemap = new HashMap<String, BitSet>();

			// The revisions in which each file exists, are stored in filemap.
			for (int i = 0; i < revisions.length; i++) {
				Artifact directory = revisions[i];

				if (directory.isEmptyDummy()) {
					// base is a dummy for 2-way merges
					continue;
				}

				List<String> content = directory.getRelativeContent();

				for (String file : content) {
					BitSet bs;

					if (filemap.containsKey(file)) {
						bs = filemap.get(file);
					} else {
						bs = new BitSet(revisions.length);
						filemap.put(file, bs);
					}

					bs.set(i);
				}
			}

			// For each file it has to be decided which operation to perform.
			// This is done by applying the standard 3-way merge rules.
			for (Map.Entry<String, BitSet> entry : filemap.entrySet()) {
				String file = entry.getKey();
				BitSet bs = entry.getValue();

				if (LOG.isDebugEnabled()) {
					StringBuilder sb = new StringBuilder();
					int bits = 0;

					sb.append("[ ");

					for (int i = 0; i < revisions.length; i++) {
						if (bs.get(i)) {
							sb.append(revisions[i].getRevision().toString());
							bits++;

							if (bits < bs.cardinality()) {
								sb.append(", ");
							}
						}
					}

					sb.append(" ]");
					LOG.debug(file + "\t" + sb.toString());
				}

				operations.addAll(applyMergeRule(revisions, output, file, bs,
						engine));
			}
		}

		return operations;
	}

	/**
	 * @param revisions
	 *            array of revisions
	 * @param output
	 *            output artifact
	 * @param file
	 *            file
	 * @param bs
	 *            bitset
	 * @param engine
	 *            merge engine
	 * @return operation
	 * @throws UnsupportedMergeTypeException
	 *             UnsupportedMergeTypeException
	 * @throws IOException  if an input output exception occurs
	 */
	private static OperationList applyMergeRule(final Artifact[] revisions,
			final Artifact output, final String file, final BitSet bs,
			final MergeEngine engine) throws UnsupportedMergeTypeException, 
			IOException {
		OperationList operations = new OperationList();

		Artifact left = revisions[LEFTPOS];
		Artifact base = revisions[BASEPOS];
		Artifact right = revisions[RIGHTPOS];

		switch (bs.cardinality()) {
		case ZERO:
			// File exists in 0 revisions.
			// This should never happen and is treated as error.
			throw new RuntimeException(
					"Ghost files! I do not know how to merge this!");
		case ONE:
			// File exists in exactly 1 revision.
			// This is an addition or a deletion.
			if (bs.get(BASEPOS)) {
				// File was deleted in base.
				Artifact deleted = new Artifact(base.getRevision(), new File(
						base.getPath() + File.separator + file));
				operations.add(new DeleteOperation(deleted));
			} else {
				// File was added in either left or right revision.
				Artifact added = bs.get(LEFTPOS) ? new Artifact(
						left.getRevision(), new File(left.getPath()
								+ File.separator + file)) : new Artifact(
						right.getRevision(), new File(right.getPath()
								+ File.separator + file));
				Artifact outputChild = output == null ? null : new Artifact(
						output.getRevision(), new File(output.getPath()
								+ File.separator + file), false);
				Artifact.createFile(outputChild, added.isDirectory());
				operations.add(new AddOperation(added, outputChild));
			}
			break;
		case TWO:
			// File exists in two revisions.
			// This is a 2-way merge or a deletion.
			if (bs.get(LEFTPOS) && bs.get(RIGHTPOS)) {
				// This is a 2-way merge.
				ArtifactList tuple = new ArtifactList();

				Artifact leftChild = new Artifact(left.getRevision(), new File(
						left.getPath() + File.separator + file));
				Artifact rightChild = new Artifact(right.getRevision(),
						new File(right.getPath() + File.separator + file));

				tuple.add(leftChild);
				tuple.add(rightChild);

				Artifact outputChild = output == null ? null : new Artifact(
						output.getRevision(), new File(output.getPath()
								+ File.separator + file), false);

				operations.addAll(calculateOperations(MergeType.TWOWAY, engine,
						tuple, outputChild));
			} else {
				// File was deleted in either left or right revision.
				assert (bs.get(BASEPOS));

				Artifact deleted = bs.get(LEFTPOS) ? new Artifact(
						left.getRevision(), new File(left.getPath()
								+ File.separator + file)) : new Artifact(
						right.getRevision(), new File(right.getPath()
								+ File.separator + file));

				operations.add(new DeleteOperation(deleted));
			}
			break;
		case THREE:
			// File exists in three revisions.
			// This is a classical 3-way merge.
			ArtifactList triple = new ArtifactList();

			Artifact leftChild = new Artifact(left.getRevision(), new File(
					left.getPath() + File.separator + file));
			Artifact baseChild = new Artifact(base.getRevision(), new File(
					base.getPath() + File.separator + file));
			Artifact rightChild = new Artifact(right.getRevision(), new File(
					right.getPath() + File.separator + file));

			triple.add(leftChild);
			triple.add(baseChild);
			triple.add(rightChild);

			Artifact outputChild = output == null ? null : new Artifact(
					output.getRevision(), new File(output.getPath()
							+ File.separator + file), false);

			operations.addAll(calculateOperations(MergeType.THREEWAY, engine,
					triple, outputChild));
			break;
		default:
			break;
		}

		return operations;
	}
}
