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
package de.fosd.jdime.matcher;

import de.fosd.jdime.common.Artifact;


/**
 * A helper class used within the matrix of the LCST matcher.
 * @author lessenic
 * 
 * @param <T> type of artifact
 *
 */
public class Entry<T extends Artifact<T>> {

	/**
	 * Direction.
	 */
	private Direction direction;

	/**
	 * Matching.
	 */
	private Matching<T> matching;

	/**
	 * Creates a new entry.
	 * @param direction direction
	 * @param matching matching
	 */
	public Entry(final Direction direction, final Matching<T> matching) {
		this.direction = direction;
		this.matching = matching;
	}

	/**
	 * @return the direction
	 */
	public final Direction getDirection() {
		return direction;
	}

	/**
	 * @param direction the direction to set
	 */
	public final void setDirection(final Direction direction) {
		this.direction = direction;
	}

	/**
	 * @return the matching
	 */
	public final Matching<T> getMatching() {
		return matching;
	}

	/**
	 * @param matching the matching to set
	 */
	public final void setMatching(final Matching<T> matching) {
		this.matching = matching;
	}
}
