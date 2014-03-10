/**
 * 
 */
package de.fosd.jdime.stats;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Olaf Lessenich
 * 
 */
public class ASTStats {
	/**
	 * Number of AST nodes.
	 */
	private int nodes = 0;

	/**
	 * Depth of AST.
	 */
	private int treedepth = 0;

	/**
	 * Maximum number of children at a level of the AST.
	 */
	private int maxchildren = 0;

	/**
	 * Map of diff statistics.
	 */
	private HashMap<String, StatsElement> diffstats;

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
			final int maxchildren, final HashMap<String, StatsElement> diffstats) {
		this.nodes = nodes;
		this.treedepth = treedepth;
		this.maxchildren = maxchildren;
		this.diffstats = diffstats;
	}

	public static ASTStats add(final ASTStats a, final ASTStats b) {
		ASTStats sum = null;
		try {
			sum = (ASTStats) a.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
		sum.add(b);
		return sum;
	}

	/**
	 * @return the nodes
	 */
	public final int getNodes() {
		return nodes;
	}

	/**
	 * @param nodes
	 *            the nodes to set
	 */
	public final void setNodes(final int nodes) {
		this.nodes = nodes;
	}

	/**
	 * 
	 */
	public final void incrementNodes() {
		nodes++;
	}

	/**
	 * @return the treedepth
	 */
	public final int getTreedepth() {
		return treedepth;
	}

	/**
	 * @param treedepth
	 *            the treedepth to set
	 */
	public final void setTreedepth(final int treedepth) {
		this.treedepth = treedepth;
	}

	/**
	 * 
	 */
	public final void incrementTreeDepth() {
		treedepth++;
	}

	/**
	 * @return the maxchildren
	 */
	public final int getMaxchildren() {
		return maxchildren;
	}

	/**
	 * @param maxchildren
	 *            the maxchildren to set
	 */
	public final void setMaxchildren(final int maxchildren) {
		this.maxchildren = maxchildren;
	}

	/**
	 * @return the diffstats
	 */
	public final HashMap<String, StatsElement> getDiffstats() {
		return diffstats;
	}

	/**
	 * @param diffstats
	 *            the diffstats to set
	 */
	public final void
			setDiffstats(final HashMap<String, StatsElement> diffstats) {
		this.diffstats = diffstats;
	}

	public final void add(ASTStats other) {
		nodes += other.nodes;

		if (other.treedepth > treedepth) {
			treedepth += other.treedepth;
		}

		if (other.maxchildren > maxchildren) {
			maxchildren += other.maxchildren;
		}

		for (String key : other.diffstats.keySet()) {
			assert (diffstats.containsKey(key)) : "Error: Key '" + key
					+ "' not found!";
			diffstats.get(key).addStatsElement(other.diffstats.get(key));
		}
	}

	public StatsElement getDiffStats(String key) {
		return diffstats.get(key);
	}

	public String toString() {
		DecimalFormat df = new DecimalFormat("#.0");
		String sep = " / ";
		String[] head = { "LEVEL", "NODES", "MATCHES", "CHANGES", "REMOVALS" };
		StringBuilder absolute = new StringBuilder();
		StringBuilder relative = new StringBuilder();

		absolute.append(StringUtils.join(head, sep));
		absolute.append(System.lineSeparator());
		relative.append(StringUtils.join(head, sep));
		relative.append(System.lineSeparator());
		for (String key : new TreeSet<>((diffstats.keySet()))) {
			StatsElement s = diffstats.get(key);
			int nodes = s.getElements();
			int matches = s.getMatches();
			int changes = s.getAdded();
			int removals = s.getDeleted();
			absolute.append(key + sep + nodes + sep + matches + sep + changes
					+ sep + removals + System.lineSeparator());

			if (nodes > 0) {
				relative.append(key + sep + 100.0 + sep
						+ df.format(100.0 * matches / nodes) + sep
						+ df.format(100.0 * changes / nodes) + sep
						+ df.format(100.0 * removals / nodes)
						+ System.lineSeparator());
			}
		}

		return absolute.toString() + System.lineSeparator()
				+ relative.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected Object clone() throws CloneNotSupportedException {
		ASTStats clone = new ASTStats(nodes, treedepth, maxchildren, diffstats);
		clone.setDiffstats((HashMap<String, StatsElement>) diffstats.clone());
		return clone;
	}
}
