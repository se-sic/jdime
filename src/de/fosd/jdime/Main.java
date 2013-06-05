/*******************************************************************************
 * Copyright (c) 2013 Olaf Lessenich.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Olaf Lessenich - initial API and implementation
 ******************************************************************************/
/**
 * 
 */
package de.fosd.jdime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
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

import de.fosd.jdime.common.ArtifactList;
import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeType;
import de.fosd.jdime.common.Revision;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.common.operations.Operation;
import de.fosd.jdime.strategy.MergeStrategy;
import de.fosd.jdime.strategy.StrategyNotFoundException;

/**
 * @author Olaf Lessenich
 * 
 */
public final class Main {

	/**
	 * Private constructor.
	 */
	private Main() {

	}

	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(Main.class);

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
	 * Merge context.
	 */
	private static MergeContext context = new MergeContext();

	/**
	 * Output artifact.
	 */
	private static FileArtifact output = null;

	/**
	 * Perform a merge operation on the input files or directories.
	 * 
	 * @param args
	 *            command line arguments
	 * @throws IOException
	 *             If an input or output exception occurs
	 * @throws InterruptedException
	 *             If a thread is interrupted
	 */
	public static void main(final String[] args) throws IOException,
			InterruptedException {
		BasicConfigurator.configure();
		context = new MergeContext();
		
		programStart = System.currentTimeMillis();

		setLogLevel("INFO");
		LOG.debug("starting program");

		ArtifactList<FileArtifact> inputFiles = parseCommandLineArgs(args);

		assert inputFiles != null : "List of input artifacts may not be null!";
		
		for (FileArtifact inputFile : inputFiles) {
			assert (inputFile != null);
			assert (inputFile instanceof FileArtifact);
			if (inputFile.isDirectory() 
					&& !context.isRecursive()) {
				LOG.fatal("To merge directories, the argument '-r' "
						+ "has to be supplied. "
						+ "See '-help' for more information!");
				exit(-1);
			}
		}
		
		if (output.exists() && !output.isEmpty()) {
			System.err.println("Output directory is not empty!");
			System.err.println("Delete '" + output.getFullPath()
					+ "'? [y/N]");
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(System.in));
			String response = reader.readLine();

			if (response.length() == 0
					|| response.toLowerCase().charAt(0) != 'y') {
				System.err.println("File not overwritten. Exiting.");
				exit(1);
			} else {
				output.remove();
			}

		}
		
		merge(inputFiles, output);

		exit(0);
	}

	/**
	 * Parses command line arguments and initializes program.
	 * 
	 * @param args
	 *            command line arguments
	 * @return List of input files
	 * @throws IOException
	 *             If an input output exception occurs
	 */
	private static ArtifactList<FileArtifact> 
			parseCommandLineArgs(final String[] args) throws IOException {
		LOG.debug("parsing command line arguments: " + Arrays.toString(args));

		Options options = new Options();
		options.addOption("help", false, "print this message");
		options.addOption("version", false,
				"print the version information and exit");
		options.addOption("debug", true, "set debug level");
		options.addOption("mode", true,
				"set merge mode (textual, structured, combined)");
		options.addOption("output", true, "output directory/file");
		options.addOption("f", false, "force overwriting of output files");
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
					if (cmd.getOptionValue("mode").equals("list")) {
						printStrategies(true);
					}
					context.setMergeStrategy(MergeStrategy.parse(cmd
							.getOptionValue("mode")));
				} catch (StrategyNotFoundException e) {
					LOG.fatal(e.getMessage());
					exit(-1);
				}

				if (context.getMergeStrategy() == null) {
					help(options, -1);
				}
			}

			if (cmd.hasOption("output")) {
				output = new FileArtifact(new Revision("merge"), new File(
						cmd.getOptionValue("output")), false);
			}

			context.setForceOverwriting(cmd.hasOption("f"));
			context.setRecursive(cmd.hasOption("r"));
			context.setQuiet(!cmd.hasOption("stdout"));

			if (cmd.hasOption("showconfig")) {
				showConfig(true);
			}

			int numInputFiles = cmd.getArgList().size();

			if (numInputFiles < MergeType.MINFILES
					|| numInputFiles > MergeType.MAXFILES) {
				// number of input files does not fit
				help(options, 0);
			}

			// prepare the list of input files
			ArtifactList<FileArtifact> inputArtifacts 
					= new ArtifactList<FileArtifact>();

			for (Object filename : cmd.getArgList()) {
				try {
					inputArtifacts.add(new FileArtifact(new File(
							(String) filename)));
				} catch (FileNotFoundException e) {
					System.err.println("Input file not found: "
							+ (String) filename);
				}
			}

			return inputArtifacts;
		} catch (ParseException e) {
			LOG.fatal("arguments could not be parsed: " 
					+ Arrays.toString(args));
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
		Logger.getRootLogger().setLevel(Level.toLevel(loglevel));
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
	 * 
	 * @param exit
	 *            whether to exit after printing the config
	 */
	private static void showConfig(final boolean exit) {
		assert (context != null);
		System.out.println("Merge strategy: " + context.getMergeStrategy());
		System.out.println();

		if (exit) {
			exit(0);
		}
	}
	
	/**
	 * Prints the available strategies.
	 * @param exit whether to exit after printing the strategies
	 */
	private static void printStrategies(final boolean exit) {
		System.out.println("Available merge strategies:");
		
		for (String s : MergeStrategy.listStrategies()) {
			System.out.println("\t- " + s);
		}
		
		if (exit) {
			exit(0);
		}
	}

	/**
	 * Merges the input files.
	 * 
	 * @param inputArtifacts
	 *            list of files to merge in order left, base, right
	 * @param output
	 *            output artifact
	 * @throws InterruptedException
	 *             If a thread is interrupted
	 * @throws IOException
	 *             If an input output exception occurs
	 */
	public static void merge(final ArtifactList<FileArtifact> inputArtifacts,
			final FileArtifact output) throws IOException, 
											InterruptedException {
		assert (inputArtifacts != null);
		Operation<FileArtifact> merge = new MergeOperation<FileArtifact>(
											inputArtifacts, output);
		merge.apply(context);
	}

}
