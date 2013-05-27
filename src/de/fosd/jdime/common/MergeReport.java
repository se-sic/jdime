/**
 * 
 */
package de.fosd.jdime.common;

import java.io.StringWriter;

/**
 * @author lessenic
 * 
 */
public class MergeReport {

	/**
	 * StdIn of a merge operation.
	 */
	private StringWriter stdIn;

	/**
	 * StdOut of a merge operation.
	 */
	private StringWriter stdErr;

	/**
	 * Type of merge.
	 */
	private MergeType mergeType;

	/**
	 * List of input files.
	 */
	private MergeTriple mergeTriple;

	/**
	 * Creates a new instance of MergeReport.
	 * 
	 * @param mergeType
	 *            Type of merge
	 * @param mergeTriple
	 *            merge scenario
	 */
	public MergeReport(final MergeType mergeType,
			final MergeTriple mergeTriple) {
		stdIn = new StringWriter();
		stdErr = new StringWriter();
		this.mergeType = mergeType;
		this.mergeTriple = mergeTriple;
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
	 * Returns the saved stdin buffer.
	 * 
	 * @return stdin
	 */
	public final String getStdIn() {
		return stdIn.toString();
	}

	/**
	 * Returns the saved stderr buffer.
	 * 
	 * @return stderr
	 */
	public final String getStdErr() {
		return stdErr.toString();
	}

	/**
	 * Returns true if stderr is not empty.
	 * 
	 * @return true if stderr is not empty
	 */
	public final boolean hasErrors() {
		return stdErr.toString().length() != 0;
	}

	/**
	 * Returns merge type.
	 * 
	 * @return merge type.
	 */
	public final MergeType getMergeType() {
		return mergeType;
	}

	/**
	 * Returns the merge triple.
	 * 
	 * @return merge triple
	 */
	public final MergeTriple getMergeTriple() {
		return mergeTriple;
	}

}
