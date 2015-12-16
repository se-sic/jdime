/*
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2015 University of Passau, Germany
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
 */
package de.fosd.jdime.common;

import java.util.Objects;

/**
 * This class represents a revision.
 *
 * @author Olaf Lessenich
 */
public class Revision {

    /**
     * Name of the revision.
     */
    private String name;

    /**
     * Constructs a new <code>Revision</code> with the given name.
     *
     * @param name
     *         name of the revision
     */
    public Revision(String name) {
        this.name = name;
    }

    /**
     * Returns the name of the revision.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the revision.
     *
     * @param name
     *         the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns whether an artifact is contained in this revision.
     *
     * @param artifact
     *         artifact
     * @return true if the artifact is contained in this revision
     */
    public boolean contains(Artifact<?> artifact) {
        return artifact != null && artifact.hasMatching(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Revision revision = (Revision) o;

        return Objects.equals(name, revision.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
