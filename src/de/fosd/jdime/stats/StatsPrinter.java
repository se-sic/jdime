/*******************************************************************************
 * Copyright (C) 2013 Olaf Lessenich.
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
 *     Olaf Lessenich - initial API and implementation
 ******************************************************************************/
package de.fosd.jdime.stats;

import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.strategy.LinebasedStrategy;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.log4j.Logger;

/**
 * @author Olaf Lessenich
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
				sb.append(" ").append(key);
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
		ArrayList<String> operations = new ArrayList<>(stats.getOperations());
		Collections.sort(operations);
		for (String key : operations) {
			System.out.println("applied " + key + " operations: "
					+ stats.getOperation(key));
		}

		System.out.println(delimiter);

		int[] diffStats = stats.getDiffStats();
		DecimalFormat df = new DecimalFormat("#.0");
		String sep = " / ";

		System.out.print("Change awareness (nodes" + sep + "matches" + sep
				+ "changes" + sep + "removals): ");
		System.out.println(diffStats[0] + sep + diffStats[3] + sep
				+ diffStats[4] + sep + diffStats[5]);

		if (diffStats[0] > 0) {
			System.out.print("Change awareness % (nodes" + sep + "matches"
					+ sep + "changes" + sep + "removals): ");
			System.out.println(100.0 + sep
					+ df.format(100.0 * diffStats[3] / diffStats[0]) + sep
					+ df.format(100.0 * diffStats[4] / diffStats[0]) + sep
					+ df.format(100.0 * diffStats[5] / diffStats[0]));
		}

		System.out.print("Change awareness toplevel (nodes" + sep + "matches"
				+ sep + "changes" + sep + "removals): ");
		System.out.println(diffStats[6] + sep + diffStats[7] + sep
				+ diffStats[8] + sep + diffStats[9]);

		if (diffStats[6] > 0) {
			System.out.print("Change awareness toplevel % (nodes" + sep
					+ "matches" + sep + "changes" + sep + "removals): ");
			System.out.println(100.0 + sep
					+ df.format(100.0 * diffStats[7] / diffStats[6]) + sep
					+ df.format(100.0 * diffStats[8] / diffStats[6]) + sep
					+ df.format(100.0 * diffStats[9] / diffStats[6]));
		}

		System.out.print("Change awareness classlevel (nodes" + sep + "matches"
				+ sep + "changes" + sep + "removals): ");
		System.out.println(diffStats[10] + sep + diffStats[11] + sep
				+ diffStats[12] + sep + diffStats[13]);

		if (diffStats[10] > 0) {
			System.out.print("Change awareness classlevel % (nodes" + sep
					+ "matches" + sep + "changes" + sep + "removals): ");
			System.out.println(100.0 + sep
					+ df.format(100.0 * diffStats[11] / diffStats[10]) + sep
					+ df.format(100.0 * diffStats[12] / diffStats[10]) + sep
					+ df.format(100.0 * diffStats[13] / diffStats[10]));
		}

		System.out.print("Change awareness methodlevel (nodes" + sep
				+ "matches" + sep + "changes" + sep + "removals): ");
		System.out.println(diffStats[14] + sep + diffStats[15] + sep
				+ diffStats[16] + sep + diffStats[17]);

		if (diffStats[14] > 0) {
			System.out.print("Change awareness methodlevel % (nodes" + sep
					+ "matches" + sep + "changes" + sep + "removals): ");
			System.out.println(100.0 + sep
					+ df.format(100.0 * diffStats[15] / diffStats[14]) + sep
					+ df.format(100.0 * diffStats[16] / diffStats[14]) + sep
					+ df.format(100.0 * diffStats[17] / diffStats[14]));
		}

		System.out.println(delimiter);

		System.out.println("Runtime: " + stats.getRuntime() + " ms");

		System.out.println(delimiter);

		// sanity checks
		assert (diffStats[0] == diffStats[3] + diffStats[4] + diffStats[5]) 
			: "Stats error: " + diffStats[0] + " != " + diffStats[3]
				+ " + " + diffStats[4] + " + " + diffStats[5];
		assert (diffStats[6] == diffStats[7] + diffStats[8] + diffStats[9]) : "Stats error: "
				+ diffStats[6]
				+ " != "
				+ diffStats[7]
				+ " + "
				+ diffStats[8]
				+ " + " + diffStats[9];
		assert (diffStats[10] == diffStats[11] + diffStats[12] + diffStats[13]) : "Stats error: "
				+ diffStats[10]
				+ " != "
				+ diffStats[11]
				+ " + "
				+ diffStats[12] + " + " + diffStats[13];
		assert (diffStats[14] == diffStats[15] + diffStats[16] + diffStats[17]) : "Stats error: "
				+ diffStats[14]
				+ " != "
				+ diffStats[15]
				+ " + "
				+ diffStats[16] + " + " + diffStats[17];
		assert (diffStats[0] == diffStats[6] + diffStats[10] + diffStats[14]) : "Stats error: "
				+ diffStats[0]
				+ " != "
				+ diffStats[6]
				+ " + "
				+ diffStats[10]
				+ " + " + diffStats[14];
		if (context.getMergeStrategy().getClass().getName()
				.equals(LinebasedStrategy.class.getName())) {
			assert (stats.getElement("files").getAdded()
					+ stats.getElement("directories").getAdded() == stats
						.getOperation("ADD"));
			assert (stats.getElement("files").getDeleted()
					+ stats.getElement("directories").getDeleted() == stats
						.getOperation("DELETE"));
			assert (stats.getElement("files").getMerged()
					+ stats.getElement("directories").getMerged() == stats
						.getOperation("MERGE"));
			LOG.debug("Sanity checks for linebased merge passed!");
		}

	}

	/**
	 * Private constructor.
	 */
	private StatsPrinter() {
	}
}
