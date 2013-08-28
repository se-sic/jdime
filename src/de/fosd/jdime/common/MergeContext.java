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

import java.io.StringWriter;

import de.fosd.jdime.stats.Stats;
import de.fosd.jdime.strategy.LinebasedStrategy;
import de.fosd.jdime.strategy.MergeStrategy;

/**
 * @author Olaf Lessenich
 * 
 */
public class MergeContext {

	/**
	 * Class constructor.
	 */
	public MergeContext() {
		programStart = System.currentTimeMillis();
	}
	
	/**
	 * Whether we are in bugfixing mode.
	 */
	private boolean bugfixing = false;

	/**
	 * Whether to dump files/asts instead of merging.
	 */
	private boolean dump = false;

	/**
	 * Force overwriting of existing output files.
	 */
	private boolean forceOverwriting = false;

	/**
	 * Whether to use graphical output while dumping.
	 */
	private boolean guiDump = false;

	/**
	 * Input Files.
	 */
	private ArtifactList<FileArtifact> inputFiles;

	/**
	 * Strategy to apply for the merge.
	 */
	private MergeStrategy<?> mergeStrategy = new LinebasedStrategy();

	/**
	 * Output file.
	 */
	private FileArtifact outputFile;

	/**
	 * Timestamp of program start.
	 */
	private long programStart;

	/**
	 * If true, the output is quiet.
	 */
	private boolean quiet = false;

	/**
	 * Merge directories recursively. Can be set with the '-r' argument.
	 */
	private boolean recursive = false;

	/**
	 * Save statistical data.
	 */
	private boolean saveStats = false;

	/**
	 * Statistical data are stored in a stats object.
	 */
	private Stats stats = null;

	/**
	 * StdOut of a merge operation.
	 */
	private StringWriter stdErr = new StringWriter();

	/**
	 * StdIn of a merge operation.
	 */
	private StringWriter stdIn = new StringWriter();

	/**
	 * Adds statistical data to already collected data.
	 * 
	 * @param other
	 *            statistical data to add
	 */
	public final void addStats(final Stats other) {
		assert (stats != null);
		assert (other != null);
		stats.add(other);
	}

	/**
	 * Append a String to stdIN.
	 * 
	 * @param s
	 *            String to append
	 */
	public final void append(final String s) {
		if (stdIn != null) {
			stdIn.append(s);
		}
	}

	/**
	 * Appends a line to the saved stderr buffer.
	 * 
	 * @param line
	 *            to be appended
	 */
	public final void appendErrorLine(final String line) {
		if (stdErr != null) {
			stdErr.append(line);
			stdErr.append(System.getProperty("line.separator"));
		}
	}

	/**
	 * Appends a line to the saved stdin buffer.
	 * 
	 * @param line
	 *            to be appended
	 */
	public final void appendLine(final String line) {
		if (stdIn != null) {
			stdIn.append(line);
			stdIn.append(System.lineSeparator());
		}
	}

	/**
	 * @return the inputFiles
	 */
	public final ArtifactList<FileArtifact> getInputFiles() {
		return inputFiles;
	}

	/**
	 * Returns the merge strategy.
	 * 
	 * @return the merge strategy
	 */
	public final MergeStrategy<?> getMergeStrategy() {
		return mergeStrategy;
	}

	/**
	 * @return the outputFile
	 */
	public final FileArtifact getOutputFile() {
		return outputFile;
	}
	
	/**
	 * @return timestamp of program start
	 */
	public final long getProgramStart() {
		return programStart;
	}

	/**
	 * Retrieves the statistical data.
	 * 
	 * @return statistical data
	 */
	public final Stats getStats() {
		return stats;
	}

	/**
	 * Returns the saved stderr buffer.
	 * 
	 * @return stderr
	 */
	public final String getStdErr() {
		assert (stdErr != null);
		return stdErr.toString();
	}

	/**
	 * Returns the saved stdin buffer.
	 * 
	 * @return stdin
	 */
	public final String getStdIn() {
		assert (stdErr != null);
		return stdIn.toString();
	}

	/**
	 * Returns true if stderr is not empty.
	 * 
	 * @return true if stderr is not empty
	 */
	public final boolean hasErrors() {
		return stdErr != null && stdErr.toString().length() != 0;
	}

	/**
	 * Returns true if stdin is not empty.
	 * 
	 * @return true if stdin is not empty
	 */
	public final boolean hasOutput() {
		return stdIn != null && stdIn.toString().length() != 0;
	}

	/**
	 * @return the saveStats
	 */
	public final boolean hasStats() {
		return saveStats;
	}
	
	/**
	 * Returns whether bugfixing mode is enabled.
	 * @return true if bugfixing mode is enabled
	 */
	public final boolean isBugfixing() {
		return bugfixing;
	}

	/**
	 * @return the dumpTree
	 */
	public final boolean isDump() {
		return dump;
	}

	/**
	 * Returns true if overwriting of files in the output directory is forced.
	 * 
	 * @return whether overwriting of output files is forced
	 */
	public final boolean isForceOverwriting() {
		return forceOverwriting;
	}

	/**
	 * @return the guiDump
	 */
	public final boolean isGuiDump() {
		return guiDump;
	}

	/**
	 * Returns true if the output is quiet.
	 * 
	 * @return if output is quiet
	 */
	public final boolean isQuiet() {
		return quiet;
	}

	/**
	 * Returns whether directories are merged recursively.
	 * 
	 * @return true, if directories are merged recursively
	 */
	public final boolean isRecursive() {
		return recursive;
	}

	/**
	 * Resets the input streams.
	 */
	public final void resetStreams() {
		stdIn = new StringWriter();
		stdErr = new StringWriter();
	}
	
	/**
	 * Enables bugfixing mode.
	 */
	public final void setBugfixing() {
		bugfixing = true;
	}

	/**
	 * @param dumpTree
	 *            the dumpTree to set
	 */
	public final void setDump(final boolean dumpTree) {
		this.dump = dumpTree;
	}

	/**
	 * Sets whether overwriting of files in the output directory is forced.
	 * 
	 * @param forceOverwriting
	 *            overwrite files in the output directory
	 */
	public final void setForceOverwriting(final boolean forceOverwriting) {
		this.forceOverwriting = forceOverwriting;
	}

	/**
	 * @param guiDump
	 *            the guiDump to set
	 */
	public final void setGuiDump(final boolean guiDump) {
		this.guiDump = guiDump;
	}

	/**
	 * @param inputFiles
	 *            the inputFiles to set
	 */
	public final void setInputFiles(
			final ArtifactList<FileArtifact> inputFiles) {
		this.inputFiles = inputFiles;
	}

	/**
	 * Sets the merge strategy.
	 * 
	 * @param mergeStrategy
	 *            merge strategy
	 */
	public final void setMergeStrategy(final MergeStrategy<?> mergeStrategy) {
		this.mergeStrategy = mergeStrategy;
	}

	/**
	 * @param outputFile
	 *            the outputFile to set
	 */
	public final void setOutputFile(final FileArtifact outputFile) {
		this.outputFile = outputFile;
	}

	/**
	 * Sets whether the output is quiet or not.
	 * 
	 * @param quiet
	 *            do not print merge results to stdout
	 */
	public final void setQuiet(final boolean quiet) {
		this.quiet = quiet;
	}

	/**
	 * Set whether directories are merged recursively.
	 * 
	 * @param recursive
	 *            directories are merged recursively
	 */
	public final void setRecursive(final boolean recursive) {
		this.recursive = recursive;
	}

	/**
	 * @param saveStats
	 *            the saveStats to set
	 */
	public final void setSaveStats(final boolean saveStats) {
		this.saveStats = saveStats;

		if (saveStats) {
			stats = mergeStrategy.createStats();
		}
	}
}
