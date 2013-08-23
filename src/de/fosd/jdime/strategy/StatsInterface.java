/*******************************************************************************
 * Copyright (c) 2013 Olaf Lessenich.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Olaf Lessenich - initial API and implementation
 ******************************************************************************/
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
