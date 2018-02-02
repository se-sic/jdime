/**
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2017 University of Passau, Germany
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
 *     Georg Seibt <seibt@fim.uni-passau.de>
 */
package de.fosd.jdime.matcher.ordered.simpleTree;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.matcher.matching.Matchings;

/**
 * A helper class used within the matrix of the LCST matcher.
 *
 * @author Olaf Lessenich
 *
 * @param <T>
 *            type of artifact
 *
 */
public class Entry<T extends Artifact<T>> {

    private Direction direction;
    private Matchings<T> matching;

    /**
     * Creates a new entry.
     *
     * @param direction
     *            direction
     * @param matching
     *            matching
     */
    public Entry(final Direction direction, final Matchings<T> matching) {
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
     * @param direction
     *            the direction to set
     */
    public final void setDirection(final Direction direction) {
        this.direction = direction;
    }

    /**
     * @return the matching
     */
    public final Matchings<T> getMatching() {
        return matching;
    }

    /**
     * @param matching
     *            the matching to set
     */
    public final void setMatching(final Matchings<T> matching) {
        this.matching = matching;
    }
}
