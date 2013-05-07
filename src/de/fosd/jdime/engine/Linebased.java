/**
 * 
 */
package de.fosd.jdime.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.log4j.Logger;

import de.fosd.jdime.Merge;
import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeReport;
import de.fosd.jdime.common.MergeType;

/**
 * Performs a linebased merge.
 * 
 * @author lessenic
 * 
 */
public class Linebased implements MergeInterface {

	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(Linebased.class);

	/**
	 * Constant prefix of the base merge command.
	 */
	private static final String BASECMD = "merge -q -p";

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.engine.MergeInterface#merge()
	 */
	@Override
	public final MergeReport merge(final MergeType mergeType,
			final List<Artifact> inputArtifacts) throws IOException,
			InterruptedException {
		LOG.setLevel(Merge.getLogLevel());
		LOG.debug("Engine started: " + this.getClass().getName());
		LOG.debug(mergeType.name() + " merge will be performed.");

		assert inputArtifacts.size() >= MINFILES : "Too few input files!";
		assert inputArtifacts.size() <= MAXFILES : "Too many input files!";

		MergeReport report = new MergeReport(mergeType, inputArtifacts);

		if (mergeType == MergeType.TWOWAY) {
			/*
			 * GNU merge does not handle two-way merges very well: 3 input files
			 * are required, so in case of a two-way merge, we set the base
			 * revision to an empty artifact.
			 */
			inputArtifacts.add(1, Artifact.createEmptyArtifact());

		}

		String cmd = BASECMD + " " + Artifact.toString(inputArtifacts);

		// launch the merge process by invoking GNU merge (rcs has to be
		// installed)
		LOG.debug("Running external command: " + cmd);

		long cmdStart = System.currentTimeMillis();

		Runtime run = Runtime.getRuntime();
		Process pr = run.exec(cmd.toString());

		// process input stream
		BufferedReader buf = new BufferedReader(new InputStreamReader(
				pr.getInputStream()));
		String line = "";
		while ((line = buf.readLine()) != null) {
			report.appendLine(line);
		}

		buf.close();

		// process error stream
		buf = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
		while ((line = buf.readLine()) != null) {
			report.appendErrorLine(line);
		}

		buf.close();
		pr.getInputStream().close();
		pr.getErrorStream().close();
		pr.getOutputStream().close();

		pr.waitFor();

		long cmdStop = System.currentTimeMillis();

		LOG.debug("External command has finished after " + (cmdStop - cmdStart)
				+ " ms.");

		if (report.hasErrors()) {
			System.err.println(report.getStdErr());
		}

		return report;
	}

}
