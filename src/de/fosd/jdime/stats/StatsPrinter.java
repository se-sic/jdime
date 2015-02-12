/*******************************************************************************
 * Copyright (C) 2013, 2014 Olaf Lessenich.
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
package de.fosd.jdime.stats;

import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.lang3.ClassUtils;
import org.apache.log4j.Logger;

import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.strategy.LinebasedStrategy;

/**
 * @author Olaf Lessenich
 * 
 */
public final class StatsPrinter {

	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(ClassUtils
			.getShortClassName(StatsPrinter.class));
	/**
	 * Delimiter.
	 */
	private static String delimiter =
			"=================================================";

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

			System.out.println(delimiter);
			System.out.println("Number of conflicts: " + stats.getConflicts());

			for (String key : stats.getKeys()) {
				System.out.println(delimiter);
				StatsElement element = stats.getElement(key);
				System.out.println("Added " + key + ": " + element.getAdded());
				System.out.println("Deleted " + key + ": "
						+ element.getDeleted());
				System.out
						.println("Merged " + key + ": " + element.getMerged());
				System.out.println("Conflicting " + key + ": "
						+ element.getConflicting());
			}

			System.out.println(delimiter);

			ArrayList<String> operations =
					new ArrayList<>(stats.getOperations());
			Collections.sort(operations);
			for (String key : operations) {
				System.out.println("applied " + key + " operations: "
						+ stats.getOperation(key));
			}

			System.out.println(delimiter);
		}

		if (context.isConsecutive()) {
			ASTStats rightStats = stats.getRightStats();
			ASTStats leftStats = stats.getLeftStats();

			if (rightStats == null) {
				return;
			}

			if (leftStats != null) {
				rightStats.setRemovalsfromAdditions(leftStats);
			}

			System.out.println(rightStats);
		} else {
			ASTStats astStats = stats.getAstStats();
			System.out.println(astStats);
		}

		if (LOG.isDebugEnabled()) {
			System.out.println(delimiter);
			System.out.println("Runtime: " + stats.getRuntime() + " ms");
			System.out.println(delimiter);
		}

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
