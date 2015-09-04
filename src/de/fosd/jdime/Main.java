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

import de.fosd.jdime.common.*;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.common.operations.Operation;
import de.fosd.jdime.stats.StatsPrinter;
import de.fosd.jdime.strategy.MergeStrategy;
import de.fosd.jdime.strategy.StrategyNotFoundException;
import de.uni_passau.fim.seibt.kvconfig.Config;
import de.uni_passau.fim.seibt.kvconfig.PropFileConfigSource;
import de.uni_passau.fim.seibt.kvconfig.SysEnvConfigSource;
import org.apache.commons.cli.*;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * @author Olaf Lessenich
 *
 */
public final class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getCanonicalName());

    private static final String TOOLNAME = "jdime";
    private static final String VERSION = "0.3.11-develop";

    private static final String LOGGING_CONFIG_FILE_NAME = "JDimeLogging.properties";
    private static final String CONFIG_FILE_NAME = "JDime.properties";
    private static final File LOGGING_CONFIG_FILE = new File(LOGGING_CONFIG_FILE_NAME);
    private static final File CONFIG_FILE = new File(CONFIG_FILE_NAME);
    public static final Config config;

    static {
        if (LOGGING_CONFIG_FILE.exists()) {

            try (InputStream is = new FileInputStream(LOGGING_CONFIG_FILE)) {
                LogManager.getLogManager().readConfiguration(is);
            } catch (IOException e) {
                LOG.log(Level.WARNING, e, () -> "Could not read logging configuration.");
            }
        }

        config = new Config();
        config.addSource(new SysEnvConfigSource(1));

        if (CONFIG_FILE.exists()) {

            try {
                config.addSource(new PropFileConfigSource(2, CONFIG_FILE));
            } catch (IOException e) {
                LOG.log(Level.WARNING, e, () -> "Could not add a ConfigSource for " + CONFIG_FILE.getAbsolutePath());
            }
        }
    }

    /**
     * Perform a merge operation on the input files or directories.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) throws IOException, ParseException, InterruptedException {
        MergeContext context = new MergeContext();

        if (!parseCommandLineArgs(context, args)) {
            System.exit(0);
        }

        ArtifactList<FileArtifact> inputFiles = context.getInputFiles();
        FileArtifact output = context.getOutputFile();

        assert inputFiles != null : "List of input artifacts may not be null!";

        for (FileArtifact inputFile : inputFiles) {
            assert (inputFile != null);
            if (inputFile.isDirectory() && !context.isRecursive()) {
                String msg = "To merge directories, the argument '-r' has to be supplied. See '-help' for more information!";
                LOG.severe(msg);
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
                LOG.warning(msg);
                throw new RuntimeException(msg);
            } else {
                LOG.warning("File exists and will be overwritten.");
                boolean isDirectory = output.isDirectory();
                output.remove();

                if (isDirectory) {
                    output.getFile().mkdir();
                }
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

        if (context.hasStatistics()) {
            StatsPrinter.print(context);
        }

        if (LOG.isLoggable(Level.CONFIG)) {
            HashMap<MergeScenario, Throwable> crashes = context.getCrashes();
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%d crashes occurred while merging:\n", crashes.size()));
            for (MergeScenario scenario : crashes.keySet()) {
                Throwable t = crashes.get(scenario);
                sb.append("* " + t.toString() + "\n");
                sb.append("    " + scenario.toString().replace(" ", "\n    ") + "\n");
            }
            LOG.config(sb.toString());
        }

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
        LOG.fine(() -> "Parsing command line arguments: " + Arrays.toString(args));
        boolean continueRun = true;

        Options options = new Options();
        options.addOption("benchmark", false,
                "benchmark with " + context.getBenchmarkRuns()
                        + " runs per file");
        options.addOption("debug", true, "set debug level"
                + " (OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL)");
        options.addOption("consecutive", false,
                "requires diffonly, treats versions"
                        + " as consecutive versions");
        options.addOption("diffonly", false, "diff only, do not merge");
        options.addOption("f", false, "force overwriting of output files");
        options.addOption("help", false, "print this message");
        options.addOption("keepgoing", false, "Keep running after exceptions.");
        options.addOption("lookahead", true,
                "Use heuristics for matching. Supply off, full, or a number as argument.");
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
        options.addOption("q", false, "quiet, do not print the merge result to stdout");
        options.addOption("version", false,
                "print the version information and exit");

        CommandLineParser parser = new PosixParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                help(options);
                return false;
            }

            if (cmd.hasOption("info")) {
                info(context);
                return false;
            }

            if (cmd.hasOption("version")) {
                version();
                return false;
            }

            if (cmd.hasOption("debug")) {
                setLogLevel(cmd.getOptionValue("debug"));
            }

            if (cmd.hasOption("mode")) {
                try {
                    switch (cmd.getOptionValue("mode").toLowerCase()) {
                    case "list":
                        printStrategies();
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
                        break;
                    }
                } catch (StrategyNotFoundException e) {
                    LOG.severe(e.getMessage());
                    throw e;
                }

                if (context.getMergeStrategy() == null) {
                    help(options);
                    return false;
                }
            }

            String outputFileName = null;
            if (cmd.hasOption("output")) {
                // TODO[low priority]: The default should in a later,
                // rock-stable version be changed to be overwriting file1 so
                // that we are compatible with gnu merge call syntax
                outputFileName = cmd.getOptionValue("output");
            }

            if (cmd.hasOption("diffonly")) {
                context.setDiffOnly(true);
                if (cmd.hasOption("consecutive")) {
                    context.setConsecutive(true);
                }
            }

            if (cmd.hasOption("lookahead")) {
                String lookAheadValue = cmd.getOptionValue("lookahead");

                // initialize with the context's default.
                int lookAhead = context.getLookAhead();

                // parse the value provided by the user
                try {
                    lookAhead = Integer.parseInt(lookAheadValue);
                } catch (NumberFormatException e) {
                    switch(lookAheadValue) {
                        case "off":
                            break;
                        case "full":
                            lookAhead = MergeContext.LOOKAHEAD_FULL;
                            break;
                    }
                }

                context.setLookAhead(lookAhead);
                LOG.finest(() -> "Lookahead = " + context.getLookAhead());
            }

            context.collectStatistics(cmd.hasOption("stats")
                    || cmd.hasOption("benchmark"));
            context.setBenchmark(cmd.hasOption("benchmark"));
            context.setForceOverwriting(cmd.hasOption("f"));
            context.setRecursive(cmd.hasOption("r"));
            
            if (cmd.hasOption("p")) {
                context.setPretend(true);
                context.setQuiet(false);
            } else if (cmd.hasOption('q')) {
                context.setQuiet(true);
            }
            
            context.setKeepGoing(cmd.hasOption("keepgoing"));

            if (cmd.hasOption("showconfig")) {
                showConfig(context);
                return false;
            }

            int numInputFiles = cmd.getArgList().size();

            if (!((context.isDumpTree() || context.isDumpFile() || context
                    .isBugfixing()) || numInputFiles >= MergeType.MINFILES)) {
                help(options);
                return false;
            }

            // prepare the list of input files
            ArtifactList<FileArtifact> inputArtifacts = new ArtifactList<>();

            char cond = 'A';
            boolean targetIsFile = true;

            for (Object filename : cmd.getArgList()) {
                try {
                    FileArtifact newArtifact = new FileArtifact(new File((String) filename));

                    if (context.isConditionalMerge()) {
                        newArtifact.setRevision(new Revision(String.valueOf(cond++)));
                    }

                    if (targetIsFile) {
                        targetIsFile = !newArtifact.isDirectory();
                    }

                    inputArtifacts.add(newArtifact);
                } catch (FileNotFoundException e) {
                    System.err.println("Input file not found: " + (String) filename);
                }
            }

            context.setInputFiles(inputArtifacts);

            if (outputFileName != null) {
                context.setOutputFile(new FileArtifact(new Revision("merge"), new File(outputFileName),
                        true, targetIsFile));
                context.setPretend(false);
            }
        } catch (ParseException e) {
            LOG.severe(() -> "Arguments could not be parsed: " + e.getMessage());
            throw e;
        }

        return continueRun;
    }

    /**
     * Print short information.
     *  @param context
     *            merge context
     *
     */
    private static void info(final MergeContext context) {
        version();
        System.out.println();
        System.out.println("Run the program with the argument '--help' in order to retrieve information on its usage!");
    }

    /**
     * Print help on usage.
     *
     * @param options
     *            Available command line options
     */
    private static void help(final Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(TOOLNAME, options, true);
    }

    /**
     * Print version information.
     *
     */
    private static void version() {
        System.out.println(TOOLNAME + " VERSION " + VERSION);
    }

    /**
     * Set the logging level. The levels in descending order are:<br>
     *
     * <ul>
     *  <li>ALL</li>
     *  <li>SEVERE (highest value)</li>
     *  <li>WARNING</li>
     *  <li>INFO</li>
     *  <li>CONFIG</li>
     *  <li>FINE</li>
     *  <li>FINER</li>
     *  <li>FINEST (lowest value)</li>
     *  <li>OFF</li>
     * </ul>
     *
     * @param logLevel
     *             one of the valid log levels according to {@link Level#parse(String)}
     */
    private static void setLogLevel(String logLevel) {
        Level level;

        try {
            level = Level.parse(logLevel);
        } catch (IllegalArgumentException e) {
            LOG.warning(() -> "Invalid log level %s. Must be one of OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST or ALL.");
            return;
        }

        Logger root = LogManager.getLogManager().getLogger(Main.class.getPackage().getName());
        root.setLevel(level);

        for (Handler handler : root.getHandlers()) {
            handler.setLevel(level);
        }
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
     */
    private static void printStrategies() {
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
    private static void dumpTrees(final MergeContext context) throws IOException {
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
    private static void dumpFiles(final MergeContext context) throws IOException {
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
    private static void bugfixing(final MergeContext context) throws IOException {
        context.setPretend(true);
        context.setQuiet(false);
        setLogLevel("FINEST");

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
