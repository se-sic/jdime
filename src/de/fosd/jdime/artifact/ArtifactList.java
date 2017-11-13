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
package de.fosd.jdime.artifact;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * An <code>ArrayList</code> of <code>Artifact</code>s. Its {@link #toString()} method is overridden to use
 * {@link Artifact#getId()} to represent its contents.
 *
 * @param <E>
 *         the type of elements held by this collection
 *
 * @author Olaf Lessenich
 * @see Artifact
 */
public class ArtifactList<E extends Artifact<E>> extends ArrayList<E> {

    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_SEP = " ";

    /**
     * Equivalent to {@link ArrayList#ArrayList()}.
     */
    public ArtifactList() {}

    /**
     * Equivalent to {@link ArrayList#ArrayList(int)}.
     */
    public ArtifactList(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Equivalent to {@link ArrayList#ArrayList(Collection)}.
     */
    public ArtifactList(Collection<? extends E> c) {
        super(c);
    }

    @Override
    public String toString() {
        return toString(DEFAULT_SEP);
    }

    /**
     * Returns a string representation of this collection. The string representation consists of a list of the
     * collection's elements in the order they are returned by its iterator. Adjacent elements are separated by the
     * given <code>separator</code>. Elements are converted to strings as by {@link Artifact#getId()}.
     *
     * @param separator
     *         the separator to be used
     *
     * @return a string representation of this collection
     */
    private String toString(String separator) {
        return String.join(separator, stream().map(E::getId).collect(Collectors.toList()));
    }
}
