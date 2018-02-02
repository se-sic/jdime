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
package de.fosd.jdime.operations;

import java.util.concurrent.atomic.AtomicLong;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.config.merge.MergeContext;

/**
 * Abstract superclass for all operations being performed on {@link Artifact Artifacts}.
 *
 * @param <T>
 *         the type of the {@link Artifact Artifacts}
 */
public abstract class Operation<T extends Artifact<T>> {

    /**
     * Operation counter.
     */
    private static AtomicLong count = new AtomicLong();

    /**
     * Returns the number of {@link Operation Operations} that have been created.
     *
     * @return the number of {@link Operation Operations} that have been created
     */
    public static long getCount() {
        return count.get();
    }

    /**
     * The number of this {@link Operation}.
     */
    private long number;

    /**
     * Constructs a new {@link Operation}.
     */
    public Operation() {
        this.number = count.getAndIncrement();
    }

    /**
     * Applies this {@link Operation}.
     *
     * @param context
     *         the current {@link MergeContext}
     */
    public abstract void apply(MergeContext context);

    /**
     * Returns the number of this {@link Operation}.
     *
     * @return the number of this {@link Operation}
     */
    public long getNumber() {
        return number;
    }

    /**
     * Returns a {@link String} identifying this {@link Operation}.
     *
     * @return the identifying {@link String} for this {@link Operation}
     */
    public String getId() {
        return String.format("%s %d", getClass().getSimpleName(), number);
    }

    @Override
    public String toString() {
        return getId();
    }
}
