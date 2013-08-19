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
	private ASTNodeArtifact left;

	private ASTNodeArtifact right;

	private int score;

	private List<Matching> children = new LinkedList<Matching>();

	private String algorithm;

	/**
	 * Returns a String representation of the algorithm that computed the
	 * matching
	 * 
	 * @return algorithm used to compute the matching
	 */
	public String getAlgorithm() {
		return algorithm;
	}

	/**
	 * Set a String representation of the algorithm used to compute the matching
	 * 
	 * @param algorithm
	 *            used to compute the matching
	 */
	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	/**
	 * Creates a new matching instance
	 * 
	 * @param left
	 *            left ASTNodeArtifact
	 * @param right
	 *            right ASTNodeArtifact
	 * @param score
	 *            number of matches
	 */
	public Matching(ASTNodeArtifact left, ASTNodeArtifact right, int score) {
		this.left = left;
		this.right = right;
		this.score = score;
	}

	/**
	 * Creates a new empty matching instance
	 */
	public Matching() {
		left = null;
		right = null;
		score = 0;
	}

	/**
	 * Sets the matchings for the respective child ASTNodeArtifacts
	 * 
	 * @param children
	 *            matchings of child ASTNodeArtifacts
	 */
	public void setChildren(List<Matching> children) {
		this.children = children;
	}

	/**
	 * Returns the matchings for child ASTNodeArtifacts
	 * 
	 * @return matchings of child ASTNodeArtifacts
	 */
	public List<Matching> getChildren() {
		return children;
	}

	/**
	 * Returns the number of matches
	 * 
	 * @return number of matches
	 */
	public int getScore() {
		return score;
	}

	/**
	 * Returns the left ASTNodeArtifact of the matching
	 * 
	 * @return left ASTNodeArtifact
	 */
	public ASTNodeArtifact getLeftNode() {
		return left;
	}

	/**
	 * Returns the right ASTNodeArtifact of the matching
	 * 
	 * @return right ASTNodeArtifact
	 */
	public ASTNodeArtifact getRightNode() {
		return right;
	}
}
