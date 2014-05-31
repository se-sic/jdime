/*******************************************************************************
 * Copyright (C) 2013 Olaf Lessenich.
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
 *     Olaf Lessenich - initial API and implementation
 ******************************************************************************/
package de.fosd.jdime.common;

import java.util.LinkedList;

/**
 * @author Olaf Lessenich
 * @param <E> artifact element
 */
public class ArtifactList<E extends Artifact<E>> extends LinkedList<E> {

    /**
     *
     */
    private static final long serialVersionUID = 5294838641795231473L;

    /* (non-Javadoc)
     * @see java.util.AbstractCollection#toString()
     */
    @Override
    public final String toString() {
        return toString(" ");
    }

    /**
     * Returns a String representing a list of artifacts.
     *
     * @param sep separator
     * @return String representation
     */
    public final String toString(final String sep) {
        assert (sep != null);

        StringBuilder sb = new StringBuilder("");
        for (E element : this) {
            sb.append(element.getId());
            sb.append(sep);
        }

        return sb.toString();
    }
}
