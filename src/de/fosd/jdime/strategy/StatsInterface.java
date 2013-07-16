/**
 * 
 */
package de.fosd.jdime.strategy;

import de.fosd.jdime.stats.Stats;

/**
 * @author lessenic
 *
 */
public interface StatsInterface {
	/**
	 * Creates and returns a new Stats Object specific to the strategy.
	 * @return new stats object
	 */
	Stats createStats();
}
