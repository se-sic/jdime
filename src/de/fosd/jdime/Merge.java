/**
 * 
 */
package de.fosd.jdime;

import java.util.Arrays;

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
	 * Time stamp to be set at program start.
	 */
	private static long programStart;
	
	/**
	 * Tool to be used for merging operations.
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
		options.addOption("info", false, "print configuration information");

		CommandLineParser parser = new PosixParser();
		try {
			CommandLine cmd = parser.parse(options, args);

			if (cmd.hasOption("help")) {
				help(options, 0);
			}

			if (cmd.hasOption("version")) {
				version();
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
			
			if (cmd.hasOption("info")) {
				info();
			}
		} catch (ParseException e) {
			LOG.fatal("arguments could not be parsed: " + Arrays.toString(args));
			LOG.fatal("aborting program");
			e.printStackTrace();
			exit(-1);
		}
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
		formatter.printHelp(TOOLNAME, options);
		exit(exitcode);
	}

	/**
	 * Print version information and exit.
	 */
	private static void version() {
		System.out.println(TOOLNAME + " VERSION " + VERSION);
		exit(0);
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
	private static void info() {
		System.out.println("Merge tool: " + mergeTool);
	}

}
