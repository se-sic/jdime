/**
 * 
 */
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
	 * Number of changed elements.
	 */
	private int changed = 0;

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
	 * Returns the number of changed elements.
	 * 
	 * @return the number of changed elements
	 */
	public final int getChanged() {
		return changed;
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
		added = added + 1;
	}

	/**
	 * Increments the number of changed elements.
	 */
	public final void incrementChanged() {
		changed = changed + 1;
	}

	/**
	 * Increments the number of conflicting elements.
	 */
	public final void incrementConflicting() {
		conflicting = conflicting + 1;
	}

	/**
	 * Increments the number of deleted elements.
	 */
	public final void incrementDeleted() {
		deleted = deleted + 1;
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
	 * Sets the number of changed elements.
	 * 
	 * @param changed
	 *            elements to set
	 */
	public final void setChanged(final int changed) {
		this.changed = changed;
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
	 * Adds another StatsElement to this one.
	 * 
	 * @param other StatsElement to add
	 */
	public final void addStatsElement(final StatsElement other) {
		added = added + other.added;
		deleted = deleted + other.deleted;
		changed = changed + other.changed;
		conflicting = conflicting + other.conflicting;
	}
}
