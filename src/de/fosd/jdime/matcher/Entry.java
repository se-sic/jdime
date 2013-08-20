package de.fosd.jdime.matcher;


/**
 * A helper class used within the matrix of the LCST matcher.
 * @author lessenic
 *
 */
public class Entry {

	/**
	 * Direction.
	 */
	public Direction direction;

	/**
	 * Matching.
	 */
	public Matching matching;

	/**
	 * Creates a new entry.
	 * @param direction direction
	 * @param matching matching
	 */
	public Entry(final Direction direction, final Matching matching) {
		this.direction = direction;
		this.matching = matching;
	}
}