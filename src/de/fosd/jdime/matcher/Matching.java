/**
 * 
 */
package de.fosd.jdime.matcher;

import java.util.LinkedList;
import java.util.List;

import de.fosd.jdime.common.Artifact;

/**
 * @author Olaf Lessenich
 *
 * @param <T> type of artifact
 */
public class Matching<T extends Artifact<T>> {

	/**
	 * Algorithm that found this matching.
	 */
	private String algorithm;

	/**
	 * List of child matchings.
	 */
	private List<Matching<T>> children = new LinkedList<Matching<T>>();

	/**
	 * String representing a color to highlight the matching in.
	 */
	private Color color = null;

	/**
	 * Left artifact of the matching.
	 */
	private T left;

	/**
	 * Right artifact of the matching.
	 */
	private T right;

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
	 *            left T
	 * @param right
	 *            right T
	 * @param score
	 *            number of matches
	 */
	public Matching(final T left, final T right,
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
	 * Returns the matchings for child Ts.
	 * 
	 * @return matchings of child Ts
	 */
	public final List<Matching<T>> getChildren() {
		return children;
	}

	/**
	 * @return the color
	 */
	public final Color getColor() {
		return color;
	}

	/**
	 * Returns the left T of the matching.
	 * 
	 * @return left T
	 */
	public final T getLeft() {
		return left;
	}

	/**
	 * Returns the right T of the matching.
	 * 
	 * @return right T
	 */
	public final T getRight() {
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
	 * Sets the matchings for the respective child Ts.
	 * 
	 * @param children
	 *            matchings of child Ts
	 */
	public final void setChildren(final List<Matching<T>> children) {
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
	 * Returns the matching artifact for the artifact passed as argument. 
	 * If the artifact is not contained in the matching, null is returned.
	 * 
	 * @param artifact artifact for which to return matching artifact
	 * @return matching artifact
	 */
	public final T getMatchingArtifact(final T artifact) {
		return left == artifact ? right : right == artifact ? left : null;
	}
}
