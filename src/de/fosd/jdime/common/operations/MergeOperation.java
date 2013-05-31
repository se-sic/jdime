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

import java.io.IOException;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeReport;
import de.fosd.jdime.common.MergeTriple;
import de.fosd.jdime.common.MergeType;
import de.fosd.jdime.engine.EngineNotFoundException;
import de.fosd.jdime.engine.MergeEngine;

/**
 * The operation merges <code>Artifact</code>s.
 * 
 * @author Olaf Lessenich
 * 
 */
public class MergeOperation extends Operation {
	/**
	 * Logger.
	 */
	//private static final Logger LOG = Logger.getLogger(MergeOperation.class);
	
	/**
	 * Type of merge.
	 */
	private MergeType mergeType;

	/**
	 * The merge triple containing the <code>Artifact</code>s.
	 */
	private MergeTriple mergeTriple;

	/**
	 * The engine used for the merge.
	 */
	private MergeEngine engine;

	/**
	 * The output <code>Artifact</code>.
	 */
	private Artifact output;

	/**
	 * Returns the output <code>Artifact</code>.
	 * 
	 * @return the output artifact
	 */
	public final Artifact getOutput() {
		return output;
	}

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
	 * @param mergeType
	 *            type of merge
	 * @param mergeTriple
	 *            triple containing <code>Artifact</code>s
	 * @param engine
	 *            that is used for the merge
	 * @param output
	 *            output <code>Artifact</code>
	 */
	public MergeOperation(final MergeType mergeType,
			final MergeTriple mergeTriple, final MergeEngine engine,
			final Artifact output) {
		this.mergeType = mergeType;
		this.mergeTriple = mergeTriple;
		this.engine = engine;
		this.output = output;
	}

	/**
	 * Returns the type of merge.
	 * 
	 * @return the type of merge
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString() {
		return "MERGE " + mergeType + " " + mergeTriple.toString(true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.operations.Operation#description()
	 */
	@Override
	public final String description() {
		return "Merging " + mergeTriple.toString(true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.operations.Operation#apply()
	 */
	@Override
	public final MergeReport apply() throws EngineNotFoundException,
			IOException, InterruptedException {
		assert (mergeTriple.getLeft().exists()) 
		: "Left artifact does not exist: " + mergeTriple.getLeft();
		assert (mergeTriple.getRight().exists()) 
		: "Right artifact does not exist: " + mergeTriple.getRight();
		assert (mergeTriple.getBase().isEmptyDummy() 
				|| mergeTriple.getBase().exists()) 
				: "Base artifact does not exist: " + mergeTriple.getBase();
		
		MergeReport mergeReport = engine.merge(this);

		if (output != null) {
			output.write(mergeReport.getReader());
		}
	
		assert (mergeReport != null) : "Report must not be null";
		return mergeReport;
	}
}
