/*******************************************************************************
 * Copyright (C) 2013, 2014 Olaf Lessenich.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 *
 * Contributors:
 *     Olaf Lessenich <lessenic@fim.uni-passau.de>
 *******************************************************************************/
package de.fosd.jdime.strategy;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.stats.Stats;

/**
 * @author Olaf Lessenich
 *
 * @param <T>
 *            type of artifact
 */
public interface StatsInterface<T extends Artifact<T>> {

	/**
	 * Creates and returns a new Stats Object specific to the strategy.
	 *
	 * @return new stats object
	 */
	Stats createStats();

	/**
	 * Returns key of statistical element.
	 *
	 * @param artifact
	 *            artifact
	 * @return key of statistical element
	 */
	String getStatsKey(final T artifact);
}
