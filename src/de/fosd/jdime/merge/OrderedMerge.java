/**
 * 
 */
package de.fosd.jdime.merge;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import de.fosd.jdime.common.ASTNodeArtifact;
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

		assert (left.matches(right));
		assert (left.hasMatching(right)) && right.hasMatching(left);

		List<T> leftChildren = left.getChildren();
		List<T> baseChildren = base.isEmptyDummy() ? null : base.getChildren();
		List<T> rightChildren = right.getChildren();

		if (leftChildren.isEmpty() || rightChildren.isEmpty()) {
			if (leftChildren.isEmpty() && rightChildren.isEmpty()) {
				return;
			} else if (leftChildren.isEmpty()) {
				if (right.hasChanges()) {
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
				if (left.hasChanges()) {
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

		if (LOG.isTraceEnabled()) {
			if (target instanceof ASTNodeArtifact) {
				LOG.trace("target.dumpTree() before merge:");
				System.out.println(((ASTNodeArtifact) target).dumpRootTree());
			}
		}

		boolean leftdone = false;
		boolean rightdone = false;
		while (!leftdone && !rightdone) {
			if (!r.contains(leftChild)) {
				if (b.contains(leftChild)) {
					// was deleted in right
					if (leftChild.hasChanges()) {
						// insertion-deletion-conflict
						throw new NotYetImplementedException("Conflict needed");
					} else {
						// can be safely deleted
						DeleteOperation<T> delOp = new DeleteOperation<T>(
								leftChild);
						delOp.apply(context);
					}
				} else {
					// leftChild is a change
					if (!l.contains(rightChild)) {
						if (b.contains(rightChild)) {
							if (rightChild.hasChanges()) {
								// deletion-insertion conflict
								throw new NotYetImplementedException(
										"Conflict needed");
							} else {
								// add the change
								AddOperation<T> addOp = new AddOperation<T>(
										leftChild, target);
								leftChild.setMerged(true);
								addOp.apply(context);
							}
						} else {
							// right is a change
							throw new NotYetImplementedException(
									"Conflict needed");
						}
					} else {
						// add the change
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
				if (b.contains(rightChild)) {
					// was deleted in left
					if (rightChild.hasChanges()) {
						// insertion-deletion-conflict
						throw new NotYetImplementedException("Conflict needed");
					} else {
						// can be safely deleted
						DeleteOperation<T> delOp = new DeleteOperation<T>(
								rightChild);
						delOp.apply(context);
					}
				} else {
					// rightChild is a change
					if (!r.contains(leftChild)) {
						if (b.contains(leftChild)) {
							if (leftChild.hasChanges()) {
								// deletion-insertion conflict
								throw new NotYetImplementedException(
										"Conflict needed");
							} else {
								// add the change
								AddOperation<T> addOp = new AddOperation<T>(
										rightChild, target);
								rightChild.setMerged(true);
								addOp.apply(context);
							}
						} else {
							// left is a change
							throw new NotYetImplementedException(
									"Conflict needed");
						}
					} else {
						// add the change
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
				assert (leftChild.hasMatching(rightChild) && rightChild
						.hasMatching(leftChild));

				if (!leftChild.isMerged() && !rightChild.isMerged()) {
					// determine whether the child is 2 or 3-way merged
					Matching<T> mBase = leftChild.getMatching(base
							.getRevision());

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
			if (LOG.isTraceEnabled()) {
				if (target instanceof ASTNodeArtifact) {
					LOG.trace("target.dumpTree() after processing child:");
					System.out.println(((ASTNodeArtifact) target)
							.dumpRootTree());
				}
			}
		}
	}
}
