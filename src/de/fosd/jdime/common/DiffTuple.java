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
 * @param <T> type of artifact
 *
 */
public class DiffTuple <T extends Artifact<?>> {
	/**
	 * Left artifact.
	 */
	private T left;
	
	/**
	 * Right artifact. 
	 */
	private T right;
	
	/**
	 * Creates a new diff tuple.
	 * @param left artifact
	 * @param right artifact
	 */
	public DiffTuple(final T left, final T right) {
		this.left = left;
		this.right = right;
	}

	/**
	 * Returns the left artifact.
	 * @return the left
	 */
	public final T getLeft() {
		return left;
	}

	/**
	 * Returns the right artifact.
	 * @return the right
	 */
	public final T getRight() {
		return right;
	}

	/**
	 * Sets the left artifact.
	 * @param left the left to set
	 */
	public final void setLeft(final T left) {
		this.left = left;
	}

	/**
	 * Sets the right artifact.
	 * @param right the right to set
	 */
	public final void setRight(final T right) {
		this.right = right;
	}
}
