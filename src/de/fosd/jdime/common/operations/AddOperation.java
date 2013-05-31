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
package de.fosd.jdime.common.operations;

import java.io.BufferedReader;
import java.io.IOException;

import de.fosd.jdime.Main;
import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeReport;
import de.fosd.jdime.common.NotYetImplementedException;

/**
 * The operation adds <code>Artifact</code>s.
 * 
 * @author Olaf Lessenich
 * 
 */
public class AddOperation extends Operation {
	/**
	 * Logger.
	 */
	//private static final Logger LOG = Logger.getLogger(AddOperation.class);
	
	/**
	 * The <code>Artifact</code> that is added by the operation.
	 */
	private Artifact artifact;

	/**
	 * The output <code>Artifact</code>.
	 */
	private Artifact output;
	
	/**
	 * Sets the output <code>Artifact</code>.
	 * 
	 * @param output
	 *            the output to set
	 */
	public final void setOutput(final Artifact output) {
		this.output = output;
	}

	/**
	 * Class constructor.
	 * 
	 * @param artifact
	 *            that is added by the operation.
	 * @param output output artifact
	 */
	public AddOperation(final Artifact artifact, final Artifact output) {
		this.artifact = artifact;
		this.output = output;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString() {
		return "ADD " + artifact.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.operations.Operation#description()
	 */
	@Override
	public final String description() {
		return "Adding " + artifact.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.operations.Operation#apply()
	 */
	@Override
	public final MergeReport apply() throws NotYetImplementedException,
			IOException {
		MergeReport addReport = new MergeReport(this);

		if (output != null) {
			assert (artifact.exists()) : "Artifact does not exist: " + artifact;
			Artifact.copyArtifact(artifact, output);
		}

		if (Main.isPrintToStdout()) {
			BufferedReader reader = artifact.getReader();
			String line;
			
			while ((line = reader.readLine()) != null) {
				addReport.appendLine(line);
			}
		}

		assert (addReport != null) : "Report must not be null";
		return addReport;
	}
}
