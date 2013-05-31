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

import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import de.fosd.jdime.Main;
import de.fosd.jdime.common.operations.AddOperation;
import de.fosd.jdime.common.operations.DeleteOperation;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.common.operations.Operation;
import de.fosd.jdime.common.operations.OperationStack;
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
	 * @throws EngineNotFoundException
	 *             if merge engine cannot be found
	 * @throws IOException
	 *             IOException
	 * @throws InterruptedException
	 *             InterruptedException
	 * @throws UnsupportedMergeTypeException
	 *             UnsupportedMergeTypeException
	 */
	public static void merge(final MergeType mergeType,
			final MergeEngine engine, final ArtifactList inputArtifacts,
			final Artifact output) throws EngineNotFoundException, IOException,
			InterruptedException, UnsupportedMergeTypeException {
		LOG.setLevel(Main.getLogLevel());
		LOG.debug(Merge.class.getName());
		LOG.debug(mergeType.name() + " merge will be performed.");

		OperationStack stack = calculateOperations(mergeType, engine,
				inputArtifacts, output);

		while (!stack.isEmpty()) {
			Operation operation = stack.pop();
			if (LOG.isDebugEnabled()) {
				LOG.trace("Popped " + operation.getClass().getSimpleName()
						+ " from stack.");
				LOG.debug(operation.description());
			}

			MergeReport report = operation.apply();

			if (Main.isPrintToStdout()) {
				System.out.println(report.getStdIn());

				if (report.hasErrors()) {
					System.err.println(report.getStdErr());
				}
			}
		}
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
	 * @return stack of operations
	 * @throws UnsupportedMergeTypeException
	 *             UnsupportedMergeTypeException
	 * @throws IOException
	 *             If an input output exception occurs
	 */
	private static OperationStack calculateOperations(
			final MergeType mergeType, final MergeEngine engine,
			final ArtifactList inputArtifacts, final Artifact output)
			throws UnsupportedMergeTypeException, IOException {
		OperationStack stack = new OperationStack();

		Artifact left, base, right;

		if (mergeType == MergeType.TWOWAY) {
			left = inputArtifacts.get(0);
			base = left.createEmptyDummy();
			right = inputArtifacts.get(1);
		} else if (mergeType == MergeType.THREEWAY) {
			left = inputArtifacts.get(0);
			base = inputArtifacts.get(1);
			right = inputArtifacts.get(2);
		} else {
			throw new UnsupportedMergeTypeException();
		}

		assert (left.getClass().equals(right.getClass())) 
				: "Only artifacts of the same type can be merged";

		left.setRevision(new Revision("left"));
		base.setRevision(new Revision("base"));
		right.setRevision(new Revision("right"));

		Artifact[] revisions = { left, base, right };

		boolean isLeaf = left.isLeaf();

		if (isLeaf) {
			// To merge files, we just have to create a merge operation.
			LOG.debug("Input artifacts are files.");

			MergeTriple triple = new MergeTriple(left, base, right);
			left.createArtifact(output, isLeaf);
			stack.push(new MergeOperation(mergeType, triple, engine, output));
			LOG.trace("Pushed MergeOperation to stack");
		} else {
			// To merge directories, we need to apply the standard three-way
			// merge rules to the content of the directories.

			for (Artifact artifact : revisions) {
				assert (!artifact.isLeaf() || artifact.isEmptyDummy());
			}

			LOG.debug("Input artifacts are directories.");

			HashMap<Artifact, BitSet> map = new HashMap<Artifact, BitSet>();

			// The revisions in which each file exists, are stored in filemap.
			for (int i = 0; i < revisions.length; i++) {
				Artifact cur = revisions[i];

				if (cur.isEmptyDummy()) {
					// base is a dummy for 2-way merges
					continue;
				}
				
				ArtifactList children = cur.getChildren();
				
				for (Artifact child : children) {
					BitSet bs;

					if (map.containsKey(child)) {
						bs = map.get(child);
					} else {
						bs = new BitSet(revisions.length);
						map.put(child, bs);
					}

					bs.set(i);
				}
			}

			// For each file it has to be decided which operation to perform.
			// This is done by applying the standard 3-way merge rules.
			for (Map.Entry<Artifact, BitSet> entry : map.entrySet()) {
				Artifact child = entry.getKey();
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
					LOG.debug(child.getId() + "\t" + sb.toString());
				}

				OperationStack substack = applyMergeRule(revisions, output,
						child, bs, engine);

				while (!substack.isEmpty()) {
					stack.push(substack.pop());
				}
			}
		}

		return stack;
	}

	/**
	 * @param revisions
	 *            array of revisions
	 * @param output
	 *            output artifact
	 * @param child
	 *            child artifact
	 * @param bs
	 *            bitset
	 * @param engine
	 *            merge engine
	 * @return operation stack of operations
	 * @throws UnsupportedMergeTypeException
	 *             UnsupportedMergeTypeException
	 * @throws IOException
	 *             if an input output exception occurs
	 */
	private static OperationStack applyMergeRule(final Artifact[] revisions,
			final Artifact output, final Artifact child, final BitSet bs,
			final MergeEngine engine) throws UnsupportedMergeTypeException,
			IOException {
		OperationStack stack = new OperationStack();

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
				Artifact deleted = base.getChild(child);
				stack.push(new DeleteOperation(deleted));
				LOG.trace("Pushed DeleteOperation to stack");
			} else {
				// File was added in either left or right revision.
				Artifact added = bs.get(LEFTPOS) ? left.getChild(child) 
												 : right.getChild(child);
				stack.push(new AddOperation(added, output));
				LOG.trace("Pushed AddOperation to stack");
			}

			break;
		case TWO:
			// File exists in two revisions.
			// This is a 2-way merge or a deletion.
			if (bs.get(LEFTPOS) && bs.get(RIGHTPOS)) {
				// This is a 2-way merge.
				ArtifactList tuple = new ArtifactList();

				Artifact leftChild = left.getChild(child);
				Artifact rightChild = right.getChild(child);
				Artifact outputChild = output == null ? null
													  : output.addChild(child);
				
				tuple.add(leftChild);
				tuple.add(rightChild);
				
				OperationStack substack = calculateOperations(MergeType.TWOWAY,
						engine, tuple, outputChild);

				while (!substack.isEmpty()) {
					stack.push(substack.pop());
				}
			} else {
				// File was deleted in either left or right revision.
				assert (bs.get(BASEPOS));

				Artifact deleted = bs.get(LEFTPOS) ? left.getChild(child)
												   : right.getChild(child);

				stack.push(new DeleteOperation(deleted));
				LOG.trace("Pushed DeleteOperation to stack");
			}

			break;
		case THREE:
			// File exists in three revisions.
			// This is a classical 3-way merge.
			ArtifactList triple = new ArtifactList();

			Artifact leftChild = left.getChild(child);
			Artifact baseChild = base.getChild(child);
			Artifact rightChild = right.getChild(child);
			Artifact outputChild = output == null ? null 
												  : output.addChild(child);

			triple.add(leftChild);
			triple.add(baseChild);
			triple.add(rightChild);
			
			OperationStack substack = calculateOperations(MergeType.THREEWAY,
					engine, triple, outputChild);

			while (!substack.isEmpty()) {
				stack.push(substack.pop());
			}

			break;
		default:
			throw new UnsupportedMergeTypeException();
		}

		return stack;
	}
}
