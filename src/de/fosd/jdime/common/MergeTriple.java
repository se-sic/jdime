/*******************************************************************************
 * Copyright (C) 2013, 2014 Olaf Lessenich.
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
 *******************************************************************************/
package de.fosd.jdime.common;

/**
 * This class represents a merge scenario for a standard three-way merge.
 *
 * @author Olaf Lessenich
 *
 * @param <T>
 *            type of artifact
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
	public MergeTriple(final MergeType mergeType, final T left, final T base,
			final T right) {
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
		return left != null
				&& base != null
				&& right != null
				&& left.getClass().equals(right.getClass())
				&& (base.isEmptyDummy() || base.getClass().equals(
						left.getClass()));
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
	@Override
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
	public final String toString(final String sep, final boolean humanReadable) {
		StringBuilder sb = new StringBuilder("");
		sb.append(left.getId()).append(sep);

		if (!humanReadable || !base.isEmptyDummy()) {
			sb.append(base.getId()).append(sep);
		}

		sb.append(right.getId());
		return sb.toString();
	}

	/**
	 * Returns a list containing all three revision. Empty dummies for base are
	 * included.
	 *
	 * @return list of artifacts
	 */
	public final ArtifactList<T> getList() {
		ArtifactList<T> list = new ArtifactList<>();
		list.add(left);
		list.add(base);
		list.add(right);
		return list;
	}
}
