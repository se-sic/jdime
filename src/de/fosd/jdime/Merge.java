/**
 * 
 */
package de.fosd.jdime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeReport;
import de.fosd.jdime.common.MergeType;
import de.fosd.jdime.engine.EngineNotFoundException;
import de.fosd.jdime.engine.MergeEngine;

/**
 * @author lessenic
 * 
 */
public final class Merge {

	/**
	 * Private constructor.
	 */
	private Merge() {

	}

	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(Merge.class);

	/**
	 * Toolname constant.
	 */
	private static final String TOOLNAME = "jdime";

	/**
	 * Version constant.
	 */
	private static final double VERSION = 0.1;

	/**
	 * Time stamp to be set at program start.
	 */
	private static long programStart;

	/**
	 * Tool to be used for merging operations. Linebased is used as default.
	 */
	private static MergeEngine mergeEngine = MergeEngine.LINEBASED;

	/**
	 * If set to true, the results of a merge are printed to STDOUT This can be
	 * overridden by the command line argument '-stdout'.
	 */
	private static boolean printToStdout = false;

	/**
	 * Merge directories recursively. Can be set with the '-r' argument.
	 */
	private static boolean recursive = false;

	/**
	 * Perform a merge operation on the input files or directories.
	 * 
	 * @param args
	 *            command line arguments
	 * @throws IOException
	 *             IOException
	 * @throws InterruptedException
	 *             InterruptedException
	 */
	public static void main(final String[] args) throws IOException,
			InterruptedException {
		BasicConfigurator.configure();

		programStart = System.currentTimeMillis();

		setLogLevel("INFO");
		LOG.debug("starting program");

		List<Artifact> inputFiles = parseCommandLineArgs(args);

		assert inputFiles != null : "List of input artifacts may not be null!";
		MergeReport report = merge(inputFiles);

		assert report != null;

		exit(0);
	}

	/**
	 * Parses command line arguments and initializes program.
	 * 
	 * @param args
	 *            command line arguments
	 * @return List of input files
	 */
	private static List<Artifact> parseCommandLineArgs(final String[] args) {
		LOG.debug("parsing command line arguments: " + Arrays.toString(args));

		Options options = new Options();
		options.addOption("help", false, "print this message");
		options.addOption("version", false,
				"print the version information and exit");
		options.addOption("debug", true, "set debug level");
		options.addOption("mode", true,
				"set merge mode (textual, structured, combined)");
		options.addOption("r", false, "merge directories recursively");
		options.addOption("showconfig", false,
				"print configuration information");
		options.addOption("stdout", false, "prints merge result to stdout");

		CommandLineParser parser = new PosixParser();
		try {
			CommandLine cmd = parser.parse(options, args);

			if (cmd.hasOption("help")) {
				help(options, 0);
			}

			if (cmd.hasOption("info")) {
				info(options, 0);
			}

			if (cmd.hasOption("version")) {
				version(true);
			}

			if (cmd.hasOption("debug")) {
				setLogLevel(cmd.getOptionValue("debug"));
			}

			if (cmd.hasOption("mode")) {
				try {
					mergeEngine = MergeEngine.parse(cmd.getOptionValue("mode"));
				} catch (EngineNotFoundException e) {
					LOG.fatal(e.getMessage());
					exit(-1);
				}

				if (mergeEngine == null) {
					help(options, -1);
				}
			}

			if (cmd.hasOption("r")) {
				recursive = true;
			}

			if (cmd.hasOption("showconfig")) {
				showConfig();
			}

			if (cmd.hasOption("stdout")) {
				printToStdout = true;
			}

			int numInputFiles = cmd.getArgList().size();

			if (numInputFiles < MergeType.MINFILES
					|| numInputFiles > MergeType.MAXFILES) {
				// number of input files does not fit
				help(options, 0);
			}

			// prepare the list of input files
			List<Artifact> inputArtifacts = new ArrayList<Artifact>();

			for (Object filename : cmd.getArgList()) {
				try {
					inputArtifacts
							.add(new Artifact(new File((String) filename)));
				} catch (FileNotFoundException e) {
					System.err.println("Input file not found: "
							+ (String) filename);
				}
			}

			return inputArtifacts;
		} catch (ParseException e) {
			LOG.fatal("arguments could not be parsed: " + Arrays.toString(args));
			LOG.fatal("aborting program");
			e.printStackTrace();
			exit(-1);
		}
		return null;
	}

	/**
	 * Print short information and exit.
	 * 
	 * @param options
	 *            Available command line options
	 * @param exitcode
	 *            the code to return on termination
	 */
	private static void info(final Options options, final int exitcode) {
		version(false);
		System.out.println();
		System.out.println("Run the program with the argument '--help' in "
				+ "order to retrieve information on its usage!");
		exit(exitcode);
	}

	/**
	 * Print help on usage and exit.
	 * 
	 * @param options
	 *            Available command line options
	 * @param exitcode
	 *            the code to return on termination
	 */
	private static void help(final Options options, final int exitcode) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(TOOLNAME, options, true);
		exit(exitcode);
	}

	/**
	 * Print version information and exit.
	 * 
	 * @param exit
	 *            program exists if true
	 */
	private static void version(final boolean exit) {
		System.out.println(TOOLNAME + " VERSION " + VERSION);

		if (exit) {
			exit(0);
		}
	}

	/**
	 * Set the logging level. Default is DEBUG.
	 * 
	 * @param loglevel
	 *            May be OFF, FATAL, ERROR, WARN, INFO, DEBUG or ALL
	 */
	private static void setLogLevel(final String loglevel) {
		LOG.setLevel(Level.toLevel(loglevel));
	}

	/**
	 * Exit program with provided return code.
	 * 
	 * @param exitcode
	 *            the code to return on termination
	 */
	private static void exit(final int exitcode) {
		long programStop = System.currentTimeMillis();
		LOG.debug("stopping program");
		LOG.debug("runtime: " + (programStop - programStart) + " ms");
		LOG.debug("exit code: " + exitcode);
		System.exit(exitcode);
	}

	/**
	 * Prints configuration information.
	 */
	private static void showConfig() {
		System.out.println("Merge tool: " + mergeEngine);
	}

	/**
	 * Merges the input files.
	 * 
	 * @param inputArtifacts
	 *            list of files to merge in order left, base, right
	 * @return MergeReport
	 * @throws IOException
	 *             IOException
	 * @throws InterruptedException
	 *             InterruptedException
	 */
	private static MergeReport merge(final List<Artifact> inputArtifacts)
			throws IOException, InterruptedException {
		assert inputArtifacts.size() >= MergeType.MINFILES : "Too few input files!";
		assert inputArtifacts.size() <= MergeType.MAXFILES : "Too many input files!";

		// Determine whether we have to perform a 2-way or a 3-way merge.
		MergeType mergeType = inputArtifacts.size() == 2 ? MergeType.TWOWAY
				: MergeType.THREEWAY;

		if (LOG.isDebugEnabled()) {
			LOG.debug(mergeType.getClass() + ": "
					+ Artifact.toString(inputArtifacts));
		}

		boolean validInput = true;
		int directories = 0;

		for (int pos = 0; pos < mergeType.getNumFiles(); pos++) {
			Artifact artifact = inputArtifacts.get(pos);
			if (!artifact.exists()) {
				validInput = false;
				System.err.println(mergeType.getRevision(pos) + " input file"
						+ " does not exist: " + artifact);
			} else if (artifact.isDirectory()) {
				directories++;
				LOG.debug(mergeType.getRevision(pos) + " is a directory: "
						+ artifact);
			} else if (artifact.isFile()) {
				LOG.debug(mergeType.getRevision(pos) + " is a file: "
						+ artifact);
			}
		}

		if (!(directories == 0 || directories == mergeType.getNumFiles())) {
			System.err
					.println("Merging files with directories is not allowed!");
			System.err.println("Increase the debug level to "
					+ "get more information!");

			validInput = false;
		} else if (directories > 0 && !recursive) {
			System.err.println("In order to merge directories, "
					+ "the -r argument has to be specified. "
					+ "See -help for more information!'");
			validInput = false;
		}

		if (!validInput) {
			exit(-1);
		} else {
			try {
				MergeReport report = mergeEngine.merge(mergeType,
						inputArtifacts);

				if (printToStdout) {
					printReport(report);
				}

				return report;
			} catch (EngineNotFoundException e) {
				LOG.fatal(e.getMessage());
				exit(-1);
			}
		}

		// should not happen
		return null;
	}

	/**
	 * Returns the logging level of this class' logger.
	 * 
	 * @return logging level
	 */
	public static Level getLogLevel() {
		return LOG.getLevel();
	}

	/**
	 * Prints the output of a merge.
	 * 
	 * @param report
	 *            MergeReport
	 */
	private static void printReport(final MergeReport report) {
		LOG.debug("Output of " + report.getMergeType().name() + " merge of "
				+ Artifact.toString(report.getInputArtifacts()));
		System.out.println(report.getStdIn());
	}

}
