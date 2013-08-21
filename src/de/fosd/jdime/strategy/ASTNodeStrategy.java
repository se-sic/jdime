/**
 * 
 */
package de.fosd.jdime.strategy;

import java.io.IOException;

import org.apache.log4j.Logger;

import de.fosd.jdime.common.ASTNodeArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeTriple;
import de.fosd.jdime.common.MergeType;
import de.fosd.jdime.common.NotYetImplementedException;
import de.fosd.jdime.common.operations.DeleteOperation;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.matcher.Matching;
import de.fosd.jdime.stats.Stats;

/**
 * @author Olaf Lessenich
 * 
 */
public class ASTNodeStrategy extends MergeStrategy<ASTNodeArtifact> {
	
	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger
			.getLogger(ASTNodeStrategy.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.strategy.MergeInterface#merge(
	 * de.fosd.jdime.common.operations .MergeOperation,
	 * de.fosd.jdime.common.MergeContext)
	 */
	@Override
	public final void merge(final MergeOperation<ASTNodeArtifact> operation,
			final MergeContext context) throws IOException,
			InterruptedException {
		assert (operation != null);
		assert (context != null);

		MergeTriple<ASTNodeArtifact> triple = operation.getMergeTriple();

		assert (triple.isValid());

		ASTNodeArtifact left = triple.getLeft();
		ASTNodeArtifact base = triple.getBase();
		ASTNodeArtifact right = triple.getRight();
		ASTNodeArtifact target = operation.getTarget();

		ASTNodeArtifact[] revisions = { left, base, right };

		for (ASTNodeArtifact node : revisions) {
			assert (node.exists());
		}

		assert (target != null);
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
			orderedMerge(context, triple.getMergeType(), left, base, right,
					target);
		} else {
			unorderedMerge(context, triple.getMergeType(), left, base, right,
					target);
		}

		// FIXME: remove me when implementation is complete
		throw new NotYetImplementedException("ASTNodeStrategy: Implement me!");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.strategy.MergeStrategy#toString()
	 */
	@Override
	public final String toString() {
		// TODO Auto-generated method stub
		return "astnode";
	}

	@Override
	public final Stats createStats() {
		return new Stats(new String[] { "nodes" });
	}

	@Override
	public final String getStatsKey(final ASTNodeArtifact artifact) {
		// FIXME: remove me when implementation is complete
		throw new NotYetImplementedException("ASTNodeStrategy: Implement me!");
	}

	@Override
	public final void dump(final ASTNodeArtifact artifact,
			final boolean graphical) throws IOException {
		if (graphical) {
			dumpGraphVizTree(artifact);
		} else {
			artifact.dumpTree();
		}
	}

	/**
	 * @param artifact
	 *            artifact that should be printed
	 */
	private void dumpGraphVizTree(final ASTNodeArtifact artifact) {
		// header
		StringBuffer sb = new StringBuffer();
		sb.append("digraph ast {" + System.lineSeparator());
		sb.append("node [shape=ellipse];" + System.lineSeparator());
		sb.append("nodesep=0.8;" + System.lineSeparator());

		// nodes
		sb.append(artifact.dumpGraphvizTree(true));

		// footer
		sb.append("}");

		System.out.println(sb.toString());
	}

	/**
	 * @param context
	 * @param type
	 * @param left
	 * @param base
	 * @param right
	 * @param target
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void unorderedMerge(final MergeContext context,
			final MergeType type, final ASTNodeArtifact left,
			final ASTNodeArtifact base, final ASTNodeArtifact right,
			final ASTNodeArtifact target) throws IOException,
			InterruptedException {
		
		if (LOG.isTraceEnabled()) {
			LOG.trace("Performing unordered merge.");
		}

		ASTNodeArtifact[] revisions = { left, right };
		for (int i = 0; i < revisions.length; i++) {
			ASTNodeArtifact myNode = revisions[i];
			ASTNodeArtifact otherNode = revisions[(i + 1) % 2];
			
			for (ASTNodeArtifact myChild : myNode.getChildren()) {
				if (myChild.hasMatches()) {
					// is not a change
					Matching<ASTNodeArtifact> mOther 
					= myChild.getMatching(otherNode.getRevision());
					if (mOther != null) {
						// child is in both left and right -> merge it
						ASTNodeArtifact otherChild = mOther
								.getMatchingArtifact(myChild);

						// determine whether the child is 2 or 3-way merged
						Matching<ASTNodeArtifact> mBase 
						= myChild.getMatching(base.getRevision());
						ASTNodeArtifact baseChild = mBase == null ? base : mBase
								.getMatchingArtifact(myChild);
						MergeType childType = mBase == null ? MergeType.TWOWAY
								: MergeType.THREEWAY;

						ASTNodeArtifact targetChild = target.addChild(myChild);
						MergeTriple<ASTNodeArtifact> childTriple = new MergeTriple<ASTNodeArtifact>(
								childType, myChild, baseChild, otherChild);

						MergeOperation<ASTNodeArtifact> astMergeOp = new MergeOperation<ASTNodeArtifact>(
								childTriple, targetChild);

						astMergeOp.apply(context);
					} else {
						// child is in my revision and base. It was deleted by other.
						// we have to check if an inner node was changed by me
						Matching mLB = myChild.getMatching(base.getRevision());
						assert (mLB != null);
						if (myChild.hasChanges()) {
							// we need to report a conflict between leftNode and
							// its deletion
							throw new NotYetImplementedException();
						} else {
							// Node can safely be deleted
							DeleteOperation<ASTNodeArtifact> astDelOp = new DeleteOperation<ASTNodeArtifact>(
									myChild);
							astDelOp.apply(context);
						}
					}
				} else {
					// is a change
					throw new NotYetImplementedException();
				}
			}
		}
	}
	
	/**
	 * @param context
	 * @param type
	 * @param left
	 * @param base
	 * @param right
	 * @param target
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void orderedMerge(final MergeContext context,
			final MergeType type, final ASTNodeArtifact left,
			final ASTNodeArtifact base, final ASTNodeArtifact right,
			final ASTNodeArtifact target) throws IOException,
			InterruptedException {
		
		if (LOG.isTraceEnabled()) {
			LOG.trace("Performing ordered merge.");
		}
	}
}
