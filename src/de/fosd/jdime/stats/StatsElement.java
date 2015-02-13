/*******************************************************************************
 * Copyright (C) 2013-2015 Olaf Lessenich.
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
package de.fosd.jdime.stats;

/**
 * @author Olaf Lessenich
 *
 */
public class StatsElement {
	/**
	 * Number of elements.
	 */
	private int elements = 0;

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
	 * Number of matched elements.
	 */
	private int matches = 0;

	/**
	 * Number of changed elements.
	 */
	private int changes = 0;

	public int getChanges() {
		return changes;
	}

	public void setChanges(int changes) {
		this.changes = changes;
	}

	public void incrementChanges() {
		changes++;
	}

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
	 * Returns the number of matched elements.
	 * 
	 * @return matches
	 */
	public final int getMatches() {
		return matches;
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
	 * Increments the number of matched elements.
	 */
	public final void incrementMatches() {
		matches++;
	}

	/**
	 * Sets the number of added elements.
	 *
	 * @param added
	 *            elements to set
	 */
	public final void setAdded(final int added) {
		this.added = added;
	}

	/**
	 * Sets the number of merged elements.
	 *
	 * @param merged
	 *            elements to set
	 */
	public final void setMerged(final int merged) {
		this.merged = merged;
	}

	/**
	 * Sets the number of conflicting elements.
	 *
	 * @param conflicting
	 *            elements to set
	 */
	public final void setConflicting(final int conflicting) {
		this.conflicting = conflicting;
	}

	/**
	 * Sets the number of deleted elements.
	 *
	 * @param deleted
	 *            the deleted to set
	 */
	public final void setDeleted(final int deleted) {
		this.deleted = deleted;
	}

	/**
	 * Sets the number of matched elements.
	 * 
	 * @param matches
	 *            matches
	 */
	public final void setMatches(final int matches) {
		this.matches = matches;
	}

	/**
	 * Adds another StatsElement to this one.
	 *
	 * @param other
	 *            StatsElement to add
	 */
	public final void addStatsElement(final StatsElement other) {
		added += other.added;
		deleted += other.deleted;
		merged += other.merged;
		conflicting += other.conflicting;
		matches += other.matches;
		changes += other.changes;
		elements += other.elements;
	}

	/**
	 * @return the elements
	 */
	public final int getElements() {
		return elements;
	}

	/**
	 * @param elements
	 *            the elements to set
	 */
	public final void setElements(final int elements) {
		this.elements = elements;
	}

	public final void incrementElements() {
		elements++;
	}

	public StatsElement copy() {
		return copy(new StatsElement());
	}

	public StatsElement copy(StatsElement copy) {
		copy.setAdded(added);
		copy.setConflicting(conflicting);
		copy.setDeleted(deleted);
		copy.setElements(elements);
		copy.setMatches(matches);
		copy.setMerged(merged);
		copy.setChanges(changes);
		return copy;
	}
}
