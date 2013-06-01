/**
 * 
 */
package de.fosd.jdime.common;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.StringWriter;

import de.fosd.jdime.strategy.LinebasedStrategy;
import de.fosd.jdime.strategy.MergeStrategy;

/**
 * @author Olaf Lessenich
 *
 */
public class MergeContext {
	/**
	 * Strategy to apply for the merge.
	 */
	private MergeStrategy mergeStrategy = new LinebasedStrategy();
	
	/**
	 * StdIn of a merge operation.
	 */
	private StringWriter stdIn = new StringWriter();

	/**
	 * StdOut of a merge operation.
	 */
	private StringWriter stdErr = new StringWriter();

	/**
	 * If true, the output is quiet.
	 */
	private boolean quiet = false;
	
	/**
	 * Merge directories recursively. Can be set with the '-r' argument.
	 */
	private boolean recursive = false;
	
	/**
	 * @return the recursive
	 */
	public final boolean isRecursive() {
		return recursive;
	}

	/**
	 * @param recursive the recursive to set
	 */
	public final void setRecursive(final boolean recursive) {
		this.recursive = recursive;
	}
	/**
	 * Force overwriting of existing output files.
	 */
	private boolean forceOverwriting = false;

	/**
	 * @param forceOverwriting the forceOverwriting to set
	 */
	public final void setForceOverwriting(final boolean forceOverwriting) {
		this.forceOverwriting = forceOverwriting;
	}

	/**
	 * @return the forceOverwriting
	 */
	public final boolean isForceOverwriting() {
		return forceOverwriting;
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
	 * Sets whether the output is quiet or not.
	 * 
	 * @param quiet whether output is quiet
	 */
	public final void setQuiet(final boolean quiet) {
		this.quiet = quiet;
	}

	/**
	 * Returns the merge strategy.
	 * @return the merge strategy
	 */
	public final MergeStrategy getMergeStrategy() {
		return mergeStrategy;
	}

	/**
	 * Sets the merge strategy.
	 * @param mergeStrategy the merge strategy to set
	 */
	public final void setMergeStrategy(final MergeStrategy mergeStrategy) {
		this.mergeStrategy = mergeStrategy;
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
			stdIn.append(System.getProperty("line.separator"));
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
	 * Returns a reader that can be used to retrieve the content of the
	 * report.
	 * 
	 * @return Reader reader
	 */
	public final BufferedReader getReader() {
		assert (stdIn != null);
		return new BufferedReader(new StringReader(stdIn.toString()));
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
	 * Returns the saved stderr buffer.
	 * 
	 * @return stderr
	 */
	public final String getStdErr() {
		assert (stdErr != null);
		return stdErr.toString();
	}
	
	/**
	 * Returns true if stderr is not empty.
	 * 
	 * @return true if stderr is not empty
	 */
	public final boolean hasErrors() {
		return stdErr != null && stdErr.toString().length() != 0;
	}
}
