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
package de.fosd.jdime.stats;

/**
 * @author Olaf Lessenich
 *
 */
public class StatsElement {

    /**
     * Number of added elements.
     */
    private int added = 0;
    /**
     * Number of deleted elements.
     */
    private int deleted = 0;
    /**
     * Number of merged elements.
     */
    private int merged = 0;
    /**
     * Number of conflicting elements.
     */
    private int conflicting = 0;

    /**
     * Returns the number of added elements.
     *
     * @return number of added elements
     */
    public final int getAdded() {
        return added;
    }

    /**
     * Returns the number of merged elements.
     *
     * @return the number of merged elements
     */
    public final int getMerged() {
        return merged;
    }

    /**
     * Returns the number of conflicting elements.
     *
     * @return the number of conflicting elements
     */
    public final int getConflicting() {
        return conflicting;
    }

    /**
     * Returns the number of deleted elements.
     *
     * @return number of deleted elements
     */
    public final int getDeleted() {
        return deleted;
    }

    /**
     * Increments the number of added elements.
     */
    public final void incrementAdded() {
        added++;
    }

    /**
     * Increments the number of merged elements.
     */
    public final void incrementMerged() {
        merged++;
    }

    /**
     * Increments the number of conflicting elements.
     */
    public final void incrementConflicting() {
        conflicting++;
    }

    /**
     * Increments the number of deleted elements.
     */
    public final void incrementDeleted() {
        deleted++;
    }

    /**
     * Sets the number of added elements.
     *
     * @param added elements to set
     */
    public final void setAdded(final int added) {
        this.added = added;
    }

    /**
     * Sets the number of merged elements.
     *
     * @param merged elements to set
     */
    public final void setMerged(final int merged) {
        this.merged = merged;
    }

    /**
     * Sets the number of conflicting elements.
     *
     * @param conflicting elements to set
     */
    public final void setConflicting(final int conflicting) {
        this.conflicting = conflicting;
    }

    /**
     * Sets the number of deleted elements.
     *
     * @param deleted the deleted to set
     */
    public final void setDeleted(final int deleted) {
        this.deleted = deleted;
    }

    /**
     * Adds another StatsElement to this one.
     *
     * @param other StatsElement to add
     */
    public final void addStatsElement(final StatsElement other) {
        added += other.added;
        deleted += other.deleted;
        merged += other.merged;
        conflicting += other.conflicting;
    }
}
