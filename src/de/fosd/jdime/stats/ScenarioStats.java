/**
 * 
 */
package de.fosd.jdime.stats;

import de.fosd.jdime.common.MergeTriple;

/**
 * @author Olaf Lessenich
 * 
 */
public class ScenarioStats {
	/**
	 * 
	 */
	private MergeTriple<?> triple;

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
	 * Class Constructor.
	 * @param triple merge triple
	 * @param conflicts number of conflicts
	 * @param conflictingLines number of conflicting lines
	 * @param lines number of lines
	 * @param runtime runtime for the scenario
	 */
	public ScenarioStats(final MergeTriple<?> triple, final int conflicts,
			final int conflictingLines, final int lines, final long runtime) {
		this.triple = triple;
		this.conflicts = conflicts;
		this.conflictingLines = conflictingLines;
		this.lines = lines;
		this.runtime = runtime;
	}

	/**
	 * @return the triple
	 */
	public final MergeTriple<?> getTriple() {
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
	
	public String toString() {
		return triple.toString() + ": " + conflicts + " conflicts, "
				+ conflictingLines + " cloc, "
				+ lines + " loc, "
				+ runtime + " ms.";
	}
}
