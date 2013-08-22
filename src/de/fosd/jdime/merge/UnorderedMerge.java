package de.fosd.jdime.merge;

import java.io.IOException;

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
		
		assert (left.matches(right));
		assert (left.hasMatching(right)) && right.hasMatching(left);

		Object[] revisions = { left, right };
		for (int i = 0; i < revisions.length; i++) {
			T myNode = (T) revisions[i];
			T otherNode = (T) revisions[(i + 1) % 2];

			for (T myChild : myNode.getChildren()) {
				if (myChild.hasMatches()) {
					// is not a change
					Matching<T> mOther = myChild
							.getMatching(otherNode.getRevision());
					if (mOther != null) {
						// child is in both left and right -> merge it
						T otherChild = mOther
								.getMatchingArtifact(myChild);

						// determine whether the child is 2 or 3-way merged
						Matching<T> mBase = myChild.getMatching(base
								.getRevision());

						MergeType childType = mBase == null ? MergeType.TWOWAY
								: MergeType.THREEWAY;
						T baseChild = mBase == null ? myChild
								.createEmptyDummy() : mBase
								.getMatchingArtifact(myChild);

						T targetChild = target == null ? null
								: target.addChild(myChild);
						MergeTriple<T> childTriple = myNode == revisions[0] 
								? new MergeTriple<T>(childType, 
										myChild, baseChild, otherChild)
								: new MergeTriple<T>(childType,
										otherChild, baseChild, myChild);

						MergeOperation<T> mergeOp = new MergeOperation<T>(
								childTriple, targetChild);

						mergeOp.apply(context);
					} else {
						// child is in my revision and base. It was deleted by
						// other.
						// we have to check if an inner node was changed by me
						Matching<T> mLB = myChild.getMatching(base
								.getRevision());
						assert (mLB != null);
						if (myChild.hasChanges()) {
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
					// is a change.
					AddOperation<T> addOp = new AddOperation<T>(
							myChild, target);
					addOp.apply(context);
				}
			}
		}
	}
}
