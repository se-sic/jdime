package de.fosd.jdime.matcher;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.UnorderedTuple;

/**
 * A container class representing a matching between two <code>T</code>s.
 *
 * @param <T> the type of the <code>Artifact</code>
 */
public class NewMatching<T extends Artifact<T>> {

	private UnorderedTuple<T, T> matchedArtifacts;
	private int score;

	/**
	 * Constructs a new <code>NewMatching</code> between the two given <code>T</code>s.
	 *
	 * @param left the left <code>Artifact</code>
	 * @param right the right <code>Artifact</code>
	 * @param score the score of the matching
	 */
	public NewMatching(T left, T right, int score) {
		this(UnorderedTuple.of(left, right), score);
	}

	/**
	 * Constructs a new <code>NewMatching</code> between the two given <code>T</code>s.
	 *
	 * @param matchedArtifacts the two matched <code>Artifact</code>s
	 * @param score the score of the matching
	 */
	public NewMatching(UnorderedTuple<T, T> matchedArtifacts, int score) {
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
	 * Returns the score of the matching.
	 *
	 * @return the score
	 */
	public int getScore() {
		return score;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		NewMatching<?> that = (NewMatching<?>) o;

		return matchedArtifacts.equals(that.matchedArtifacts);
	}

	@Override
	public int hashCode() {
		return matchedArtifacts.hashCode();
	}
}
