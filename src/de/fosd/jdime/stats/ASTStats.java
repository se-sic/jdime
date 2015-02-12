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
/**
 * 
 */
package de.fosd.jdime.stats;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.TreeSet;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import de.fosd.jdime.common.LangElem;

/**
 * @author Olaf Lessenich
 * 
 */
public class ASTStats {
	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(ClassUtils
			.getShortClassName(ASTStats.class));

	public static ASTStats add(final ASTStats a, final ASTStats b) {
		ASTStats sum = null;
		try {
			sum = (ASTStats) a.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		sum.add(b);
		return sum;
	}

	/**
	 * Map of diff statistics.
	 */
	private HashMap<String, StatsElement> diffstats;

	private boolean hasChanges = false;

	/**
	 * Maximum number of children at a level of the AST.
	 */
	private int maxchildren = 0;

	/**
	 * Number of AST nodes.
	 */
	private int nodes = 0;

	/**
	 * Depth of AST.
	 */
	private int treedepth = 0;

	private int fragments = 0;

	/**
	 * Creates a new ASTStats instance.
	 * 
	 * @param nodes
	 *            number of AST nodes
	 * @param treedepth
	 *            depth of AST
	 * @param maxchildren
	 *            maximum number of children at an AST level
	 * @param diffstats
	 *            diff statistics
	 */
	public ASTStats(final int nodes, final int treedepth,
			final int maxchildren,
			final HashMap<String, StatsElement> diffstats,
			final boolean hasChanges) {
		this.nodes = nodes;
		this.treedepth = treedepth;
		this.maxchildren = maxchildren;
		this.diffstats = diffstats;
		this.hasChanges = hasChanges;
	}

	public final void add(ASTStats other) {
		nodes += other.nodes;
		fragments += other.fragments;

		if (other.treedepth > treedepth) {
			treedepth = other.treedepth;
		}

		if (other.maxchildren > maxchildren) {
			maxchildren = other.maxchildren;
		}

		for (String key : other.diffstats.keySet()) {
			assert (diffstats.containsKey(key)) : "Error: Key '" + key
					+ "' not found!";
			// if (diffstats.containsKey(key)) {
			diffstats.get(key).addStatsElement(other.diffstats.get(key));
			// }
		}

		if (!hasChanges) {
			hasChanges = other.hasChanges();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected Object clone() throws CloneNotSupportedException {
		ASTStats clone =
				new ASTStats(nodes, treedepth, maxchildren, diffstats,
						hasChanges);
		clone.setFragments(fragments);
		HashMap<String, StatsElement> diffstatsClone = new HashMap<>();

		for (String key : diffstats.keySet()) {
			diffstatsClone.put(key, diffstats.get(key).copy());
		}

		clone.setDiffstats(diffstatsClone);
		return clone;
	}

	/**
	 * @return the diffstats
	 */
	public final HashMap<String, StatsElement> getDiffstats() {
		return diffstats;
	}

	public StatsElement getDiffStats(String key) {
		return diffstats.get(key);
	}

	/**
	 * @return the maxchildren
	 */
	public final int getMaxchildren() {
		return maxchildren;
	}

	/**
	 * @return the nodes
	 */
	public final int getNodes() {
		return nodes;
	}

	/**
	 * @return the treedepth
	 */
	public final int getTreedepth() {
		return treedepth;
	}

	public boolean hasChanges() {
		return hasChanges;
	}

	public void hasChanges(boolean hasChanges) {
		this.hasChanges = hasChanges;
	}

	/**
	 * 
	 */
	public final void incrementNodes() {
		nodes++;
	}

	/**
	 * 
	 */
	public final void incrementTreeDepth() {
		treedepth++;
	}

	public final void setConflicts(ASTStats other) {
		for (String key : other.diffstats.keySet()) {
			assert (diffstats.containsKey(key)) : "Error: Key '" + key
					+ "' not found!";
			StatsElement mystats = diffstats.get(key);
			StatsElement otherstats = other.diffstats.get(key);
			assert mystats.getConflicting() == 0 : "Unexpected: conflicts > 0";
			mystats.setConflicting(otherstats.getConflicting());
		}
	}
	
	public final void setRemovalsfromAdditions(ASTStats other) {
		for (String key : other.diffstats.keySet()) {
			assert (diffstats.containsKey(key)) : "Error: Key '" + key
					+ "' not found!";
			StatsElement mystats = diffstats.get(key);
			StatsElement otherstats = other.diffstats.get(key);
			mystats.setDeleted(otherstats.getAdded());
		}
	}
	
	public final void setRemovalsfromMatches(ASTStats other) {
		for (String key : other.diffstats.keySet()) {
			assert (diffstats.containsKey(key)) : "Error: Key '" + key
					+ "' not found!";
			StatsElement mystats = diffstats.get(key);
			StatsElement otherstats = other.diffstats.get(key);
			mystats.setDeleted(otherstats.getMatches());
		}
	}
	
	
	public final void setAdditionsfromMatches(ASTStats other) {
		for (String key : other.diffstats.keySet()) {
			assert (diffstats.containsKey(key)) : "Error: Key '" + key
					+ "' not found!";
			StatsElement mystats = diffstats.get(key);
			StatsElement otherstats = other.diffstats.get(key);
			mystats.setAdded(otherstats.getMatches());
		}
	}
	
	public final void resetRemovals() {
		for (String key : diffstats.keySet()) {
			StatsElement mystats = diffstats.get(key);
			mystats.setDeleted(0);
		}
	}
	
	public final void resetAdditions() {
		for (String key : diffstats.keySet()) {
			StatsElement mystats = diffstats.get(key);
			mystats.setAdded(0);
		}
	}

	/**
	 * @param diffstats
	 *            the diffstats to set
	 */
	public final void
			setDiffstats(final HashMap<String, StatsElement> diffstats) {
		this.diffstats = diffstats;
	}

	/**
	 * @param maxchildren
	 *            the maxchildren to set
	 */
	public final void setMaxchildren(final int maxchildren) {
		this.maxchildren = maxchildren;
	}

	/**
	 * @param nodes
	 *            the nodes to set
	 */
	public final void setNodes(final int nodes) {
		this.nodes = nodes;
	}

	/**
	 * @param treedepth
	 *            the treedepth to set
	 */
	public final void setTreedepth(final int treedepth) {
		this.treedepth = treedepth;
	}

	/**
	 * @return the fragments
	 */
	public int getFragments() {
		return fragments;
	}

	/**
	 * @param fragments
	 *            the fragments to set
	 */
	public void setFragments(int fragments) {
		this.fragments = fragments;
	}

	public double getAvgFragmentSize() {
		return fragments == 0 ? 0.0 : (double) diffstats.get(
				LangElem.NODE.toString()).getChanges()
				/ (double) fragments;
	}

	/**
	 *
	 */
	public void incrementFragments() {
		assert (hasChanges());
		this.fragments++;
	}

	@Override
	public String toString() {
		DecimalFormat df = new DecimalFormat("#.0");

		StringBuilder sb = new StringBuilder();

		if (LOG.isDebugEnabled()) {
			sb.append("Total nodes: " + nodes + System.lineSeparator());
			sb.append("Treedepth: " + treedepth + System.lineSeparator());
			sb.append("Maximum children: " + maxchildren
					+ System.lineSeparator());
			sb.append("Fragments: " + fragments + System.lineSeparator());
			sb.append("Avg. Fragment size: " + df.format(getAvgFragmentSize())
					+ System.lineSeparator());
		}

		String[] head =
				{ "LEVEL", "NODES", "MATCHED", "CHANGED", "ADDED", "REMOVED",
						"CONFLICTS" };

		String[][] absolute = new String[6][7];
		String[][] relative = new String[6][7];
		String[] csvHead = new String[36];
		String[] csv = new String[36];;
		int[] sum = new int[5];
		int i = 0;
		int j = 0;

		for (String key : new TreeSet<>((diffstats.keySet()))) {
			StatsElement s = diffstats.get(key);
			int nodes = s.getElements();
			int matched = s.getMatches();
			int changed = s.getChanges();
			int added = s.getAdded();
			int removed = s.getDeleted();
			int conflicts = s.getConflicting();

			// sanity checks
			// assert(changed == added + removed);
			// assert (nodes == matched + changed);
			if (i > 0) {
				sum[0] += nodes;
				sum[1] += matched;
				sum[2] += changed;
				sum[3] += added;
				sum[4] += removed;
			}
			
			String name = key.toLowerCase();
			if (name.endsWith("ss")) {
				name = name + "es";
			} else {
				name = name + "s";
			}

			absolute[i][0] = name;
			absolute[i][1] = String.valueOf(nodes);
			absolute[i][2] = String.valueOf(matched);
			absolute[i][3] = String.valueOf(changed);
			absolute[i][4] = String.valueOf(added);
			absolute[i][5] = String.valueOf(removed);
			absolute[i][6] = String.valueOf(conflicts);
			
			csvHead[j] = name;
			csv[j++] = String.valueOf(nodes);
			csvHead[j] = "matched " + name;
			csv[j++] = String.valueOf(matched);
			csvHead[j] = "changed " + name;
			csv[j++] = String.valueOf(changed);
			csvHead[j] = "added " + name;
			csv[j++] = String.valueOf(added);
			csvHead[j] = "removed " + name;
			csv[j++] = String.valueOf(removed);
			csvHead[j] = "conflicting " + name;
			csv[j++] = String.valueOf(conflicts);
			

			if (nodes > 0) {
				relative[i][0] = key.toLowerCase();
				relative[i][1] = df.format(100.0);
				relative[i][2] = df.format(100.0 * matched / nodes);
				relative[i][3] = df.format(100.0 * changed / nodes);
				relative[i][4] = df.format(100.0 * added / nodes);
				relative[i][5] = df.format(100.0 * removed / nodes);
				relative[i][6] = df.format(100.0 * conflicts / nodes);
			} else {
				relative[i][0] = key.toLowerCase();
				relative[i][1] = "null";
				relative[i][2] = "null";
				relative[i][3] = "null";
				relative[i][4] = "null";
				relative[i][5] = "null";
				relative[i][6] = "null";
			}

			i++;
		}

		// sanity checks
		// assert (sum[0] == Integer.parseInt(absolute[0][1]));
		// assert (sum[1] == Integer.parseInt(absolute[0][2]));
		// assert (sum[2] == Integer.parseInt(absolute[0][3]));
		// assert (sum[3] == Integer.parseInt(absolute[0][4]));
		// assert (sum[4] == Integer.parseInt(absolute[0][5]));

		

		// CSV
		sb.append(StringUtils.join(csvHead, ';'));
		sb.append(System.lineSeparator());
		sb.append(StringUtils.join(csv, ';'));

		// return general.toString() + System.lineSeparator()
		// + absolute.toString() + System.lineSeparator()
		// + relative.toString();

		return sb.toString();
	}
}
