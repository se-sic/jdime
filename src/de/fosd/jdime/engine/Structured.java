/**
 * 
 */
package de.fosd.jdime.engine;

import java.util.List;

import org.apache.log4j.Logger;

import de.fosd.jdime.Merge;
import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeReport;
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
			final List<Artifact> inputArtifacts) {
		// TODO Auto-generated method stub
		LOG.setLevel(Merge.getLogLevel());
		LOG.debug("Engine started: " + this.getClass().getName());

		assert inputArtifacts.size() >= MINFILES : "Too few input files!";
		assert inputArtifacts.size() <= MAXFILES : "Too many input files!";

		MergeReport report = new MergeReport(mergeType, inputArtifacts);

		return report;
	}

}
