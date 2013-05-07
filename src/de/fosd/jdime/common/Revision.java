/**
 * 
 */
package de.fosd.jdime.common;

/**
 * This class represents a revision. 
 * @author lessenic
 *
 */
public class Revision {
	/**
	 * Name of the revision.
	 */
	private String name;
	
	/**
	 * Returns the name of the revision.
	 * @return the name
	 */
	public final String getName() {
		return name;
	}

	/**
	 * Sets the name of the revision.
	 * @param name the name to set
	 */
	public final void setName(final String name) {
		this.name = name;
	}

	/**
	 * Creates a new instance of revision.
	 * @param name name of the revision
	 */
	public Revision(final String name) {
		this.name = name;
	}
	
}
