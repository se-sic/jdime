/**
 * 
 */
package de.fosd.jdime.stats;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.bethecoder.ascii_table.ASCIITable;

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
	public final void setDiffstats(final HashMap<String, StatsElement> diffstats) {
		this.diffstats = diffstats;
	}

	public final void add(ASTStats other) {
		nodes += other.nodes;

		if (other.treedepth > treedepth) {
			treedepth = other.treedepth;
		}

		if (other.maxchildren > maxchildren) {
			maxchildren = other.maxchildren;
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
		StringBuilder sb = new StringBuilder();
		sb.append("Total nodes: " + nodes + System.lineSeparator());
		sb.append("Treedepth: " + treedepth + System.lineSeparator());
		sb.append("Maximum children: " + maxchildren
				+ System.lineSeparator());

		DecimalFormat df = new DecimalFormat("#.0");
		String[] head = { "LEVEL", "NODES", "MATCHES", "CHANGES", "REMOVALS" };

		String[][] absolute = new String[4][5];
		String[][] relative = new String[4][5];
		int[] sum = new int[4]; 
		int i = 0;
		
		for (String key : new TreeSet<>((diffstats.keySet()))) {
			StatsElement s = diffstats.get(key);
			int nodes = s.getElements();
			int matches = s.getMatches();
			int changes = s.getAdded();
			int removals = s.getDeleted();
			
			// sanity checks
			assert (nodes == matches + changes + removals);
			if (i>0) {
				sum[0] += nodes;
				sum[1] += matches;
				sum[2] += changes;
				sum[3] += removals;
			}

			absolute[i][0] = key.toLowerCase();
			absolute[i][1] = String.valueOf(nodes);
			absolute[i][2] = String.valueOf(matches); 
			absolute[i][3] = String.valueOf(changes); 
			absolute[i][4] = String.valueOf(removals); 
			
			if (nodes > 0) {
				relative[i][0] = key.toLowerCase();
				relative[i][1] = df.format(100.0);
				relative[i][2] = df.format(100.0 * matches / nodes);
				relative[i][3] = df.format(100.0 * changes / nodes);
				relative[i][4] = df.format(100.0 * removals / nodes);
			} else {
				relative[i][0] = key.toLowerCase();
				relative[i][1] = "null";
				relative[i][2] = "null";
				relative[i][3] = "null";
				relative[i][4] = "null";
			}
			
			i++;
		}
		
		// sanity checks
		assert (sum[0] == Integer.parseInt(absolute[0][1]));
		assert (sum[1] == Integer.parseInt(absolute[0][2]));
		assert (sum[2] == Integer.parseInt(absolute[0][3]));
		assert (sum[3] == Integer.parseInt(absolute[0][4]));

		
		sb.append(System.lineSeparator());
		sb.append(ASCIITable.getInstance().getTable(head, absolute));
		sb.append(System.lineSeparator());
		sb.append(ASCIITable.getInstance().getTable(head, relative));

//		return general.toString() + System.lineSeparator()
//				+ absolute.toString() + System.lineSeparator()
//				+ relative.toString();
		
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected Object clone() throws CloneNotSupportedException {
		ASTStats clone = new ASTStats(nodes, treedepth, maxchildren, diffstats);
		HashMap<String, StatsElement> diffstatsClone = new HashMap<>();
		
		for (String key : diffstats.keySet()) {
			StatsElement s = diffstats.get(key);
			StatsElement sClone = new StatsElement();
			sClone.addStatsElement(s);
			diffstatsClone.put(key, sClone);
		}
		
		clone.setDiffstats(diffstatsClone);
		return clone;
	}
}
