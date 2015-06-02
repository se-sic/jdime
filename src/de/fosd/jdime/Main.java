/*
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2015 University of Passau, Germany
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 *
 * Contributors:
 *     Olaf Lessenich <lessenic@fim.uni-passau.de>
 *     Georg Seibt <seibt@fim.uni-passau.de>
 */
package de.fosd.jdime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import de.fosd.jdime.common.ASTNodeArtifact;
import de.fosd.jdime.common.ArtifactList;
import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeType;
import de.fosd.jdime.common.Revision;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.common.operations.Operation;
import de.fosd.jdime.stats.StatsPrinter;
import de.fosd.jdime.strategy.MergeStrategy;
import de.fosd.jdime.strategy.NWayStrategy;
import de.fosd.jdime.strategy.StrategyNotFoundException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.ClassUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * @author Olaf Lessenich
 *
 */
public final class Main {

	private static final Logger LOG = Logger.getLogger(ClassUtils.getShortClassName(Main.class));
	private static final String TOOLNAME = "jdime";
	private static final String VERSION = "0.3.10-nway";

	/**
	 * Perform a merge operation on the input files or directories.
	 *
	 * @param args command line arguments
	 */
	public static void main(final String[] args) throws IOException, ParseException, InterruptedException {
		BasicConfigurator.configure();
		MergeContext context = new MergeContext();
		setLogLevel("INFO");

		//try {
			if (!parseCommandLineArgs(context, args)) {
				System.exit(0);
			}

			ArtifactList<FileArtifact> inputFiles = context.getInputFiles();
			FileArtifact output = context.getOutputFile();

			assert inputFiles != null : "List of input artifacts may not be null!";

			for (FileArtifact inputFile : inputFiles) {
				assert (inputFile != null);
				if (inputFile.isDirectory() && !context.isRecursive()) {
					String msg = "To merge directories, the argument '-r' "
						+ "has to be supplied. "
						+ "See '-help' for more information!";
					LOG.fatal(msg);
					throw new RuntimeException(msg);
				}
			}

			if (output != null && output.exists() && !output.isEmpty()) {
				System.err.println("Output directory is not empty!");
				System.err.println("Delete '" + output.getFullPath() + "'? [y/N]");
				BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
				String response = reader.readLine();

				if (response.length() == 0 || response.toLowerCase().charAt(0) != 'y') {
					String msg = "File exists and will not be overwritten.";
					LOG.warn(msg);
					throw new RuntimeException(msg);
				} else {
					LOG.warn("File exists and will be overwritten.");
					output.remove();
				}

			}

			if (context.isBugfixing()) {
				bugfixing(context);
			} else if (context.isDumpTree()) {
				dumpTrees(context);
			} else if (context.isDumpFile()) {
				dumpFiles(context);
			} else {
				merge(context);
			}

			if (context.hasStats()) {
				StatsPrinter.print(context);
			}
		/*} catch (Throwable t) {
			LOG.debug("stopping program");
			LOG.debug("runtime: " + (System.currentTimeMillis() - context.getProgramStart())
					+ " ms");
			System.exit(-1);
		}*/

		System.exit(0);
	}

	/**
	 * Parses command line arguments and initializes program.
	 *
	 * @param context
	 *            merge context
	 * @param args
	 *            command line arguments
	 * @return true if program should continue
	 * @throws IOException
	 *             If an input output exception occurs
	 * @throws ParseException
	 *             If arguments cannot be parsed
	 */
	private static boolean parseCommandLineArgs(final MergeContext context,
			final String[] args) throws IOException, ParseException {
		assert (context != null);
		LOG.debug("parsing command line arguments: " + Arrays.toString(args));
		boolean continueRun = true;

		Options options = new Options();
		options.addOption("benchmark", false,
				"benchmark with " + context.getBenchmarkRuns()
						+ " runs per file");
		options.addOption("debug", true, "set debug level");
		options.addOption("consecutive", false,
				"requires diffonly, treats versions"
						+ " as consecutive versions");
		options.addOption("diffonly", false, "diff only, do not merge");
		options.addOption("f", false, "force overwriting of output files");
		options.addOption("help", false, "print this message");
		options.addOption("keepgoing", false, "Keep running after exceptions.");
		options.addOption("mode", true,
				"set merge mode (unstructured, structured, autotuning, dumptree"
						+ ", dumpgraph, dumpfile, prettyprint, nway)");
		options.addOption("output", true, "output directory/file");
		options.addOption("r", false, "merge directories recursively");
		options.addOption("showconfig", false,
				"print configuration information");
		options.addOption("stats", false,
				"collects statistical data of the merge");
		options.addOption("p", false, "(print/pretend) prints the merge result to stdout instead of an output file");
		options.addOption("version", false,
				"print the version information and exit");

		CommandLineParser parser = new PosixParser();
		try {
			CommandLine cmd = parser.parse(options, args);

			if (cmd.hasOption("help")) {
				help(context, options);
				return false;
			}

			if (cmd.hasOption("info")) {
				info(context, options);
				return false;
			}

			if (cmd.hasOption("version")) {
				version(context);
				return false;
			}

			if (cmd.hasOption("debug")) {
				setLogLevel(cmd.getOptionValue("debug"));
			}

			if (cmd.hasOption("mode")) {
				try {
					switch (cmd.getOptionValue("mode").toLowerCase()) {
					case "list":
						printStrategies(context);
						return false;
					case "bugfixing":
						context.setMergeStrategy(MergeStrategy
								.parse("structured"));
						context.setBugfixing();
						break;
					case "test":
						InternalTests.run();
						return false;
					case "testenvironment":
						InternalTests.runEnvironmentTest();
						return false;
					case "dumptree":
						// User only wants to display the ASTs
						context.setMergeStrategy(MergeStrategy
								.parse("structured"));
						context.setDumpTree(true);
						context.setGuiDump(false);
						break;
					case "dumpgraph":
						// User only wants to display the ASTs
						context.setMergeStrategy(MergeStrategy
								.parse("structured"));
						context.setDumpTree(true);
						context.setGuiDump(true);
						break;
					case "dumpfile":
						// User only wants to display the files
						context.setMergeStrategy(MergeStrategy
								.parse("linebased"));
						context.setDumpFiles(true);
						break;
					case "prettyprint":
						// User wants to parse and pretty-print file
						context.setMergeStrategy(MergeStrategy
								.parse("structured"));
						context.setDumpFiles(true);
						break;
					default:
						// User wants to merge
						context.setMergeStrategy(MergeStrategy.parse(cmd
								.getOptionValue("mode")));

						if (context.getMergeStrategy() instanceof NWayStrategy) {
							LOG.trace("n-way merge");
							context.setConditionalMerge(true);
						}
						break;
					}
				} catch (StrategyNotFoundException e) {
					LOG.fatal(e.getMessage());
					throw e;
				}

				if (context.getMergeStrategy() == null) {
					help(context, options);
					return false;
				}
			}

			if (cmd.hasOption("output")) {
				// TODO[low priority]: The default should in a later,
				// rock-stable version be changed to be overwriting file1 so
				// that we are compatible with gnu merge call syntax
				context.setOutputFile(new FileArtifact(new Revision("merge"),
						new File(cmd.getOptionValue("output")), false));
			}

			if (cmd.hasOption("diffonly")) {
				context.setDiffOnly(true);
				if (cmd.hasOption("consecutive")) {
					context.setConsecutive(true);
				}
			}

			context.setSaveStats(cmd.hasOption("stats")
					|| cmd.hasOption("benchmark"));
			context.setBenchmark(cmd.hasOption("benchmark"));
			context.setForceOverwriting(cmd.hasOption("f"));
			context.setRecursive(cmd.hasOption("r"));
			
			if (cmd.hasOption("p")) {
				context.setPretend(true);
				context.setQuiet(false);
			}
			
			context.setKeepGoing(cmd.hasOption("keepgoing"));

			if (cmd.hasOption("showconfig")) {
				showConfig(context);
				return false;
			}

			int numInputFiles = cmd.getArgList().size();

			if (!((context.isDumpTree() || context.isDumpFile() || context
					.isBugfixing()) || numInputFiles >= MergeType.MINFILES)) {
				help(context, options);
				return false;
			}

			// prepare the list of input files
			ArtifactList<FileArtifact> inputArtifacts = new ArtifactList<>();

			char cond = 'A';

			for (Object filename : cmd.getArgList()) {
				try {
					FileArtifact newArtifact = new FileArtifact(new File((String) filename));

					if (context.isConditionalMerge()) {
						newArtifact.setRevision(new Revision(String.valueOf(cond++)));
					}

					inputArtifacts.add(newArtifact);
				} catch (FileNotFoundException e) {
					System.err.println("Input file not found: "
							+ (String) filename);
				}
			}

			context.setInputFiles(inputArtifacts);
		} catch (ParseException e) {
			LOG.fatal("arguments could not be parsed: " + Arrays.toString(args));
			throw e;
		}

		return continueRun;
	}

	/**
	 * Print short information.
	 *
	 * @param context
	 *            merge context
	 * @param options
	 *            Available command line options
	 */
	private static void info(final MergeContext context, final Options options) {
		version(context);
		System.out.println();
		System.out.println("Run the program with the argument '--help' in order to retrieve information on its usage!");
	}

	/**
	 * Print help on usage.
	 *
	 * @param context
	 *            merge context
	 * @param options
	 *            Available command line options
	 */
	private static void help(final MergeContext context, final Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(TOOLNAME, options, true);
	}

	/**
	 * Print version information.
	 *
	 * @param context
	 *            merge context
	 */
	private static void version(final MergeContext context) {
		System.out.println(TOOLNAME + " VERSION " + VERSION);
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
	 * Prints configuration information.
	 *
	 * @param context
	 *            merge context
	 */
	private static void showConfig(final MergeContext context) {
		assert (context != null);
		System.out.println("Merge strategy: " + context.getMergeStrategy());
		System.out.println();
	}

	/**
	 * Prints the available strategies.
	 *
	 * @param context
	 *            merge context
	 */
	private static void printStrategies(final MergeContext context) {
		System.out.println("Available merge strategies:");

		for (String s : MergeStrategy.listStrategies()) {
			System.out.println("\t- " + s);
		}
	}

	/**
	 * Merges the input files.
	 *
	 * @param context
	 *            merge context
	 * @throws InterruptedException
	 *             If a thread is interrupted
	 * @throws IOException
	 *             If an input output exception occurs
	 */
	public static void merge(final MergeContext context) throws IOException,
			InterruptedException {
		assert (context != null);
		Operation<FileArtifact> merge = new MergeOperation<>(context.getInputFiles(), context.getOutputFile(), null, null, context.isConditionalMerge());
		merge.apply(context);
	}

	/**
	 * Mainly used for debugging purposes.
	 *
	 * @param context
	 *            merge context
	 * @throws IOException
	 *             If an input output exception occurs
	 */
	@SuppressWarnings("unchecked")
	public static void dumpTrees(final MergeContext context) throws IOException {
		for (FileArtifact artifact : context.getInputFiles()) {
			MergeStrategy<FileArtifact> strategy =
					(MergeStrategy<FileArtifact>) context.getMergeStrategy();
			System.out.println(strategy.dumpTree(artifact, context.isGuiDump()));
		}
	}

	/**
	 * Mainly used for debugging purposes.
	 *
	 * @param context
	 *            merge context
	 * @throws IOException
	 *             If an input output exception occurs
	 */
	@SuppressWarnings("unchecked")
	public static void dumpFiles(final MergeContext context) throws IOException {
		for (FileArtifact artifact : context.getInputFiles()) {
			MergeStrategy<FileArtifact> strategy =
					(MergeStrategy<FileArtifact>) context.getMergeStrategy();
			System.out.println(strategy.dumpFile(artifact, context.isGuiDump()));
		}
	}

	/**
	 * Only used for debugging purposes.
	 *
	 * @param context
	 *            merge context
	 *
	 */
	private static void bugfixing(final MergeContext context) {
		context.setPretend(true);
		context.setQuiet(false);
		setLogLevel("trace");

		for (FileArtifact artifact : context.getInputFiles()) {
			ASTNodeArtifact ast = new ASTNodeArtifact(artifact);
			// System.out.println(ast.getASTNode().dumpTree());
			// System.out.println(ast.getASTNode());
			// System.out.println(ast.prettyPrint());
			System.out.println(ast.dumpTree());
			System.out.println("--");
			int[] s = ast.getStats();
			System.out.println("Number of nodes: " + s[0]);
			System.out.println("Tree Depth: " + s[1]);
			System.out.println("MaxChildren: " + s[2]);
			System.out.println("--------------------------------------------");
		}
	}

	/**
	 * Private constructor.
	 */
	private Main() {
	}
}
