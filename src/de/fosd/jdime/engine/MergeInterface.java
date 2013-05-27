package de.fosd.jdime.engine;

import java.io.IOException;

import de.fosd.jdime.common.MergeReport;
import de.fosd.jdime.common.MergeTriple;
import de.fosd.jdime.common.MergeType;

/**
 * Interface for the merge engines.
 * @author lessenic
 *
 */
public interface MergeInterface {
	/**
	 * At least two input files are needed.
	 */
	int MINFILES = 2;

	/**
	 * More than three input files are not supported at the moment.
	 */
	int MAXFILES = 3;

	/**
	 * Performs a merge on the given input files.
	 * @param mergeType type of merge
	 * @param triple the merge triple
	 * @return merge report
	 * @throws IOException IOException
	 * @throws InterruptedException InterruptedException
	 */
	MergeReport merge(MergeType mergeType, MergeTriple triple)
			throws IOException, InterruptedException;
}
