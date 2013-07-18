/**
 * 
 */
package de.fosd.jdime.strategy;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.stats.Stats;


/**
 * @author lessenic
 *
 * @param <T> type of artifact
 */
public interface StatsInterface<T extends Artifact<T>> {
	/**
	 * Creates and returns a new Stats Object specific to the strategy.
	 * @return new stats object
	 */
	Stats createStats();
	
	/**
	 * Returns key of statistical element.
	 * @param artifact artifact
	 * @return key of statistical element
	 */
	String getStatsKey(final T artifact);
}
