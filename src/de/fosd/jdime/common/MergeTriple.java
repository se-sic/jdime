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
 * @param <T> type of artifact
 * 
 */
public class MergeTriple<T extends Artifact<T>> {
	/**
	 * Type of merge.
	 */
	private MergeType mergeType;

	/**
	 * Left artifact.
	 */
	private T left;

	/**
	 * Base artifact.
	 */
	private T base;

	/**
	 * Right artifact.
	 */
	private T right;

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
	public MergeTriple(final MergeType mergeType, final T left,
			final T base, final T right) {
		this.mergeType = mergeType;
		this.left = left;
		this.base = base;
		this.right = right;
	}

	/**
	 * Returns the base artifact.
	 * 
	 * @return the base
	 */
	public final T getBase() {
		return base;
	}

	/**
	 * Returns the left artifact.
	 * 
	 * @return the left
	 */
	public final T getLeft() {
		return left;
	}

	/**
	 * Returns the type of merge.
	 * 
	 * @return type of merge
	 */
	public final MergeType getMergeType() {
		return mergeType;
	}

	/**
	 * Returns the right artifact.
	 * 
	 * @return the right
	 */
	public final T getRight() {
		return right;
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

	/**
	 * Merges this triple.
	 * 
	 * @param operation merge operation
	 * @param context
	 *            merge context
	 * @throws InterruptedException If a thread is interrupted
	 * @throws IOException If an input output exception occurs
	 */
	public final void merge(final MergeOperation<T> operation, 
			final MergeContext<T> context)
			throws IOException, InterruptedException {
		operation.getMergeTriple().getLeft().merge(operation, context);
	}

	/**
	 * Sets the base artifact.
	 * 
	 * @param base
	 *            the base to set
	 */
	public final void setBase(final T base) {
		this.base = base;
	}

	/**
	 * Sets the left artifact.
	 * 
	 * @param left
	 *            the left to set
	 */
	public final void setLeft(final T left) {
		this.left = left;
	}

	/**
	 * Sets the right artifact.
	 * 
	 * @param right
	 *            the right to set
	 */
	public final void setRight(final T right) {
		this.right = right;
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
}
