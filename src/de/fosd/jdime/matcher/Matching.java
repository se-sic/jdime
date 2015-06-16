package de.fosd.jdime.matcher;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.UnorderedTuple;
import org.apache.commons.lang3.ClassUtils;
import org.apache.log4j.Logger;

/**
 * A container class representing a matching between two <code>T</code>s.
 *
 * @param <T> the type of the <code>Artifact</code>
 */
public class Matching<T extends Artifact<T>> implements Cloneable, Comparable<Matching<T>> {

	private static final Logger LOG = Logger.getLogger(ClassUtils.getShortClassName(Matching.class));

	/**
	 * The algorithm that found the matching.
	 */
	private String algorithm;

	/**
	 * The color to highlight the matching in.
	 */
	private Color highlightColor;

	private UnorderedTuple<T, T> matchedArtifacts;
	private int score;

	/**
	 * Constructs a new empty <code>Matching</code> with score 0.
	 */
	public Matching() {
		this(null, null, 0);
	}

	/**
	 * Constructs a new <code>Matching</code> between the two given <code>T</code>s.
	 *
	 * @param left the left <code>Artifact</code>
	 * @param right the right <code>Artifact</code>
	 * @param score the score of the matching
	 */
	public Matching(T left, T right, int score) {
		this(UnorderedTuple.of(left, right), score);
	}

	/**
	 * Constructs a new <code>Matching</code> between the two given <code>T</code>s.
	 *
	 * @param matchedArtifacts the two matched <code>Artifact</code>s
	 * @param score the score of the matching
	 */
	public Matching(UnorderedTuple<T, T> matchedArtifacts, int score) {
		this.matchedArtifacts = matchedArtifacts;
		this.score = score;
	}

	/**
	 * Returns the left <code>Artifact</code> of the matching.
	 *
	 * @return the left <code>Artifact</code>
	 */
	public T getLeft() {
		return matchedArtifacts.getX();
	}

	/**
	 * Returns the right <code>Artifact</code> of the matching.
	 *
	 * @return the right <code>Artifact</code>
	 */
	public T getRight() {
		return matchedArtifacts.getY();
	}

	/**
	 * Returns the matched <code>Artifact</code>s.
	 *
	 * @return the matched <code>Artifact</code>s
	 */
	public UnorderedTuple<T, T> getMatchedArtifacts() {
		return matchedArtifacts;
	}

	/**
	 * If one of the <code>Artifact</code>s contained in this <code>Matching</code> is referentially equal to
	 * <code>artifact</code> this method returns the other <code>Artifact</code> in this <code>Matching</code>.
	 * Otherwise <code>null</code> is returned.
	 *
	 * @param artifact
	 * 		the <code>Artifact</code> whose match is to be returned
	 * @return the match of the <code>artifact</code> or <code>null</code>
	 */
	public T getMatchingArtifact(Artifact<T> artifact) {
		T left = getLeft();
		T right = getRight();

		//if (LOG.isTraceEnabled()) {
		//	LOG.trace("artifact = " + artifact.getId());
		//	LOG.trace("left = " + left.getId());
		//	LOG.trace("right = " + right.getId());
		//}

		return left == artifact ? right : right == artifact ? left : null;
	}

	/**
	 * Replaces one of the <code>Artifact</code>s contained in this <code>Matching</code> if it has the same id (as
	 * per {@link Artifact#getId()}) with <code>artifact</code>. Do not use if you do not exactly know what you are
	 * doing.
	 *
	 * @param artifact
	 * 		the artifact to possibly insert into this <code>Matching</code>
	 */
	public void updateMatching(T artifact) {
		T left = getLeft();
		T right = getRight();

		if (left.getId().equals(artifact.getId())) {
			matchedArtifacts.setX(artifact);
		} else if (right.getId().equals(artifact.getId())) {
			matchedArtifacts.setY(artifact);
		}
	}

	/**
	 * Returns the score of the matching.
	 *
	 * @return the score
	 */
	public int getScore() {
		return score;
	}

	/**
	 * Sets score to the given value.
	 *
	 * @param score the new score
	 */
	public void setScore(int score) {
		this.score = score;
	}

	/**
	 * Returns a <code>String</code> describing the algorithm that found the matching.
	 *
	 * @return the algorithm description
	 */
	public String getAlgorithm() {
		return algorithm;
	}

	/**
	 * Sets the <code>String</code> describing the algorithm that found the matching to the given value.
	 *
	 * @param algorithm the new algorithm description
	 */
	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	/**
	 * Returns the color to highlight the matching in.
	 *
	 * @return the highlight color
	 */
	public Color getHighlightColor() {
		return highlightColor;
	}

	/**
	 * Sets the color to highlight the matching in to the given value.
	 *
	 * @param highlightColor the new highlight color
	 */
	public void setHighlightColor(Color highlightColor) {
		this.highlightColor = highlightColor;
	}

	@Override
	public String toString() {
		return String.format("(%s, %s) = %d", getLeft().getId(), getRight().getId(), score);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Matching<?> that = (Matching<?>) o;

		return matchedArtifacts.equals(that.matchedArtifacts);
	}

	@Override
	public int hashCode() {
		return matchedArtifacts.hashCode();
	}

	@Override
	@SuppressWarnings("unchecked") // the warning is inevitable but harmless
	public Matching<T> clone() {

		try {
			Matching<T> clone = (Matching<T>) super.clone();
			clone.matchedArtifacts = matchedArtifacts.clone();

			return clone;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int compareTo(Matching<T> o) {
		int dif = matchedArtifacts.getX().compareTo(o.getMatchedArtifacts().getX());

		if (dif != 0) {
			return dif;
		}

		dif = matchedArtifacts.getY().compareTo(o.getMatchedArtifacts().getY());

		if (dif != 0) {
			return dif;
		}

		return score - o.score;
	}
}
