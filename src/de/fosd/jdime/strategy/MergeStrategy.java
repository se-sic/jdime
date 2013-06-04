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
package de.fosd.jdime.strategy;

import java.io.IOException;

import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.operations.MergeOperation;

/**
 * @author Olaf Lessenich
 * 
 */
public abstract class MergeStrategy implements MergeInterface {
	
	/**
	 * Parses a String and returns a strategy. Null is returned if no
	 * appropriate Tool is found.
	 * 
	 * @param str
	 *            name of the merge tool
	 * @return MergeStrategy merge strategy
	 */
	public static MergeStrategy parse(final String str) {
		assert str != null : "Merge strategy may not be null!";

		String input = str.toLowerCase();

		switch (input) {
		case "linebased":
			return new LinebasedStrategy();
		case "structured":
			return null;
		case "combined":
			return null;
		default:
			throw new StrategyNotFoundException("Strategy not found: " + str);
		}
	}

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
	public abstract void merge(MergeOperation operation, MergeContext context) 
			throws IOException, InterruptedException;
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public abstract String toString();

}
