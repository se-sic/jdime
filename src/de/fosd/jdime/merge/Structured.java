/**
 * 
 */
package de.fosd.jdime.merge;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;

import de.fosd.jdime.Merge;
import de.fosd.jdime.MergeReport;
import de.fosd.jdime.MergeType;

/**
 * @author lessenic
 *
 */
public class Structured implements MergeInterface {
	
	private static final Logger LOG = Logger.getLogger(Structured.class);

	/* (non-Javadoc)
	 * @see de.fosd.jdime.merge.MergeInterface#merge()
	 */
	@Override
	public MergeReport merge(MergeType mergeType, List<File> inputFiles) {
		// TODO Auto-generated method stub
		LOG.setLevel(Merge.getLogLevel());
		LOG.debug("Engine started: " + this.getClass().getName());
		
		assert inputFiles.size() >= MINFILES : "Too few input files!";
		assert inputFiles.size() <= MAXFILES : "Too many input files!";
		
		MergeReport report = new MergeReport(mergeType, inputFiles);
		
		return report;
	}

}
