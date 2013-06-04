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

import de.fosd.jdime.common.MergeContext;

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
	 * @param context merge context
	 * @throws IOException
	 *             If an input or output exception occurs
	 * @throws InterruptedException
	 *             If a thread is interrupted
	 */
	public abstract void apply(final MergeContext context) throws IOException,
			InterruptedException;

	/**
	 * Returns the name of the operation.
	 * 
	 * @return name of the operation
	 */
	public abstract String getName();
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public abstract String toString();
}
