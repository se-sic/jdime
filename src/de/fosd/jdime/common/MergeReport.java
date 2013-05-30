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

import java.io.BufferedReader;
import java.io.StringReader;
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
	
	/**
	 * Operation that was/will be applied.
	 */
	private Operation operation;
	
	/**
	 * If set to true, no output is saved.
	 */
	private boolean quiet;
	
	/**
	 * @return if quiet
	 */
	public final boolean isQuiet() {
		return quiet;
	}

	/**
	 * Creates a new instance of MergeReport.
	 *
	 * @param operation operation to apply
	 * @param quiet If true, no output is saved.
	 */
	public MergeReport(final Operation operation, final boolean quiet) {
		stdIn = new StringWriter();
		stdErr = new StringWriter();
		this.operation = operation;
		this.quiet = quiet;
	}

	/**
	 * Creates a new instance of MergeReport.
	 *
	 * @param operation operation to apply
	 */
	public MergeReport(final Operation operation) {
		this(operation, false);
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
	 * Returns a reader that can be used to retrieve the content of the
	 * report.
	 * 
	 * @return Reader reader
	 */
	public final BufferedReader getReader() {
		return new BufferedReader(new StringReader(stdIn.toString()));
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

	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getStdIn();
	}

}
