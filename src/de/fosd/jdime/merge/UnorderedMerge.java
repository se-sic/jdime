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
package de.fosd.jdime.merge;

import java.io.IOException;

import org.apache.log4j.Logger;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeTriple;
import de.fosd.jdime.common.MergeType;
import de.fosd.jdime.common.NotYetImplementedException;
import de.fosd.jdime.common.operations.AddOperation;
import de.fosd.jdime.common.operations.DeleteOperation;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.matcher.Matching;

/**
 * @author Olaf Lessenich
 * 
 * @param <T>
 *            type of artifact
 */
public class UnorderedMerge<T extends Artifact<T>> 
		implements MergeInterface<T> {

	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(UnorderedMerge.class);

	@Override
	public final void merge(final MergeOperation<T> operation,
			final MergeContext context) throws IOException,
			InterruptedException {
		assert (operation != null);
		assert (context != null);

		MergeTriple<T> triple = operation.getMergeTriple();
		T left = triple.getLeft();
		T base = triple.getBase();
		T right = triple.getRight();
		T target = operation.getTarget();
		String logprefix = operation.getId() + " - ";

		assert (left.matches(right));
		assert (left.hasMatching(right)) && right.hasMatching(left);

		if (LOG.isTraceEnabled()) {
			LOG.trace(logprefix + this.getClass().getSimpleName() + ".merge("
					+ left.getId() + ", " + base.getId() + ", " + right.getId()
					+ ")");
		}
		
		if (LOG.isTraceEnabled()) {
			LOG.trace(logprefix + "Children that need to be merged:");
			LOG.trace(logprefix + left.getId() + " -> (" + left.getChildren() + ")");
			LOG.trace(logprefix + right.getId() + "-> (" + right.getChildren() + ")");
		}

		Object[] revisions = { left, right };
		for (int i = 0; i < revisions.length; i++) {
			T myNode = (T) revisions[i];
			T otherNode = (T) revisions[(i + 1) % 2];

			int j = 0;
			if (LOG.isTraceEnabled()) {
				LOG.trace(logprefix + "Traversing children of " + myNode.getId() + " ("
						+ myNode.getNumChildren() + " children)");
			}

			if (LOG.isTraceEnabled() && target != null) {
				LOG.trace(logprefix + "target.dumpTree() before merge:");
					System.out.println(target.dumpRootTree());
			}

			for (T myChild : myNode.getChildren()) {
				if (LOG.isTraceEnabled()) {
					LOG.trace(logprefix + "Processing child " + (i + 1) + " of "
							+ myNode.getNumChildren() + ": " + myChild.getId()
							+ " (Parent: " + myNode.getId() + ")");
				}
				if (myChild.hasMatches()) {
					if (LOG.isTraceEnabled()) {
						LOG.trace(logprefix + myChild.getId() 
								+ " is not a change");
					}
					// is not a change
					Matching<T> mOther = myChild.getMatching(otherNode
							.getRevision());
					if (mOther != null) {
						if (LOG.isTraceEnabled()) {
							LOG.trace(logprefix + myChild.getId() 
									+ " is in left and right");
						}
						// child is in both left and right -> merge it
						T otherChild = mOther.getMatchingArtifact(myChild);

						if (!myChild.isMerged() && !otherChild.isMerged()) {
							// determine whether the child is 2 or 3-way merged
							Matching<T> mBase = myChild.getMatching(base
									.getRevision());

							MergeType childType = mBase == null ? MergeType.TWOWAY
									: MergeType.THREEWAY;
							T baseChild = mBase == null ? myChild
									.createEmptyDummy() : mBase
									.getMatchingArtifact(myChild);

							T targetChild = target == null ? null : target
									.addChild(myChild);

							MergeTriple<T> childTriple = myNode == revisions[0] ? new MergeTriple<T>(
									childType, myChild, baseChild, otherChild)
									: new MergeTriple<T>(childType, otherChild,
											baseChild, myChild);

							MergeOperation<T> mergeOp = new MergeOperation<T>(
									childTriple, targetChild);
							myChild.setMerged(true);
							otherChild.setMerged(true);
							mergeOp.apply(context);
						}
					} else {
						if (LOG.isTraceEnabled()) {
							LOG.trace(logprefix + myChild.getId() 
									+ " was deleted by the other revision");
						}
						// child is in my revision and base. It was deleted by
						// other.
						// we have to check if an inner node was changed by me
						Matching<T> mLB = myChild.getMatching(base
								.getRevision());
						assert (mLB != null);
						if (myChild.hasChanges()) {
							if (LOG.isTraceEnabled()) {
								LOG.trace(logprefix + myChild.getId() 
										+ " has changes in subtree.");
							}
							// we need to report a conflict between leftNode and
							// its deletion
							throw new NotYetImplementedException();
						} else {
							// Node can safely be deleted
							DeleteOperation<T> delOp = new DeleteOperation<T>(
									myChild);
							delOp.apply(context);
						}
					}
				} else {
					if (LOG.isTraceEnabled()) {
						LOG.trace(logprefix + myChild.getId() + " is a change");
					}
					if (LOG.isTraceEnabled()) {
						LOG.trace(logprefix + "Add the change: " 
								+ myChild.getId());
					}
					// is a change.
					AddOperation<T> addOp 
						= new AddOperation<T>(myChild, target);
					myChild.setMerged(true);
					addOp.apply(context);
				}
				if (LOG.isTraceEnabled() && target != null) {
					LOG.trace(logprefix 
							+ "target.dumpTree() after processing child:");
						System.out.println(target.dumpRootTree());
				}
			}
		}

		if (LOG.isTraceEnabled()) {
			LOG.trace(logprefix + this.getClass().getSimpleName() + ".merge("
					+ left.getId() + ", " + base.getId() + ", " + right.getId()
					+ ") finished");
		}
	}
}
