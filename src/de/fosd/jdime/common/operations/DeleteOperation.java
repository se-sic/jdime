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

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.DummyReport;
import de.fosd.jdime.common.MergeReport;

/**
 * The operation deletes <code>Artifact</code>s.
 * 
 * @author Olaf Lessenich
 * 
 */
public class DeleteOperation extends Operation {
	/**
	 * The <code>Artifact</code> that is deleted by the operation.
	 */
	private Artifact artifact;

	/**
	 * Class constructor.
	 * 
	 * @param artifact
	 *            that is deleted by the operation
	 */
	public DeleteOperation(final Artifact artifact) {
		this.artifact = artifact;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString() {
		return "DELETE " + artifact.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.operations.Operation#description()
	 */
	@Override
	public final String description() {
		return "Deleting " + artifact.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.operations.Operation#apply()
	 */
	@Override
	public final MergeReport apply() {
		// TODO: create a real report
		System.out.println("IMPLEMENT ME: DeleteOperation.apply()");
		return new DummyReport();
	}
}
