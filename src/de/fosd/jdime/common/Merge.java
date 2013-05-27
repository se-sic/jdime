/**
 * 
 */
package de.fosd.jdime.common;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import de.fosd.jdime.Main;
import de.fosd.jdime.engine.EngineNotFoundException;
import de.fosd.jdime.engine.MergeEngine;

/**
 * @author lessenic
 * 
 */
public final class Merge {

	/**
	 * 
	 */
	private Merge() {
	}

	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(Merge.class);

	/**
	 * Performs a merge on files or directories.
	 * 
	 * @param mergeType
	 *            type of merge
	 * @param engine
	 *            merge engine
	 * @param inputArtifacts
	 *            input files
	 * @return list of merge reports
	 * @throws EngineNotFoundException
	 *             if merge engine cannot be found
	 * @throws IOException
	 *             IOException
	 * @throws InterruptedException
	 *             InterruptedException
	 */
	public static List<MergeReport> merge(final MergeType mergeType,
			final MergeEngine engine, final List<Artifact> inputArtifacts)
			throws EngineNotFoundException, IOException, InterruptedException {
		LOG.setLevel(Main.getLogLevel());
		LOG.debug(Merge.class.getName());
		LOG.debug(mergeType.name() + " merge will be performed.");

		List<MergeReport> reports = new LinkedList<MergeReport>();
		OperationList operations = calculateOperations(mergeType,
				inputArtifacts, 0, "");

		for (Operation operation : operations) {
			if (LOG.isDebugEnabled()) {
				LOG.debug(operation);
			}
			if (operation instanceof MergeOperation) {
				// FIXME: maybe this can be done nicer with a visitor pattern
				reports.add(engine.merge(mergeType,
						((MergeOperation) operation).getMergeTriple()));
			} else {
				// TODO
				throw new UnsupportedOperationException();
			}
		}

		return reports;
	}

	/**
	 * Extracts the operations that are needed for the merge.
	 * 
	 * @param mergeType
	 *            type of merge
	 * @param inputArtifacts
	 *            input files
	 * @param depth
	 *            recursion depth
	 * @param commonPath
	 *            common path for the merged file
	 * @return list of operations
	 * @throws FileNotFoundException
	 *             FileNotFoundException
	 */
	private static OperationList calculateOperations(final MergeType mergeType,
			final List<Artifact> inputArtifacts, final int depth,
			final String commonPath) throws FileNotFoundException {
		OperationList operations = new OperationList();
		boolean isDirectory = inputArtifacts.get(0).isDirectory();

		if (!isDirectory) {
			// easiest case: files only - just add a merge operation for them!
			MergeTriple triple = null;

			if (mergeType == MergeType.TWOWAY) {
				triple = new MergeTriple(inputArtifacts.get(0),
						Artifact.createEmptyArtifact(), inputArtifacts.get(1));
			} else if (mergeType == MergeType.THREEWAY) {
				triple = new MergeTriple(inputArtifacts.get(0),
						inputArtifacts.get(1), inputArtifacts.get(2));
			}

			assert (triple != null);
			operations.add(new MergeOperation(mergeType, triple));
		} else {
			// we are merging directories. we need to apply the standard
			// three-way merge rules to the content of the directories.
			// TODO
			throw new UnsupportedOperationException();
		}

		return operations;
	}
}
