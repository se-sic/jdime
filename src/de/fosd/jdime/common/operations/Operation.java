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

import de.fosd.jdime.common.MergeReport;
import de.fosd.jdime.common.NotYetImplementedException;
import de.fosd.jdime.engine.EngineNotFoundException;

/**
 * This class represents an operation that is applied to <code>Artifact</code>s.
 * 
 * @author Olaf Lessenich
 * 
 */
public abstract class Operation {
	/**
	 * Applies the operation and returns a report.
	 * 
	 * @return report
	 * @throws EngineNotFoundException
	 *             If the engine does not exist
	 * @throws IOException
	 *             If an input or output exception occurs
	 * @throws InterruptedException
	 *             If a thread is interrupted
	 * @throws NotYetImplementedException
	 *             If some functions are accessed that have not been implemented
	 *             yet
	 */
	public abstract MergeReport apply() throws EngineNotFoundException,
			IOException, InterruptedException, NotYetImplementedException;

	/**
	 * Returns a textual description of the operation.
	 * 
	 * @return textual description
	 * @throws NotYetImplementedException
	 *             If some functions are accessed that have not been implemented
	 *             yet
	 */
	public abstract String description() throws NotYetImplementedException;
}
