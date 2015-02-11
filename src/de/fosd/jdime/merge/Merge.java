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
package de.fosd.jdime.merge;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.ClassUtils;
import org.apache.log4j.Logger;

import de.fosd.jdime.common.ASTNodeArtifact;
import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeTriple;
import de.fosd.jdime.common.operations.ConflictOperation;
import de.fosd.jdime.common.operations.DeleteOperation;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.matcher.Color;
import de.fosd.jdime.matcher.Matching;

/**
 * @author Olaf Lessenich
 *
 * @param <T>
 *            type of artifact
 */
public class Merge<T extends Artifact<T>> implements MergeInterface<T> {

	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(ClassUtils
			.getShortClassName(Merge.class));
	/**
     *
     */
	private UnorderedMerge<T> unorderedMerge = null;
	/**
     *
     */
	private OrderedMerge<T> orderedMerge = null;
	/**
	 * Logging prefix.
	 */
	private String logprefix;

	@Override
	public final void merge(final MergeOperation<T> operation,
			final MergeContext context) throws IOException,
			InterruptedException {
		logprefix = operation.getId() + " - ";
		MergeTriple<T> triple = operation.getMergeTriple();
		T left = triple.getLeft();
		T base = triple.getBase();
		T right = triple.getRight();
		T target = operation.getTarget();
		Diff<T> diff = new Diff<>();

		Matching<T> m;
		if (!left.matchingComputed() && !right.matchingComputed()) {
			if (!base.isEmptyDummy()) {
				// 3-way merge

				// diff base left
				m = diff.compare(base, left, Color.GREEN);
				if (LOG.isDebugEnabled()) {
					if (m.getScore() == 0) {
						LOG.debug(base.getId() + " and " + left.getId()
								+ " have no matches.");
					}
				}

				// diff base right
				m = diff.compare(base, right, Color.GREEN);
				if (LOG.isDebugEnabled()) {
					if (m.getScore() == 0) {
						LOG.debug(base.getId() + " and " + right.getId()
								+ " have no matches.");
					}
				}
			}

			// diff left right
			m = diff.compare(left, right, Color.BLUE);

			// TODO: compute and write diff stats
			if (context.isDiffOnly() && left.isRoot()
					&& left instanceof ASTNodeArtifact) {
				assert (right.isRoot());
				return;
			}

			if (m.getScore() == 0) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(left.getId() + " and " + right.getId()
							+ " have no matches.");
				}
				return;
			}
		}

		assert (left.hasMatching(right) && right.hasMatching(left));

		if (target != null && target.isRoot() && !target.hasMatches()) {
			// hack to fix the matches for the merged root node
			target.cloneMatches(left);
		}

		// check if one or both the nodes have no children
		List<T> leftChildren = left.getChildren();
		List<T> rightChildren = right.getChildren();

		if (LOG.isTraceEnabled()) {
			LOG.trace(prefix() + "Children that need to be merged:");
			LOG.trace(prefix(left) + "-> (" + leftChildren + ")");
			LOG.trace(prefix(right) + "-> (" + rightChildren + ")");
		}

		if ((base.isEmptyDummy() || base.hasChildren())
				&& (leftChildren.isEmpty() || rightChildren.isEmpty())) {
			if (leftChildren.isEmpty() && rightChildren.isEmpty()) {
				if (LOG.isTraceEnabled()) {
					LOG.trace(prefix(left) + "and [" + right.getId()
							+ "] have no children.");
				}
				return;
			} else if (leftChildren.isEmpty()) {
				if (LOG.isTraceEnabled()) {
					LOG.trace(prefix(left) + "has no children.");
					LOG.trace(prefix(right) + "was deleted by left");
				}
				if (right.hasChanges()) {
					if (LOG.isTraceEnabled()) {
						LOG.trace(prefix(right) + "has changes in subtree");
					}
					for (T rightChild : right.getChildren()) {
						ConflictOperation<T> conflictOp = new ConflictOperation<>(
								rightChild, null, rightChild, target);
						conflictOp.apply(context);
					}
					return;
				} else {
					for (T rightChild : rightChildren) {

						DeleteOperation<T> delOp = new DeleteOperation<>(
								rightChild);
						delOp.apply(context);
					}
					return;
				}
			} else if (rightChildren.isEmpty()) {
				if (LOG.isTraceEnabled()) {
					LOG.trace(prefix(right) + "has no children.");
					LOG.trace(prefix(left) + " was deleted by right");
				}
				if (left.hasChanges()) {
					if (LOG.isTraceEnabled()) {
						LOG.trace(prefix(left) + " has changes in subtree");
					}
					for (T leftChild : left.getChildren()) {
						ConflictOperation<T> conflictOp = new ConflictOperation<>(
								leftChild, leftChild, null, target);
						conflictOp.apply(context);
					}
					return;
				} else {
					for (T leftChild : leftChildren) {
						DeleteOperation<T> delOp = new DeleteOperation<>(
								leftChild);
						delOp.apply(context);
					}
					return;
				}
			} else {
				throw new RuntimeException("Something is very broken.");
			}
		}

		// determine whether we have to respect the order of children
		boolean isOrdered = false;
		for (int i = 0; !isOrdered && i < left.getNumChildren(); i++) {
			if (left.getChild(i).isOrdered()) {
				isOrdered = true;
			}
		}
		for (int i = 0; !isOrdered && i < right.getNumChildren(); i++) {
			if (right.getChild(i).isOrdered()) {
				isOrdered = true;
			}
		}

		if (LOG.isTraceEnabled() && target != null) {
			LOG.trace(logprefix + "target.dumpTree() before merge:");
			System.out.println(target.dumpRootTree());
		}
		if (isOrdered) {
			if (orderedMerge == null) {
				orderedMerge = new OrderedMerge<>();
			}
			orderedMerge.merge(operation, context);
		} else {
			if (unorderedMerge == null) {
				unorderedMerge = new UnorderedMerge<>();
			}
			unorderedMerge.merge(operation, context);
		}
		return;
	}

	/**
	 * Returns the logging prefix.
	 *
	 * @return logging prefix
	 */
	private String prefix() {
		return logprefix;
	}

	/**
	 * Returns the logging prefix.
	 *
	 * @param artifact
	 *            artifact that is subject of the logging
	 * @return logging prefix
	 */
	private String prefix(final T artifact) {
		return logprefix + "[" + artifact.getId() + "] ";
	}
}
