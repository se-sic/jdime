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

package de.fosd.jdime;

import java.util.HashMap;

import org.gnu.glpk.GLPK;

import de.fosd.jdime.common.Level;
import de.fosd.jdime.stats.ASTStats;
import de.fosd.jdime.stats.StatsElement;

/**
 * 
 * @author Olaf Lessenich
 */
public final class InternalTests {
	
	private static String delimiter = "--------------------------------------";
	
	/**
	 * 
	 */
	private InternalTests() {

	}

	/**
     * 
     */
	public static void run() {
		runEnvironmentTest();
		runASTStatsTests();
	}

	public static void runEnvironmentTest() {
		GLPK.glp_create_prob();
		System.out.println(InternalTests.class.getCanonicalName() + ": OK");
	}

	public static void runASTStatsTests() {
		ASTStats[] stats = new ASTStats[2];

		for (int i = 0; i < stats.length; i++) {
			HashMap<String, StatsElement> diffstats = new HashMap<>();

			StatsElement all = new StatsElement();
			for (Level level : Level.values()) {
				if (level.equals(Level.ALL)) {
					continue;
				}

				StatsElement s = new StatsElement();
				s.setAdded((int) (10 * Math.random()));
				s.setMatches((int) (10 * Math.random()));
				s.setDeleted((int) (10 * Math.random()));
				s.setElements(s.getAdded() + s.getDeleted() + s.getMatches());
				all.addStatsElement(s);
				diffstats.put(level.toString(), s);
			}

			diffstats.put(Level.ALL.toString(), all);

			stats[i] =
					new ASTStats(all.getElements(), (int) (10 * Math.random()),
							(int) (10 * Math.random()), diffstats);
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
