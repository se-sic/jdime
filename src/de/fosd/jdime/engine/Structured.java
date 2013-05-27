/**
 * 
 */
package de.fosd.jdime.engine;

import org.apache.log4j.Logger;

import de.fosd.jdime.Main;
import de.fosd.jdime.common.MergeReport;
import de.fosd.jdime.common.MergeTriple;
import de.fosd.jdime.common.MergeType;

/**
 * Performs a structured merge.
 * 
 * @author lessenic
 * 
 */
public class Structured implements MergeInterface {

	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(Structured.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.engine.MergeInterface#merge()
	 */
	@Override
	public final MergeReport merge(final MergeType mergeType,
			final MergeTriple triple) {
		// TODO Auto-generated method stub
		LOG.setLevel(Main.getLogLevel());
		LOG.debug("Engine started: " + this.getClass().getName());

		MergeReport report = new MergeReport(mergeType, triple);

		throw new UnsupportedOperationException();
		
		//return report;
	}

}
