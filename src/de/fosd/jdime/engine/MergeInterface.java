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
package de.fosd.jdime.engine;

import java.io.IOException;

import de.fosd.jdime.common.MergeOperation;
import de.fosd.jdime.common.MergeReport;

/**
 * Interface for the merge engines.
 * @author Olaf Lessenich
 *
 */
public interface MergeInterface {
	/**
	 * At least two input files are needed.
	 */
	int MINFILES = 2;

	/**
	 * More than three input files are not supported at the moment.
	 */
	int MAXFILES = 3;

	/**
	 * Performs a merge on the given input files.
	 * @param mergeType type of merge
	 * @param triple the merge triple
	 * @return merge report
	 * @throws IOException IOException
	 * @throws InterruptedException InterruptedException
	 */
	MergeReport merge(MergeOperation operation)
			throws IOException, InterruptedException;
}
