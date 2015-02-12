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
 *******************************************************************************/
package de.fosd.jdime.common;

/**
 * @author Olaf Lessenich
 *
 */
public enum MergeType {

	/**
	 * Two-way merge.
	 */
	TWOWAY(2, "left", "right"),

	/**
	 * Three-way merge.
	 */
	THREEWAY(3, "left", "base", "right");

	/**
	 * At least two input files are needed.
	 */
	public static final int MINFILES = TWOWAY.numFiles;

	/**
	 * More than three input files are not supported at the moment.
	 */
	public static final int MAXFILES = THREEWAY.numFiles;

	/**
	 * Number of required input files.
	 */
	private int numFiles;

	/**
	 * Names of input revisions.
	 */
	private String[] revisions;

	/**
	 * Creates a new instance of MergeType.
	 *
	 * @param numFiles
	 *            number of required input files
	 * @param revisions
	 *            names of input revisions
	 */
	MergeType(final int numFiles, final String... revisions) {
		this.numFiles = numFiles;
		this.revisions = revisions;
	}

	/**
	 * Returns revision name of the input file at a certain position.
	 *
	 * @param pos
	 *            position of the input file
	 * @return revision name
	 */
	public String getRevision(final int pos) {
		return revisions[pos];
	}

	/**
	 * Returns number of required input files.
	 *
	 * @return required input files.
	 */
	public int getNumFiles() {
		return numFiles;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Enum#toString()
	 */
	@Override
	public final String toString() {
		return this.name();
	}
}
