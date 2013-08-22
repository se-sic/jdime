/**
 * 
 */
package de.fosd.jdime.matcher.ordered;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.matcher.Matcher;
import de.fosd.jdime.matcher.Matching;
import de.fosd.jdime.matcher.MatchingInterface;

/**
 * @author Olaf Lessenich
 * 
 * @param <T> type of artifact
 */
public abstract class OrderedMatcher<T extends Artifact<T>> 
	implements MatchingInterface<T> {
	
	/**
	 * Matcher.
	 */
	protected Matcher<T> matcher;

	/**
	 * Creates a new instance of OrderedMatcher.
	 * @param matcher matcher
	 */
	public OrderedMatcher(final Matcher<T> matcher) {
		this.matcher = matcher;
	}

	/**
	 * Compares two nodes.
	 * 
	 * @param left
	 *            left node
	 * @param right
	 *            right node
	 * @return matching
	 */
	public abstract Matching<T> match(final T left, final T right);

}
