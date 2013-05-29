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
 * This class represents a tuple of files that are compared to each other.
 * @author Olaf Lessenich
 *
 */
public class DiffTuple {
	/**
	 * Left artifact.
	 */
	private Artifact left;
	
	/**
	 * Right artifact. 
	 */
	private Artifact right;
	
	/**
	 * Creates a new diff tuple.
	 * @param left artifact
	 * @param right artifact
	 */
	public DiffTuple(final Artifact left, final Artifact right) {
		this.left = left;
		this.right = right;
	}

	/**
	 * Returns the left artifact.
	 * @return the left
	 */
	public final Artifact getLeft() {
		return left;
	}

	/**
	 * Sets the left artifact.
	 * @param left the left to set
	 */
	public final void setLeft(final Artifact left) {
		this.left = left;
	}

	/**
	 * Returns the right artifact.
	 * @return the right
	 */
	public final Artifact getRight() {
		return right;
	}

	/**
	 * Sets the right artifact.
	 * @param right the right to set
	 */
	public final void setRight(final Artifact right) {
		this.right = right;
	}
}
