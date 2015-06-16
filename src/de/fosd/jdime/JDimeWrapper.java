package de.fosd.jdime;

import de.fosd.jdime.common.ArtifactList;
import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.Revision;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.common.operations.Operation;
import de.fosd.jdime.strategy.MergeStrategy;
import de.fosd.jdime.strategy.NWayStrategy;
import de.fosd.jdime.strategy.StructuredStrategy;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class JDimeWrapper {
	private static final Logger LOG = Logger.getLogger(ClassUtils.getShortClassName(JDimeWrapper.class));

	public static void main(String[] args) throws IOException, InterruptedException {
		// setup log4j (otherwise we will drown in debug output)
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);

		// setup JDime using the MergeContext
		MergeContext context = new MergeContext();
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

		// setup strategies
		MergeStrategy<FileArtifact> structured = new StructuredStrategy();
		MergeStrategy<FileArtifact> conditional = new NWayStrategy();

		// create the merge operation
		Operation<FileArtifact> merge = new MergeOperation<>(context.getInputFiles(), context.getOutputFile(), null, null, context.isConditionalMerge());

		// run the merge first with structured strategy to see whether there are conflicts
		context.setMergeStrategy(structured);
		context.setQuiet(true);
		context.setSaveStats(true);
		merge.apply(context);

		// if there are no conflicts, run the conditional strategy
		if (context.getStats().getConflicts() == 0) {
			context = (MergeContext) context.clone();
			context.setMergeStrategy(conditional);
			context.setQuiet(false);
			context.setSaveStats(false);
			merge.apply(context);
		}
	}
}
