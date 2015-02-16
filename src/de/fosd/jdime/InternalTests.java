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
package de.fosd.jdime;

import java.util.HashMap;

import de.fosd.jdime.common.LangElem;
import de.fosd.jdime.stats.ASTStats;
import de.fosd.jdime.stats.StatsElement;
import org.gnu.glpk.GLPK;

/**
 * Contains static methods testing the functionality of various aspects of the program.
 * 
 * @author Olaf Lessenich
 */
public final class InternalTests {

	private static String delimiter = "====================================================";

	/**
	 * Utility class constructor.
	 */
	private InternalTests() {

	}

	/**
     * Runs all internal tests.
     */
	public static void run() {
		runEnvironmentTest();
		runASTStatsTests();
	}

    /**
     * Checks whether the environment for the program is correctly configured. Particularly this verifies that
     * the native libraries are working. 
     */
	public static void runEnvironmentTest() {
		
        try {
            System.out.println("GLPK " + GLPK.glp_version() +  " is working.");
            System.out.println(InternalTests.class.getCanonicalName() + " : OK");
		} catch (Throwable t) {
            System.out.println(t);
            System.out.println(InternalTests.class.getCanonicalName() + " : FAILED");
			
            throw t;
		}
	}

	public static void runASTStatsTests() {
		ASTStats[] stats = new ASTStats[2];

		for (int i = 0; i < stats.length; i++) {
			HashMap<String, StatsElement> diffstats = new HashMap<>();

			StatsElement all = new StatsElement();
			for (LangElem level : LangElem.values()) {
				if (level.equals(LangElem.NODE)) {
					continue;
				}

				StatsElement s = new StatsElement();
				s.setAdded((int) (5 * Math.random()));
				s.setMatches((int) (5 * Math.random()));
				s.setDeleted((int) (5 * Math.random()));
				s.setElements(s.getAdded() + s.getDeleted() + s.getMatches());
				s.setConflicting((int) (s.getElements() * Math.random()));
				s.setChanges(s.getAdded() + s.getDeleted() + s.getConflicting());
				all.addStatsElement(s);
				diffstats.put(level.toString(), s);
			}

			diffstats.put(LangElem.NODE.toString(), all);

			stats[i] = new ASTStats(all.getElements(),
					(int) (5 * Math.random()), (int) (5 * Math.random()),
					diffstats, all.getChanges() != 0);
		}

		ASTStats sum = ASTStats.add(stats[0], stats[1]);

		System.out.println(delimiter);
		System.out.println("Left:");
		System.out.println(stats[0]);

		System.out.println(delimiter);
		System.out.println("Right:");
		System.out.println(stats[1]);

		System.out.println(delimiter);
		System.out.println("Sum:");
		System.out.println(sum);
	}
}
