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
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import de.fosd.jdime.common.ArtifactList;
import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeTriple;
import de.fosd.jdime.common.MergeType;
import de.fosd.jdime.common.NotYetImplementedException;
import de.fosd.jdime.common.UnsupportedMergeTypeException;
import de.fosd.jdime.common.operations.AddOperation;
import de.fosd.jdime.common.operations.DeleteOperation;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.common.operations.Operation;
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
	 * Array position of left revision.
	 */
	private static final int LEFTPOS = 0;

	/**
	 * Array position of base revision.
	 */
	private static final int BASEPOS = 1;

	/**
	 * Array position of right revision.
	 */
	private static final int RIGHTPOS = 2;

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

		FileArtifact target = null;

		if (operation.getTarget() != null) {
			assert (operation.getTarget() instanceof FileArtifact);
			target = operation.getTarget();
		}

		FileArtifact[] revisions = { left, base, right };

		for (FileArtifact dir : revisions) {
			assert ((dir.exists() && dir.isDirectory()) || dir.isEmptyDummy());
		}

		HashMap<FileArtifact, BitSet> map = new HashMap<FileArtifact, BitSet>();

		// The revisions in which each artifact exists, are stored in map.
		for (int i = 0; i < revisions.length; i++) {
			FileArtifact cur = revisions[i];

			if (cur.isEmptyDummy()) {
				// base is a dummy for 2-way merges
				continue;
			}

			ArtifactList<FileArtifact> children = cur.getChildren();

			assert (children != null);

			for (FileArtifact child : children) {
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

		// For each artifact it has to be decided which operation to
		// perform. This is done by applying the standard 3-way merge rules.
		for (Map.Entry<FileArtifact, BitSet> entry : map.entrySet()) {
			FileArtifact child = entry.getKey();
			BitSet bs = entry.getValue();

			assert (child != null);
			assert (bs != null);

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

			Operation<FileArtifact> subOperation = applyMergeRules(revisions,
					target, child, bs, context);
			subOperation.apply(context);

		}

	}

	/**
	 * Decides which merge rule applies and executes it.
	 * 
	 * @param revisions
	 *            revisions
	 * @param target
	 *            target
	 * @param child
	 *            child
	 * @param bs
	 *            bitset
	 * @param context
	 *            merge context
	 * @return operation
	 * @throws IOException
	 *             If an input output exception occurs
	 * @throws InterruptedException
	 *             if a thread is interrupted
	 */
	private Operation<FileArtifact> applyMergeRules(
			final FileArtifact[] revisions, final FileArtifact target,
			final FileArtifact child, final BitSet bs,
			final MergeContext context) throws IOException,
			InterruptedException {
		assert (context != null);
		assert (revisions != null);
		assert (revisions.length >= MergeType.MINFILES);
		assert (revisions.length <= MergeType.MAXFILES);
		assert (child != null);
		assert (bs != null);

		FileArtifact left = revisions[LEFTPOS];
		FileArtifact base = revisions[BASEPOS];
		FileArtifact right = revisions[RIGHTPOS];

		switch (bs.cardinality()) {
		case ZERO:
			// Artifact exists in 0 revisions.
			// This should never happen and is treated as error.
			throw new RuntimeException(
					"Ghost artifacts! I do not know how to merge this!");
		case ONE:
			// Artifact exists in exactly 1 revision.
			// This is an addition or a deletion.
			if (bs.get(BASEPOS)) {
				// Artifact was deleted in base.
				FileArtifact deleted = base.getChild(child);
				assert (deleted != null);
				return new DeleteOperation<FileArtifact>(deleted);
			} else {
				// Artifact was added in either left or right revision.
				FileArtifact added = bs.get(LEFTPOS) ? left.getChild(child)
						: right.getChild(child);
				assert (added != null);
				return new AddOperation<FileArtifact>(added, target);
			}

		case TWO:
			// Artifact exists in two revisions.
			// This is a 2-way merge or a deletion.
			if (bs.get(LEFTPOS) && bs.get(RIGHTPOS)) {
				// This is a 2-way merge.
				ArtifactList<FileArtifact> tuple = new ArtifactList<FileArtifact>();

				FileArtifact leftChild = left.getChild(child);
				FileArtifact rightChild = right.getChild(child);
				FileArtifact targetChild = target == null ? null : target
						.addChild(child);

				assert (leftChild != null);
				assert (rightChild != null);
				assert (target == null || targetChild != null);

				tuple.add(leftChild);
				tuple.add(rightChild);

				return new MergeOperation<FileArtifact>(tuple, targetChild);
			} else {
				// Artifact was deleted in either left or right revision.
				assert (bs.get(BASEPOS));

				FileArtifact deleted = bs.get(LEFTPOS) ? left.getChild(child)
						: right.getChild(child);
				assert (deleted != null);
				return new DeleteOperation<FileArtifact>(deleted);
			}

		case THREE:
			// Artifact exists in three revisions.
			// This is a classical 3-way merge.
			ArtifactList<FileArtifact> triple = new ArtifactList<FileArtifact>();

			FileArtifact leftChild = left.getChild(child);
			FileArtifact baseChild = base.getChild(child);
			FileArtifact rightChild = right.getChild(child);
			FileArtifact targetChild = target == null ? null : target
					.addChild(child);
			assert (leftChild != null);
			assert (baseChild != null);
			assert (rightChild != null);
			assert (target == null || targetChild != null);

			triple.add(leftChild);
			triple.add(baseChild);
			triple.add(rightChild);

			return new MergeOperation<FileArtifact>(triple, targetChild);

		default:
			throw new UnsupportedMergeTypeException();
		}
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
