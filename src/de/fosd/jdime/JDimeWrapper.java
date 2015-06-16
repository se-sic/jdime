package de.fosd.jdime;

import de.fosd.jdime.common.ArtifactList;
import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.Revision;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.common.operations.Operation;
import de.fosd.jdime.strategy.NWayStrategy;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class JDimeWrapper {
	private static final Logger LOG = Logger.getLogger(ClassUtils.getShortClassName(JDimeWrapper.class));

	public static void main(String[] args) throws IOException, InterruptedException {
		// setup log4j (otherwise we will drown in debug output)
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);

		// setup JDime using the MergeContext
		MergeContext context = new MergeContext();
		context.setMergeStrategy(new NWayStrategy());
		context.setPretend(true);
		context.setQuiet(false);

		// prepare the list of input files
		ArtifactList<FileArtifact> inputArtifacts = new ArtifactList<>();

		for (Object filename : args) {
			try {
				FileArtifact newArtifact = new FileArtifact(new File((String) filename));

				// set the revision name, this will be used as condition for ifdefs
				// as an example, I'll just use the filenames
				newArtifact.setRevision(new Revision(FilenameUtils.getBaseName(newArtifact.getPath())));

				inputArtifacts.add(newArtifact);
			} catch (FileNotFoundException e) {
				System.err.println("Input file not found: " + (String) filename);
			}
		}

		context.setInputFiles(inputArtifacts);

		// create the merge operation
		Operation<FileArtifact> merge = new MergeOperation<>(context.getInputFiles(), context.getOutputFile(), null, null, context.isConditionalMerge());

		// run the merge
		merge.apply(context);
	}
}
