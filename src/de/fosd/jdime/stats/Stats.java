/**
 * 
 */
package de.fosd.jdime.stats;

/**
 * @author Olaf Lessenich
 *
 */
public abstract class Stats {
	
	/**
	 * Adds the statistical data from another stats object.
	 * 
	 * @param other stats data to add
	 */
	public abstract void add(final Stats other);
}
