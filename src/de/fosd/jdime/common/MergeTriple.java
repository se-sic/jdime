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

import java.io.IOException;

import de.fosd.jdime.common.operations.MergeOperation;

/**
 * This class represents a merge scenario for a standard three-way merge.
 * 
 * @author Olaf Lessenich
 * 
 */
public class MergeTriple {
	/**
	 * Type of merge.
	 */
	private MergeType mergeType;

	/**
	 * @return the mergeType
	 */
	public final MergeType getMergeType() {
		return mergeType;
	}

	/**
	 * Left artifact.
	 */
	private Artifact left;

	/**
	 * Base artifact.
	 */
	private Artifact base;

	/**
	 * Right artifact.
	 */
	private Artifact right;

	/**
	 * Creates a new merge triple.
	 * 
	 * @param mergeType
	 *            type of merge
	 * @param left
	 *            artifact
	 * @param base
	 *            artifact
	 * @param right
	 *            artifact
	 */
	public MergeTriple(final MergeType mergeType, final Artifact left,
			final Artifact base, final Artifact right) {
		this.mergeType = mergeType;
		this.left = left;
		this.base = base;
		this.right = right;
	}

	/**
	 * Returns the left artifact.
	 * 
	 * @return the left
	 */
	public final Artifact getLeft() {
		return left;
	}

	/**
	 * Sets the left artifact.
	 * 
	 * @param left
	 *            the left to set
	 */
	public final void setLeft(final Artifact left) {
		this.left = left;
	}

	/**
	 * Returns the base artifact.
	 * 
	 * @return the base
	 */
	public final Artifact getBase() {
		return base;
	}

	/**
	 * Sets the base artifact.
	 * 
	 * @param base
	 *            the base to set
	 */
	public final void setBase(final Artifact base) {
		this.base = base;
	}

	/**
	 * Returns the right artifact.
	 * 
	 * @return the right
	 */
	public final Artifact getRight() {
		return right;
	}

	/**
	 * Sets the right artifact.
	 * 
	 * @param right
	 *            the right to set
	 */
	public final void setRight(final Artifact right) {
		this.right = right;
	}

	/**
	 * Returns a String representing the MergeTriple.
	 * 
	 * @param sep
	 *            separator
	 * @param humanReadable
	 *            do not print dummy files if true
	 * @return String representation
	 */
	public final String toString(final String sep, 
			final boolean humanReadable) {
		StringBuilder sb = new StringBuilder("");
		sb.append(left.toString() + sep);

		if (!humanReadable || !base.isEmptyDummy()) {
			sb.append(base.toString() + sep);
		}

		sb.append(right.toString());
		return sb.toString();
	}

	/**
	 * Returns a String representing the MergeTriple separated by whitespace.
	 * 
	 * @return String representation
	 */
	public final String toString() {
		return toString(" ", false);
	}

	/**
	 * Returns a String representing the MergeTriple separated by whitespace,
	 * omitting empty dummy files.
	 * 
	 * @param humanReadable
	 *            do not print dummy files if true
	 * @return String representation
	 */
	public final String toString(final boolean humanReadable) {
		return toString(" ", humanReadable);
	}

	/**
	 * Merges this triple.
	 * 
	 * @param operation merge operation
	 * @param context
	 *            merge context
	 * @throws InterruptedException If a thread is interrupted
	 * @throws IOException If an input output exception occurs
	 */
	public final void merge(final MergeOperation operation, 
			final MergeContext context)
			throws IOException, InterruptedException {
		operation.getMergeTriple().getLeft().merge(operation, context);
	}
	
	/**
	 * Returns whether this is a valid merge triple.
	 * 
	 * @return true if the merge triple is valid.
	 */
	public final boolean isValid() {
		return left != null && base != null && right != null 
				&& left.getClass().equals(right.getClass()) 
				&& (base.isEmptyDummy() 
						|| base.getClass().equals(left.getClass()));
	}
}
