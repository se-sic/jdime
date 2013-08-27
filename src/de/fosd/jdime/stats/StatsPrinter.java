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
package de.fosd.jdime.stats;

import java.util.ArrayList;
import java.util.Collections;

import org.apache.log4j.Logger;

import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.strategy.LinebasedStrategy;

/**
 * @author lessenic
 * 
 */
public final class StatsPrinter {

	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(StatsPrinter.class);
	
	/**
	 * Delimiter.
	 */
	private static String delimiter = "--------------------------------------"; 

	/**
	 * Private constructor.
	 */
	private StatsPrinter() {

	}

	/**
	 * Prints statistical information.
	 * 
	 * @param context
	 *            merge context
	 */
	public static void print(final MergeContext context) {
		assert (context != null);
		
		Stats stats = context.getStats();
		assert (stats != null);

		if (LOG.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder();
			sb.append("Keys:");

			for (String key : stats.getKeys()) {
				sb.append(" " + key);
			}

			LOG.debug(sb.toString());
		}
		
		System.out.println(delimiter);
		System.out.println("Number of conflicts: " + stats.getConflicts());

		for (String key : stats.getKeys()) {
			System.out.println(delimiter);
			StatsElement element = stats.getElement(key);
			System.out.println("Added " + key + ": " + element.getAdded());
			System.out.println("Deleted " + key + ": " + element.getDeleted());
			System.out.println("Merged " + key + ": " + element.getMerged());
			System.out.println("Conflicting " + key + ": "
					+ element.getConflicting());
		}
		
		System.out.println(delimiter);
		
		// Fuck Java for not letting me sort a Set.
		ArrayList<String> operations = new ArrayList<String>(
				stats.getOperations());
		Collections.sort(operations);
		for (String key : operations) {
			System.out.println("applied " + key + " operations: " 
					+ stats.getOperation(key));
		}

		System.out.println(delimiter);
	
		// sanity checks
		if (context.getMergeStrategy().getClass().getName().equals(
				LinebasedStrategy.class.getName())) {
			assert (stats.getElement("files").getAdded() 
					+ stats.getElement("directories").getAdded() 
					== stats.getOperation("ADD"));
			assert (stats.getElement("files").getDeleted() 
					+ stats.getElement("directories").getDeleted() 
					== stats.getOperation("DELETE"));
			assert (stats.getElement("files").getMerged() 
					+ stats.getElement("directories").getMerged() 
					== stats.getOperation("MERGE"));
			LOG.debug("Sanity checks for linebased merge passed!");
		}
		
	}

}
