/**
 * 
 */
package de.fosd.jdime.matcher.unordered;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.matcher.Matcher;
import de.fosd.jdime.matcher.Matching;
import de.fosd.jdime.matcher.MatchingInterface;

/**
 * @author Olaf Lessenich
 * 
 * @param <T> type of artifact
 *
 */
public abstract class UnorderedMatcher<T extends Artifact<T>> 
	implements MatchingInterface<T> {
	
	/**
	 * 
	 */
	protected Matcher<T> matcher;
	
	/**
	 * Creates a new instance of UnorderedMatcher.
	 * @param matcher matcher
	 */
	public UnorderedMatcher(final Matcher<T> matcher) {
		this.matcher = matcher;
	}

	/**
	 * Returns the largest common subtree of two unordered trees.
	 * 
	 * @param left
	 *            left tree
	 * @param right
	 *            right tree
	 * @return largest common subtree of left and right tree
	 */
	public abstract Matching<T> match(final T left, final T right);
	
}
