/**
 * 
 */
package de.fosd.jdime.matcher;

import java.util.LinkedList;
import java.util.List;

import de.fosd.jdime.common.ASTNodeArtifact;

/**
 * @author Olaf Lessenich
 * 
 */
public class Matching {

	/**
	 * Algorithm that found this matching.
	 */
	private String algorithm;

	/**
	 * List of child matchings.
	 */
	private List<Matching> children = new LinkedList<Matching>();

	/**
	 * String representing a color to highlight the matching in.
	 */
	private Color color = null;

	/**
	 * Left node of the matching.
	 */
	private ASTNodeArtifact left;

	/**
	 * Right node of the matching.
	 */
	private ASTNodeArtifact right;

	/**
	 * Matching score. Higher score means more submatches.
	 */
	private int score;

	/**
	 * Creates a new empty matching instance.
	 */
	public Matching() {
		left = null;
		right = null;
		score = 0;
	}

	/**
	 * Creates a new matching instance.
	 * 
	 * @param left
	 *            left ASTNodeArtifact
	 * @param right
	 *            right ASTNodeArtifact
	 * @param score
	 *            number of matches
	 */
	public Matching(final ASTNodeArtifact left, final ASTNodeArtifact right,
			final int score) {
		this.left = left;
		this.right = right;
		this.score = score;
	}

	/**
	 * Returns a String representation of the algorithm that computed the
	 * matching.
	 * 
	 * @return algorithm used to compute the matching
	 */
	public final String getAlgorithm() {
		return algorithm;
	}

	/**
	 * Returns the matchings for child ASTNodeArtifacts.
	 * 
	 * @return matchings of child ASTNodeArtifacts
	 */
	public final List<Matching> getChildren() {
		return children;
	}

	/**
	 * @return the color
	 */
	public final Color getColor() {
		return color;
	}

	/**
	 * Returns the left ASTNodeArtifact of the matching.
	 * 
	 * @return left ASTNodeArtifact
	 */
	public final ASTNodeArtifact getLeftNode() {
		return left;
	}

	/**
	 * Returns the right ASTNodeArtifact of the matching.
	 * 
	 * @return right ASTNodeArtifact
	 */
	public final ASTNodeArtifact getRightNode() {
		return right;
	}

	/**
	 * Returns the number of matches.
	 * 
	 * @return number of matches
	 */
	public final int getScore() {
		return score;
	}

	/**
	 * Set a String representation of the algorithm used to compute the
	 * matching.
	 * 
	 * @param algorithm
	 *            used to compute the matching
	 */
	public final void setAlgorithm(final String algorithm) {
		this.algorithm = algorithm;
	}

	/**
	 * Sets the matchings for the respective child ASTNodeArtifacts.
	 * 
	 * @param children
	 *            matchings of child ASTNodeArtifacts
	 */
	public final void setChildren(final List<Matching> children) {
		this.children = children;
	}

	/**
	 * @param color
	 *            the color to set
	 */
	public final void setColor(final Color color) {
		this.color = color;
	}

	/**
	 * Returns the matching node for the node passed as argument. If the node is
	 * not contained in the matching, null is returned.
	 * 
	 * @param node node for which to return matching node
	 * @return matching node
	 */
	public final ASTNodeArtifact getMatchingNode(final ASTNodeArtifact node) {
		return left == node ? right : right == node ? left : null;
	}
}
