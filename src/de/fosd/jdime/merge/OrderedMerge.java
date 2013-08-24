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
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeTriple;
import de.fosd.jdime.common.MergeType;
import de.fosd.jdime.common.NotYetImplementedException;
import de.fosd.jdime.common.Revision;
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
public class OrderedMerge<T extends Artifact<T>> implements MergeInterface<T> {

	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(OrderedMerge.class);

	@Override
	public final void merge(final MergeOperation<T> operation,
			final MergeContext context) throws IOException,
			InterruptedException {

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

		List<T> leftChildren = left.getChildren();
		List<T> rightChildren = right.getChildren();
		
		if (LOG.isTraceEnabled()) {
			LOG.trace(logprefix + "Children that need to be merged:");
			LOG.trace(logprefix + left.getId() + " -> (" + leftChildren + ")");
			LOG.trace(logprefix + right.getId() + "-> (" + rightChildren + ")");
		}

		if (leftChildren.isEmpty() || rightChildren.isEmpty()) {
			if (leftChildren.isEmpty() && rightChildren.isEmpty()) {
				if (LOG.isTraceEnabled()) {
					LOG.trace(logprefix + left.getId() + " and " + right.getId()
							+ " have no children.");
				}
				return;
			} else if (leftChildren.isEmpty()) {
				if (LOG.isTraceEnabled()) {
					LOG.trace(logprefix + left.getId() + " has no children.");
				}
				if (LOG.isTraceEnabled()) {
					LOG.trace(logprefix + right.getId() 
							+ " was deleted by left");
				}
				if (right.hasChanges()) {
					if (LOG.isTraceEnabled()) {
						LOG.trace(logprefix + right.getId() 
								+ " has changes in subtree");
					}
					throw new NotYetImplementedException("Conflict needed");
				} else {

					for (T rightChild : rightChildren) {

						DeleteOperation<T> delOp = new DeleteOperation<T>(
								rightChild);
						delOp.apply(context);
					}
					return;
				}
			} else if (rightChildren.isEmpty()) {
				if (LOG.isTraceEnabled()) {
					LOG.trace(logprefix + right.getId() + " has no children.");
				}
				if (LOG.isTraceEnabled()) {
					LOG.trace(logprefix + left.getId() 
							+ " was deleted by left");
				}
				if (left.hasChanges()) {
					if (LOG.isTraceEnabled()) {
						LOG.trace(logprefix + 
								left.getId() + " has changes in subtree");
					}
					throw new NotYetImplementedException("Conflict needed");
				} else {
					for (T leftChild : leftChildren) {
						DeleteOperation<T> delOp = new DeleteOperation<T>(
								leftChild);
						delOp.apply(context);
					}
					return;
				}
			} else {
				throw new RuntimeException("Something is very broken.");
			}
		}

		Revision l = left.getRevision();
		Revision b = base.getRevision();
		Revision r = right.getRevision();
		Iterator<T> leftIt = leftChildren.iterator();
		Iterator<T> rightIt = rightChildren.iterator();
		T leftChild = (T) leftIt.next();
		T rightChild = (T) rightIt.next();

		if (LOG.isTraceEnabled() && target != null) {
			LOG.trace(logprefix + "target.dumpTree() before merge:");
			System.out.println(target.dumpRootTree());
		}

		boolean leftdone = false;
		boolean rightdone = false;
		while (!leftdone || !rightdone) {
			if (!r.contains(leftChild)) {
				if (LOG.isTraceEnabled()) {
					LOG.trace(logprefix + leftChild.getId() 
							+ " is not in right");
				}
				if (b.contains(leftChild)) {
					if (LOG.isTraceEnabled()) {
						LOG.trace(logprefix + leftChild.getId() 
								+ " was deleted by right");
					}
					// was deleted in right
					if (leftChild.hasChanges()) {
						// insertion-deletion-conflict
						if (LOG.isTraceEnabled()) {
							LOG.trace(leftChild.getId()
									+ " has changes in subtree.");
						}
						throw new NotYetImplementedException("Conflict needed");
					} else {
						// can be safely deleted
						DeleteOperation<T> delOp = new DeleteOperation<T>(
								leftChild);
						delOp.apply(context);
					}
				} else {
					if (LOG.isTraceEnabled()) {
						LOG.trace(logprefix + leftChild.getId() 
								+ " is a change");
					}
					// leftChild is a change
					if (!l.contains(rightChild)) {
						if (LOG.isTraceEnabled()) {
							LOG.trace(logprefix + rightChild.getId() + " is not in left");
						}
						if (b.contains(rightChild)) {
							if (LOG.isTraceEnabled()) {
								LOG.trace(logprefix + rightChild.getId()
										+ " was deleted by left");
							}
							// rightChild was deleted in left
							if (rightChild.hasChanges()) {
								if (LOG.isTraceEnabled()) {
									LOG.trace(logprefix + rightChild.getId()
											+ " has changes in subtree.");
								}
								// deletion-insertion conflict
								throw new NotYetImplementedException(
										"Conflict needed");
							} else {
								// add the left change
								AddOperation<T> addOp = new AddOperation<T>(
										leftChild, target);
								leftChild.setMerged(true);
								addOp.apply(context);
							}
						} else {
							if (LOG.isTraceEnabled()) {
								LOG.trace(logprefix + rightChild.getId() 
										+ " is a change");
							}
							// rightChild is a change
							throw new NotYetImplementedException(
									"Conflict needed");
						}
					} else {
						if (LOG.isTraceEnabled()) {
							LOG.trace(logprefix + "Add change: " 
						+ leftChild.getId());
						}
						// add the left change
						AddOperation<T> addOp = new AddOperation<T>(leftChild,
								target);
						leftChild.setMerged(true);
						addOp.apply(context);
					}
				}

				if (leftIt.hasNext()) {
					leftChild = leftIt.next();
				} else {
					leftdone = true;
				}
			}

			if (!l.contains(rightChild)) {
				if (LOG.isTraceEnabled()) {
					LOG.trace(logprefix + rightChild.getId() 
							+ " is not in left");
				}
				if (b.contains(rightChild)) {
					if (LOG.isTraceEnabled()) {
						LOG.trace(logprefix + rightChild.getId() 
								+ " was deleted by left");
					}

					// was deleted in left
					if (rightChild.hasChanges()) {
						if (LOG.isTraceEnabled()) {
							LOG.trace(logprefix + rightChild.getId()
									+ " has changes in subtree.");
						}
						// insertion-deletion-conflict
						throw new NotYetImplementedException("Conflict needed");
					} else {
						// can be safely deleted
						DeleteOperation<T> delOp = new DeleteOperation<T>(
								rightChild);
						delOp.apply(context);
					}
				} else {
					if (LOG.isTraceEnabled()) {
						LOG.trace(logprefix + rightChild.getId() 
								+ " is a change");
					}
					// rightChild is a change
					if (!r.contains(leftChild)) {
						if (LOG.isTraceEnabled()) {
							LOG.trace(logprefix + leftChild.getId() 
									+ " is not in right");
						}
						if (b.contains(leftChild)) {
							if (LOG.isTraceEnabled()) {
								LOG.trace(logprefix + leftChild.getId()
										+ " was deleted by right");
							}
							if (leftChild.hasChanges()) {
								if (LOG.isTraceEnabled()) {
									LOG.trace(logprefix + leftChild.getId()
											+ " has changes in subtree.");
								}
								// deletion-insertion conflict
								throw new NotYetImplementedException(
										"Conflict needed");
							} else {
								if (LOG.isTraceEnabled()) {
									LOG.trace(logprefix + "add the change: "
											+ rightChild.getId());
								}
								// add the right change
								AddOperation<T> addOp = new AddOperation<T>(
										rightChild, target);
								rightChild.setMerged(true);
								addOp.apply(context);
							}
						} else {
							if (LOG.isTraceEnabled()) {
								LOG.trace(logprefix + leftChild.getId() 
										+ " is a change");
							}
							// leftChild is a change
							throw new NotYetImplementedException(
									"Conflict needed");
						}
					} else {
						if (LOG.isTraceEnabled()) {
							LOG.trace(logprefix + "Add the change: " 
						+ rightChild.getId());
						}
						// add the right change
						AddOperation<T> addOp = new AddOperation<T>(rightChild,
								target);
						rightChild.setMerged(true);
						addOp.apply(context);
					}
				}

				if (rightIt.hasNext()) {
					rightChild = rightIt.next();
				} else {
					rightdone = true;
				}

			} else if (l.contains(rightChild) && r.contains(leftChild)) {
				// left and right have the artifact. merge it.
				if (LOG.isTraceEnabled()) {
					LOG.trace(logprefix + "Child is in left and right: " 
				+ left.getId());
				}
				assert (leftChild.hasMatching(rightChild) && rightChild
						.hasMatching(leftChild));

				if (!leftChild.isMerged() && !rightChild.isMerged()) {
					// determine whether the child is 2 or 3-way merged
					Matching<T> mBase = leftChild.getMatching(b);

					MergeType childType = mBase == null ? MergeType.TWOWAY
							: MergeType.THREEWAY;
					T baseChild = mBase == null ? leftChild.createEmptyDummy()
							: mBase.getMatchingArtifact(leftChild);
					T targetChild = target == null ? null : target
							.addChild(leftChild);

					MergeTriple<T> childTriple = new MergeTriple<T>(childType,
							leftChild, baseChild, rightChild);

					MergeOperation<T> mergeOp = new MergeOperation<T>(
							childTriple, targetChild);

					leftChild.setMerged(true);
					rightChild.setMerged(true);
					mergeOp.apply(context);
				}

				if (leftIt.hasNext()) {
					leftChild = leftIt.next();
				} else {
					leftdone = true;
				}

				if (rightIt.hasNext()) {
					rightChild = rightIt.next();
				} else {
					rightdone = true;
				}
			}
			if (LOG.isTraceEnabled() && target != null) {
				LOG.trace(logprefix 
						+ "target.dumpTree() after processing child:");
				System.out.println(target.dumpRootTree());
			}
		}

		if (LOG.isTraceEnabled()) {
			LOG.trace(logprefix + this.getClass().getSimpleName() + ".merge("
					+ left.getId() + ", " + base.getId() + ", " + right.getId()
					+ ") finished");
		}
	}
}
