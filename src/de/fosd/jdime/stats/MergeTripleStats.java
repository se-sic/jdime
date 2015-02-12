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
package de.fosd.jdime.stats;

import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeTriple;

/**
 * TODO: high-level documentation
 * @author Olaf Lessenich
 *
 */
public class MergeTripleStats {

	private MergeTriple<FileArtifact> triple;
	private int conflicts;
	private int conflictingLines;
	private int lines;
	private long runtime;
	private boolean error = false;
	private String errormsg;
	private ASTStats astStats;
	private ASTStats leftStats;
	private ASTStats rightStats;

	/**
	 * Class Constructor.
	 *
	 * @param triple
	 *            merge triple
	 * @param conflicts
	 *            number of conflicts
	 * @param conflictingLines
	 *            number of conflicting lines
	 * @param lines
	 *            number of lines
	 * @param runtime
	 *            runtime for the scenario
	 */
	public MergeTripleStats(final MergeTriple<FileArtifact> triple,
			final int conflicts, final int conflictingLines, final int lines,
			final long runtime, final ASTStats astStats, final ASTStats leftStats, final ASTStats rightStats) {
		this.triple = triple;
		this.conflicts = conflicts;
		this.conflictingLines = conflictingLines;
		this.lines = lines;
		this.runtime = runtime;
		this.astStats = astStats;
		this.leftStats = leftStats;
		this.rightStats = rightStats;
	}

	/**
	 * Class constructor.
	 *
	 * @param triple
	 *            merge triple
	 * @param errormsg
	 *            error message
	 */
	public MergeTripleStats(final MergeTriple<FileArtifact> triple,
			final String errormsg) {
		this.triple = triple;
		this.error = true;
		this.errormsg = errormsg;
	}

	/**
	 * Returns true if there were errors during this merge.
	 *
	 * @return true if errors occurred during the merge
	 */
	public final boolean hasErrors() {
		return error;
	}

	/**
	 * Returns the error message.
	 *
	 * @return error message
	 */
	public final String getErrorMsg() {
		return errormsg;
	}

	/**
	 * @return the triple
	 */
	public final MergeTriple<FileArtifact> getTriple() {
		return triple;
	}

	/**
	 * @return the conflicts
	 */
	public final int getConflicts() {
		return conflicts;
	}

	/**
	 * @return the conflictingLines
	 */
	public final int getConflictingLines() {
		return conflictingLines;
	}

	/**
	 * @return the lines
	 */
	public final int getLines() {
		return lines;
	}

	/**
	 * @return the runtime
	 */
	public final long getRuntime() {
		return runtime;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString() {
		return triple.toString() + ": " + conflicts + " conflicts, "
				+ conflictingLines + " cloc, " + lines + " loc, " + runtime
				+ " ms.";
	}

	/**
	 * @return the astStats
	 */
	public final ASTStats getASTStats() {
		return astStats;
	}
	
	public final ASTStats getLeftASTStats() {
		return leftStats;
	}
	
	public final ASTStats getRightASTStats() {
		return rightStats;
	}
}
