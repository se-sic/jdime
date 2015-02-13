/*******************************************************************************
 * Copyright (C) 2013-2015 Olaf Lessenich.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 *
 * Contributors:
 *     Olaf Lessenich <lessenic@fim.uni-passau.de>
 *******************************************************************************/
package de.fosd.jdime.merge;

import java.io.IOException;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeType;
import de.fosd.jdime.common.operations.MergeOperation;

/**
 * Interface for merge algorithms.
 *
 * @author Olaf Lessenich
 *
 * @param <T>
 *            type of artifact
 *
 */
public interface MergeInterface<T extends Artifact<T>> {

	int MINFILES = MergeType.MINFILES;
	int MAXFILES = MergeType.MAXFILES;

	/**
	 * Executes a merge based on a <code>MergeOperation</code>.
	 * <p>
	 * The source and target <code>Artifacts</code> are extracted from the
	 * <code>MergeOperation</code>.
	 * It is determined what kind of merge (e.g., two-way or three-way) has to be done.
	 * The source <code>Artifacts</code> are compared to each other using
	 * implementations of <code>MatchingInterface</code>.
	 * Finally, a unified <code>Artifact</code> is created, the target <code>Artifact</code>.
	 * Therefore, it should be considered by the merge implementation whether
	 * the order of elements is significant or not.
	 *
	 * @param operation
	 *            merge operation
	 * @param context
	 *            merge context
	 * @throws IOException
	 *             IOException
	 * @throws InterruptedException
	 *             InterruptedException
	 */
	void merge(MergeOperation<T> operation, MergeContext context)
			throws IOException, InterruptedException;
}
