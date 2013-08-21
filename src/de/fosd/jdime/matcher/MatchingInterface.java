/**
 * 
 */
package de.fosd.jdime.matcher;

import de.fosd.jdime.common.Artifact;

/**
 * @author Olaf Lessenich
 *
 * @param <T>
 */
public interface MatchingInterface<T extends Artifact<T>> {
	/**
	 * Returns a tree of matches for the provided artifacts.
	 * @param left artifact
	 * @param right artifact
	 * @return tree of matches
	 */
	Matching<T> match(final T left, final T right);
}
