/**
 * 
 */
package de.fosd.jdime;

import java.io.File;
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

/**
 * @author lessenic
 * 
 */
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
	 * 
	 */
	private static final Logger LOG = Logger.getLogger(Merge.class);

	/**
	 * 
	 */
	private static final String TOOLNAME = "jdime";

	/**
	 * 
	 */
	private static final double VERSION = 0.1;

	/**
	 * At least two input files are needed.
	 */
	private static final int MINFILES = 2;

	/**
	 * More than three input files are not supported at the moment.
	 */
	private static final int MAXFILES = 3;

	/**
	 * Time stamp to be set at program start.
	 */
	private static long programStart;

	/**
	 * Tool to be used for merging operations. Linebased is used as default.
	 */
	private static MergeTool mergeTool = MergeTool.LINEBASED;

	/**
	 * Perform a merge operation on the input files or directories.
	 * 
	 * @param args
	 *            command line arguments
	 */
	public static void main(final String[] args) {
		BasicConfigurator.configure();

		programStart = System.currentTimeMillis();

		setLogLevel("INFO");
		LOG.debug("starting program");

		parseCommandLineArgs(args);

		exit(0);
	}

	/**
	 * Parses command line arguments and initializes program.
	 * 
	 * @param args
	 *            command line arguments
	 */
	private static void parseCommandLineArgs(final String[] args) {
		LOG.debug("parsing command line arguments: " + Arrays.toString(args));

		Options options = new Options();
		options.addOption("help", false, "print this message");
		options.addOption("version", false,
				"print the version information and exit");
		options.addOption("debug", true, "set debug level");
		options.addOption("mode", true,
				"set merge mode (textual, structured, combined)");
		options.addOption("showconfig", false,
				"print configuration information");

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
				mergeTool = MergeTool.parse(cmd.getOptionValue("mode"));

				if (mergeTool == null) {
					help(options, -1);
				}
			}

			if (cmd.hasOption("showconfig")) {
				showConfig();
			}

			int numInputFiles = cmd.getArgList().size();

			if (numInputFiles < MINFILES || numInputFiles > MAXFILES) {
				// number of input files does not fit
				help(options, 0);
			} else {
				// try to run the merge
				List<File> inputFiles = new ArrayList<File>();

				for (Object filename : cmd.getArgList()) {
					inputFiles.add(new File((String) filename));
				}

				merge(inputFiles);
			}
		} catch (ParseException e) {
			LOG.fatal("arguments could not be parsed: " + Arrays.toString(args));
			LOG.fatal("aborting program");
			e.printStackTrace();
			exit(-1);
		}
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
		System.out.println("Merge tool: " + mergeTool);
	}

	/**
	 * Merges the input files.
	 * 
	 * @param inputFiles
	 *            list of files to merge in order left, base, right
	 */
	private static void merge(final List<File> inputFiles) {
		assert inputFiles.size() >= MINFILES : "Too few input files!";
		assert inputFiles.size() <= MAXFILES : "Too many input files!";

		// Determine if we have to perform a 2-way or a 3-way merge.
		MergeType mergeType = inputFiles.size() == 2 ? MergeType.TWOWAY
				: MergeType.THREEWAY;

		if (LOG.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder();

			for (File file : inputFiles) {
				sb.append(file.getPath());
				sb.append(" ");
			}

			LOG.debug(mergeType.getClass() + ": " + sb.toString());
		}

		boolean validInput = true;
		int directories = 0;

		for (int pos = 0; pos < mergeType.getNumFiles(); pos++) {
			File file = inputFiles.get(pos);
			if (!file.exists()) {
				validInput = false;
				System.err.println(mergeType.getRevision(pos) + " input file"
						+ " does not exist: " + file.getPath());
			} else if (file.isDirectory()) {
				directories++;
				LOG.debug(mergeType.getRevision(pos) + " is a directory: "
						+ file.getPath());
			} else if (file.isFile()) {
				LOG.debug(mergeType.getRevision(pos) + " is a file: "
						+ file.getPath());
			}
		}

		if (!(directories == 0 || directories == mergeType.getNumFiles())) {
			System.err
					.println("Merging files with directories is not allowed!");
			System.err
					.println("Increase the debug level to get more information!");

			if (validInput) {
				validInput = false;
			}
		}

		if (!validInput) {
			exit(-1);
		}
	}

}
