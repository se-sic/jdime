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
package de.fosd.jdime.strategy;

import java.io.IOException;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeType;
import de.fosd.jdime.common.operations.MergeOperation;

/**
 * Interface for the merge strategies.
 * @author Olaf Lessenich
 * 
 * @param <T> type of artifact
 *
 */
public interface MergeInterface<T extends Artifact<T>> {
	/**
	 * At least two input files are needed.
	 */
	int MINFILES = MergeType.MINFILES;

	/**
	 * More than three input files are not supported at the moment.
	 */
	int MAXFILES = MergeType.MAXFILES;

	/**
	 * Performs a merge.
	 * 
	 * @param operation
	 *            merge operation
	 * @param context
	 *            merge context
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	void merge(MergeOperation<T> operation, MergeContext context) 
			throws IOException, InterruptedException;
}
