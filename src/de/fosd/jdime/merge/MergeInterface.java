package de.fosd.jdime.merge;

import java.io.File;
import java.io.IOException;
import java.util.List;

import de.fosd.jdime.MergeReport;
import de.fosd.jdime.MergeType;

public interface MergeInterface {
	/**
	 * At least two input files are needed.
	 */
	public static final int MINFILES = 2;

	/**
	 * More than three input files are not supported at the moment.
	 */
	public static final int MAXFILES = 3;

	public MergeReport merge(MergeType mergeType, List<File> inputFiles)
			throws IOException, InterruptedException;
}
