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

import org.apache.log4j.Logger;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeTriple;
import de.fosd.jdime.common.MergeType;
import de.fosd.jdime.common.Revision;
import de.fosd.jdime.common.operations.AddOperation;
import de.fosd.jdime.common.operations.ConflictOperation;
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
	
	/**
	 * Logging prefix.
	 */
	private String logprefix;

	@Override
	public final void merge(final MergeOperation<T> operation,
			final MergeContext context) throws IOException,
			InterruptedException {

		MergeTriple<T> triple = operation.getMergeTriple();
		T left = triple.getLeft();
		T base = triple.getBase();
		T right = triple.getRight();
		T target = operation.getTarget();
		logprefix = operation.getId() + " - ";

		assert (left.matches(right));
		assert (left.hasMatching(right)) && right.hasMatching(left);

		if (LOG.isTraceEnabled()) {
			LOG.trace(prefix() + this.getClass().getSimpleName() + ".merge("
					+ left.getId() + ", " + base.getId() + ", " + right.getId()
					+ ")");
		}

		Revision l = left.getRevision();
		Revision b = base.getRevision();
		Revision r = right.getRevision();
		Iterator<T> leftIt = left.getChildren().iterator();
		Iterator<T> rightIt = right.getChildren().iterator();
		T leftChild = (T) leftIt.next();
		T rightChild = (T) rightIt.next();

		boolean leftdone = false;
		boolean rightdone = false;
		while (!leftdone || !rightdone) {
			if (!r.contains(leftChild)) {
				if (LOG.isTraceEnabled()) {
					LOG.trace(prefix(leftChild) + "is not in right");
				}
				if (b.contains(leftChild)) {
					if (LOG.isTraceEnabled()) {
						LOG.trace(prefix(leftChild) + "was deleted by right");
					}
					// was deleted in right
					if (leftChild.hasChanges()) {
						// insertion-deletion-conflict
						if (LOG.isTraceEnabled()) {
							LOG.trace(prefix(leftChild)
									+ "has changes in subtree.");
						}
						ConflictOperation<T> conflictOp 
							= new ConflictOperation<T>(leftChild, leftChild, null, target);
						conflictOp.apply(context);
					} else {
						// can be safely deleted
						DeleteOperation<T> delOp = new DeleteOperation<T>(
								leftChild);
						delOp.apply(context);
					}
				} else {
					if (LOG.isTraceEnabled()) {
						LOG.trace(prefix(leftChild) + "is a change");
					}
					// leftChild is a change
					if (!l.contains(rightChild)) {
						if (LOG.isTraceEnabled()) {
							LOG.trace(prefix(rightChild) + "is not in left");
						}
						if (b.contains(rightChild)) {
							if (LOG.isTraceEnabled()) {
								LOG.trace(prefix(rightChild)
										+ "was deleted by left");
							}
							// rightChild was deleted in left
							if (rightChild.hasChanges()) {
								if (LOG.isTraceEnabled()) {
									LOG.trace(prefix(rightChild)
											+ "has changes in subtree.");
								}
								// deletion-insertion conflict
								ConflictOperation<T> conflictOp 
									= new ConflictOperation<T>(rightChild, null, 
											rightChild, target);
								conflictOp.apply(context);
							} else {
								// add the left change
								AddOperation<T> addOp = new AddOperation<T>(
										leftChild, target);
								leftChild.setMerged(true);
								addOp.apply(context);
							}
						} else {
							if (LOG.isTraceEnabled()) {
								LOG.trace(prefix(rightChild) + "is a change");
							}
							// rightChild is a change
							ConflictOperation<T> conflictOp 
								= new ConflictOperation<T>(leftChild, leftChild, 
									rightChild, target);
							conflictOp.apply(context);
							
							if (rightIt.hasNext()) {
								rightChild = rightIt.next();
							} else {
								rightdone = true;
							}
						}
					} else {
						if (LOG.isTraceEnabled()) {
							LOG.trace(prefix(leftChild) + "adding change");
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
					LOG.trace(prefix(rightChild) + "is not in left");
				}
				if (b.contains(rightChild)) {
					if (LOG.isTraceEnabled()) {
						LOG.trace(prefix(rightChild) + "was deleted by left");
					}

					// was deleted in left
					if (rightChild.hasChanges()) {
						if (LOG.isTraceEnabled()) {
							LOG.trace(prefix(rightChild)
									+ "has changes in subtree.");
						}
						// insertion-deletion-conflict
						ConflictOperation<T> conflictOp 
							= new ConflictOperation<T>(rightChild, null, 
									rightChild, target);
						conflictOp.apply(context);
					} else {
						// can be safely deleted
						DeleteOperation<T> delOp = new DeleteOperation<T>(
								rightChild);
						delOp.apply(context);
					}
				} else {
					if (LOG.isTraceEnabled()) {
						LOG.trace(prefix(rightChild) + "is a change");
					}
					// rightChild is a change
					if (!r.contains(leftChild)) {
						if (LOG.isTraceEnabled()) {
							LOG.trace(prefix(leftChild) + "is not in right");
						}
						if (b.contains(leftChild)) {
							if (LOG.isTraceEnabled()) {
								LOG.trace(prefix(leftChild)
										+ "was deleted by right");
							}
							if (leftChild.hasChanges()) {
								if (LOG.isTraceEnabled()) {
									LOG.trace(prefix(leftChild)
											+ "has changes in subtree.");
								}
								// deletion-insertion conflict
								ConflictOperation<T> conflictOp 
									= new ConflictOperation<T>(leftChild, leftChild, null, 
											target);
								conflictOp.apply(context);
							} else {
								if (LOG.isTraceEnabled()) {
									LOG.trace(prefix(rightChild) 
											+ "adding change");
								}
								// add the right change
								AddOperation<T> addOp = new AddOperation<T>(
										rightChild, target);
								rightChild.setMerged(true);
								addOp.apply(context);
							}
						} else {
							if (LOG.isTraceEnabled()) {
								LOG.trace(prefix(leftChild) + "is a change");
							}
							// leftChild is a change
							ConflictOperation<T> conflictOp 
								= new ConflictOperation<T>(leftChild, leftChild, 
										rightChild, target);
							conflictOp.apply(context);
						
							if (leftIt.hasNext()) {
								leftChild = leftIt.next();
							} else {
								leftdone = true;
							}
						}
					} else {
						if (LOG.isTraceEnabled()) {
							LOG.trace(prefix(rightChild) + "adding change");
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
					LOG.trace(prefix(leftChild) + "is in both revisions [" 
							+ rightChild.getId() + "]");
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
				LOG.trace(prefix() 
						+ "target.dumpTree() after processing child:");
				System.out.println(target.dumpRootTree());
			}
		}
	}
	
	/**
	 * Returns the logging prefix.
	 * @return logging prefix
	 */
	private String prefix() {
		return logprefix;
	}
	
	/**
	 * Returns the logging prefix.
	 * @param artifact artifact that is subject of the logging
	 * @return logging prefix
	 */
	private String prefix(final T artifact) {
		return logprefix + "[" + artifact.getId() + "] ";
	}
}
