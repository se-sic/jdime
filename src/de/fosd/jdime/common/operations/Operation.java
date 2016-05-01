/**
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
 *     Georg Seibt <seibt@fim.uni-passau.de>
 */
package de.fosd.jdime.common.operations;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;

/**
 * This class represents an operation that is applied to <code>Artifact</code>s.
 *
 * @param <T>
 *         type of artifact
 * @author Olaf Lessenich
 */
public abstract class Operation<T extends Artifact<T>> {

    /**
     * Operation counter.
     */
    private static long count = 1;

    /**
     * Returns counter value.
     *
     * @return counter value
     */
    public static long getCount() {
        return count;
    }

    /**
     * Number of the current operation.
     */
    private long number;

    /**
     * Returns a new instance of operation.
     */
    public Operation() {
        this.number = count;
        count++;
    }

    /**
     * Applies the operation.
     *
     * @param context
     *         merge context
     */
    public abstract void apply(MergeContext context);

    /**
     * Returns the name of the operation.
     *
     * @return name of the operation
     */
    public abstract String getName();

    @Override
    public abstract String toString();

    /**
     * Returns the number of the operation.
     *
     * @return number
     */
    public long getNumber() {
        return number;
    }

    /**
     * Returns an ID.
     *
     * @return id
     */
    public String getId() {
        return "OP" + number;
    }
}
