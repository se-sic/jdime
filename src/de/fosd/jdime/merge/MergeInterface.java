package de.fosd.jdime.merge;

import java.io.File;
import java.io.IOException;
import java.util.List;

import de.fosd.jdime.MergeReport;
import de.fosd.jdime.MergeType;

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
	 * @param inputFiles list of input files
	 * @return merge report
	 * @throws IOException IOException
	 * @throws InterruptedException InterruptedException
	 */
	MergeReport merge(MergeType mergeType, List<File> inputFiles)
			throws IOException, InterruptedException;
}
