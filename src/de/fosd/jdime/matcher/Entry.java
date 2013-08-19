package de.fosd.jdime.matcher;


/**
 * A helper class used within the matrix of the LCST matcher
 * @author lessenic
 *
 */
public class Entry {

	public Direction direction;

	public Matching matching;

	public Entry(Direction direction, Matching matching) {
		this.direction = direction;
		this.matching = matching;
	}
}