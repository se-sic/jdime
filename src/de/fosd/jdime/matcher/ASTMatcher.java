/**
 * 
 */
package de.fosd.jdime.matcher;

import de.fosd.jdime.common.ASTNodeArtifact;

/**
 * @author Olaf Lessenich
 * 
 */
public final class ASTMatcher {
	/**
	 * 
	 */
	private ASTMatcher() {
		
	}
	
	private static int calls = 0;
	
	/**
	 * @param left artifact
	 * @param right artifact
	 * @return Matching
	 */
	public static Matching match(final ASTNodeArtifact left,
			final ASTNodeArtifact right) {
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
		
		calls++;

		return isOrdered ? OrderedASTMatcher.match(left, right)
				: UnorderedASTMatcher.match(left, right);
	}

	/**
	 * Marks corresponding nodes using an already computed matching. The
	 * respective nodes are flagged with <code>matchingFlag</code> and
	 * references are set to each other.
	 * 
	 * @param matching
	 *            used to mark nodes
	 */
	public static void storeMatching(final Matching matching) {
		ASTNodeArtifact left = matching.getLeftNode();
		ASTNodeArtifact right = matching.getRightNode();

		assert (left.matches(right));

		if (matching.getScore() > 0) {
			left.addMatching(matching);
			right.addMatching(matching);
		}

		for (Matching childMatching : matching.getChildren()) {
			storeMatching(childMatching);
		}
	}
	
	public static void reset() {
		calls = 0;
		OrderedASTMatcher.calls = 0;
		UnorderedASTMatcher.calls = 0;
	}
	
	public static String getLog() {
		StringBuffer sb = new StringBuffer();
		sb.append("matcher calls (all/ordered/unordered): ");
		sb.append(calls + "/");
		sb.append(OrderedASTMatcher.calls + "/");
		sb.append(UnorderedASTMatcher.calls);
		assert (calls == UnorderedASTMatcher.calls + OrderedASTMatcher.calls) :
			"Wrong sum for matcher calls";
		return sb.toString();
	}
}
