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
package de.fosd.jdime.merge;

import java.io.IOException;

import org.apache.log4j.Logger;

import de.fosd.jdime.common.ASTNodeArtifact;
import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeTriple;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.matcher.Color;
import de.fosd.jdime.matcher.Matcher;
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
	private static final Logger LOG = Logger.getLogger(Merge.class);

	/**
	 * 
	 */
	private UnorderedMerge<T> unorderedMerge = null;

	/**
	 * 
	 */
	private OrderedMerge<T> orderedMerge = null;

	@Override
	public final void merge(final MergeOperation<T> operation,
			final MergeContext context) throws IOException,
			InterruptedException {
		MergeTriple<T> triple = operation.getMergeTriple();
		T left = triple.getLeft();
		T base = triple.getBase();
		T right = triple.getRight();

		Matching<T> m;
		if (!left.matchingComputed() && !right.matchingComputed()) {
			if (!base.isEmptyDummy()) {
				// 3-way merge

				// diff base left
				m = diff(base, left, Color.GREEN);
				if (LOG.isDebugEnabled()) {
					if (m.getScore() == 0) {
						LOG.debug(base.getId() + " and " + left.getId() 
							+ " have no matches.");
					}
				}

				// diff base right
				m = diff(base, right, Color.GREEN);
				if (LOG.isDebugEnabled()) {
					if (m.getScore() == 0) {
						LOG.debug(base.getId() + " and " + right.getId() 
							+ " have no matches.");
					}
				}
			}

			// diff left right
			m = diff(left, right, Color.BLUE);
			if (m.getScore() == 0) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(left.getId() + " and " + right.getId() 
						+ " have no matches.");
				}
				return;
			}
		}
		
		assert (left.hasMatching(right) && right.hasMatching(left));

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

		if (isOrdered) {
			if (orderedMerge == null) {
				orderedMerge = new OrderedMerge<T>();
			}

			orderedMerge.merge(operation, context);
		} else {
			if (unorderedMerge == null) {
				unorderedMerge = new UnorderedMerge<T>();
			}

			unorderedMerge.merge(operation, context);
		}
	}

	/**
	 * Compares two nodes.
	 * 
	 * @param left
	 *            left node
	 * @param right
	 *            right node
	 * @param color
	 *            color of the matching (for debug output only)
	 * @return Matching of the two nodes
	 */
	private Matching<T> diff(final T left, final T right, final Color color) {
		Matcher<T> matcher = new Matcher<T>(left.hasUniqueLabels()
				&& right.hasUniqueLabels());
		Matching<T> m = matcher.match(left, right);

		if (LOG.isTraceEnabled()) {
			LOG.trace("match(" + left.getRevision() + ", "
					+ right.getRevision() + ") = " + m.getScore());
			LOG.trace(matcher.getLog());
			LOG.trace("Store matching information within nodes.");
		}

		matcher.storeMatching(m, color);
		if (LOG.isTraceEnabled()) {
			LOG.trace("Dumping matching of " + left.getRevision() + " and "
					+ right.getRevision());
			System.out.println(m.dumpTree());
			
			LOG.trace("left.dumpTree():");
			System.out.println(left.dumpTree());
			
			LOG.trace("right.dumpTree():");
			System.out.println(right.dumpTree());
		}

		return m;
	}
}
