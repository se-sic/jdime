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
package de.fosd.jdime.stats;

import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeTriple;

/**
 * @author Olaf Lessenich
 * 
 */
public class MergeTripleStats {
	/**
	 * 
	 */
	private MergeTriple<FileArtifact> triple;

	/**
	 * 
	 */
	private int conflicts;

	/**
	 * 
	 */
	private int conflictingLines;

	/**
	 * 
	 */
	private int lines;

	/**
	 * 
	 */
	private long runtime;
	
	/**
	 * 
	 */
	private boolean error = false;
	
	/**
	 * 
	 */
	private String errormsg;

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
			final long runtime) {
		this.triple = triple;
		this.conflicts = conflicts;
		this.conflictingLines = conflictingLines;
		this.lines = lines;
		this.runtime = runtime;
	}
	
	/**
	 * Class constructor.
	 * 
	 * @param errormsg error message
	 */
	public MergeTripleStats(final String errormsg) {
		this.error = true;
		this.errormsg = errormsg;
	}
	
	/**
	 * Returns true if there were errors during this merge.
	 * @return true if errors occurred during the merge
	 */
	public final boolean hasErrors() {
		return error;
	}
	
	/**
	 * Returns the error message.
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

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString() {
		return triple.toString() + ": " + conflicts + " conflicts, "
				+ conflictingLines + " cloc, " + lines + " loc, " + runtime
				+ " ms.";
	}
}
