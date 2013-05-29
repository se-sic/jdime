/*******************************************************************************
 * Copyright (c) 2013 Olaf Lessenich.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Olaf Lessenich - initial API and implementation
 ******************************************************************************/
/**
 * 
 */
package de.fosd.jdime.common;

/**
 * This class represents a revision. 
 * @author Olaf Lessenich
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
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString() {
		return name;
	}
	
}
