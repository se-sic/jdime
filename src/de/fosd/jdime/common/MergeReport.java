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

import de.fosd.jdime.common.operations.Operation;

/**
 * @author Olaf Lessenich
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
	
	private Operation operation;

	/**
	 * Creates a new instance of MergeReport.
	 * 
	 * @param mergeType
	 *            Type of merge
	 * @param mergeTriple
	 *            merge scenario
	 */
	public MergeReport(final Operation operation) {
		stdIn = new StringWriter();
		stdErr = new StringWriter();
		this.operation = operation;
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
	 * Returns operation.
	 * 
	 * @return operation.
	 */
	public final Operation getOperation() {
		return operation;
	}

	
	public String toString() {
		return getStdIn();
	}

}
