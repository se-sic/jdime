/**
 * 
 */
package de.fosd.jdime.stats;

/**
 * @author Olaf Lessenich
 *
 */
public class LinebasedStats extends Stats {
	/**
	 * Directory stats.
	 */
	private StatsElement dirs = new StatsElement();
	
	/**
	 * File stats.
	 */
	private StatsElement files = new StatsElement();
	
	/**
	 * Line stats.
	 */
	private StatsElement lines = new StatsElement();

	/* (non-Javadoc)
	 * @see de.fosd.jdime.stats.Stats#add(de.fosd.jdime.stats.Stats)
	 */
	@Override
	public final void add(final Stats other) {
		assert (other instanceof LinebasedStats);
		LinebasedStats otherStats = (LinebasedStats) other;
		dirs.addStatsElement(otherStats.dirs);
		files.addStatsElement(otherStats.files);
		lines.addStatsElement(otherStats.lines);
	}

}
