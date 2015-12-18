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
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import de.fosd.jdime.common.ArtifactList;
import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeScenario;
import de.fosd.jdime.common.MergeType;
import de.fosd.jdime.common.Revision;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.stats.Statistics;
import de.fosd.jdime.strategy.MergeStrategy;
import de.fosd.jdime.strategy.StrategyNotFoundException;
import de.uni_passau.fim.seibt.kvconfig.Config;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;

import static de.fosd.jdime.JDimeConfig.*;

/**
 * Contains the main method of the application.
 */
public final class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getCanonicalName());

    private static final String TOOLNAME = "jdime";
    private static final String VERSION = "0.4.0-develop";

    /**
     * Prevent instantiation.
     */
    private Main() {}

    /**
     * Perform a merge operation on the input files or directories.
     *
     * @param args
     *         command line arguments
     */
    public static void main(String[] args) {

        try {
            run(args);
        } catch (Throwable e) {
            LOG.log(Level.SEVERE, e, () -> "Uncaught exception.");
            System.exit(1);
        }
    }

    /**
     * Perform a merge operation on the input files or directories.
     *
     * @param args
     *         command line arguments
     */
    public static void run(String[] args) throws IOException, ParseException, InterruptedException {
        MergeContext context = new MergeContext();

        if (!parseCommandLineArgs(context, args)) {
            return;
        }

        ArtifactList<FileArtifact> inputFiles = context.getInputFiles();
        FileArtifact output = context.getOutputFile();

        for (FileArtifact inputFile : inputFiles) {

            if (inputFile.isDirectory() && !context.isRecursive()) {
                String msg = "To merge directories, the argument '-r' has to be supplied. See '-help' for more information!";

                LOG.severe(msg);
                System.err.println(msg);

                return;
            }
        }

        if (output != null && output.exists() && !output.isEmpty()) {
            boolean overwrite;

            try (BufferedReader r = new BufferedReader(new InputStreamReader(System.in))) {
                System.err.println("Output directory is not empty!");
                System.err.println("Delete '" + output.getFullPath() + "'? [y/N]");

                String response = r.readLine().trim().toLowerCase();
                overwrite = response.length() != 0 && response.charAt(0) == 'y';
            }

            if (overwrite) {
                LOG.warning("File exists and will be overwritten.");

                output.remove();

                if (output.isDirectory()) {
                    FileUtils.forceMkdir(output.getFile());
                }
            } else {
                String msg = "File exists and will not be overwritten.";

                LOG.severe(msg);
                System.err.println(msg);

                return;
            }

        }

        if (context.isDumpTree()) {
            dumpTrees(context);
        } else if (context.isDumpFile()) {
            dumpFiles(context);
        } else {
            merge(context);
        }

        if (context.hasStatistics()) {
            outputStatistics(context.getStatistics());
        }

        if (LOG.isLoggable(Level.CONFIG)) {
            Map<MergeScenario<?>, Throwable> crashes = context.getCrashes();
            String ls = System.lineSeparator();
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%d crashes occurred while merging:\n", crashes.size()));

            for (MergeScenario<?> scenario : crashes.keySet()) {
                Throwable t = crashes.get(scenario);
                sb.append("* ").append(t.toString()).append(ls);
                sb.append("    ").append(scenario.toString().replace(" ", ls + "    ")).append(ls);
            }

            LOG.config(sb.toString());
        }
    }

    /**
     * Outputs the given <code>Statistics</code> according to the set configuration options.
     *
     * @param statistics
     *         the <code>Statistics</code> to output
     */
    private static void outputStatistics(Statistics statistics) {
        Config config = getConfig();

        String hrOut = config.get(STATISTICS_HR_OUTPUT).orElse(STATISTICS_OUTPUT_STDOUT);
        String xmlOut = config.get(STATISTICS_XML_OUTPUT).orElse(STATISTICS_OUTPUT_OFF);

        switch (hrOut) {
            case STATISTICS_OUTPUT_OFF:
                LOG.fine("Human readable statistics output is disabled.");
                break;
            case STATISTICS_OUTPUT_STDOUT:
                statistics.print(System.out);
                break;
            default: {
                File f = new File(hrOut);

                if (f.isDirectory()) {
                    String name = config.get(STATISTICS_HR_NAME).orElse(STATISTICS_HR_DEFAULT_NAME);
                    f = new File(f, String.format(name, new Date()));
                }

                if (config.getBoolean(STATISTICS_OUTPUT_USE_UNIQUE_FILES).orElse(true)) {
                    f = findNonExistent(f);
                }

                try {
                    statistics.print(f);
                } catch (FileNotFoundException e) {
                    LOG.log(Level.WARNING, e, () -> "Statistics output failed.");
                }
            }
        }

        switch (xmlOut) {
            case STATISTICS_OUTPUT_OFF:
                LOG.fine("XML statistics output is disabled.");
                break;
            case STATISTICS_OUTPUT_STDOUT:
                statistics.printXML(System.out);
                break;
            default: {
                File f = new File(xmlOut);

                if (f.isDirectory()) {
                    String name = config.get(STATISTICS_XML_NAME).orElse(STATISTICS_XML_DEFAULT_NAME);
                    f = new File(f, String.format(name, new Date()));
                }

                if (config.getBoolean(STATISTICS_OUTPUT_USE_UNIQUE_FILES).orElse(true)) {
                    f = findNonExistent(f);
                }

                try {
                    statistics.printXML(f);
                } catch (FileNotFoundException e) {
                    LOG.log(Level.WARNING, e, () -> "Statistics output failed.");
                }
            }
        }
    }

    /**
     * Returns a <code>File</code> (possibly <code>f</code>) that does not exist in the parent directory of
     * <code>f</code>. If <code>f</code> exists an increasing number is appended to the name of <code>f</code> until
     * a <code>File</code> is found that does not exist.
     *
     * @param f
     *         the <code>File</code> to find a non existent version of
     * @return a <code>File</code> in the parent directory of <code>f</code> that does not exist
     */
    private static File findNonExistent(File f) {

        if (!f.exists()) {
            return f;
        }

        String fullName = f.getName();
        String name;
        String extension;

        int pos = fullName.lastIndexOf('.');

        if (pos != -1) {
            name = fullName.substring(0, pos);
            extension = fullName.substring(pos, fullName.length());
        } else {
            name = fullName;
            extension = "";
        }

        File parent = f.getParentFile();

        Stream<File> files = IntStream.range(0, Integer.MAX_VALUE).mapToObj(v -> {
            String fileName = String.format("%s_%d%s", name, v, extension);
            return new File(parent, fileName);
        });

        File nextFree = files.filter(file -> !file.exists()).findFirst().orElseThrow(() ->
                new RuntimeException("Can not find a file that does not exist."));

        return nextFree;
    }

    /**
     * Parses command line arguments and initializes program.
     *
     * @param context
     *         merge context
     * @param args
     *         command line arguments
     * @return true if program should continue
     * @throws IOException
     *         If an input output exception occurs
     * @throws ParseException
     *         If arguments cannot be parsed
     */
    private static boolean parseCommandLineArgs(MergeContext context, String[] args) throws IOException, ParseException {
        LOG.fine(() -> "Parsing command line arguments: " + Arrays.toString(args));

        try {
            CommandLine cmd = JDimeConfig.parseArgs(args);

            if (cmd.hasOption(CLI_HELP)) {
                JDimeConfig.printCLIHelp();
                return false;
            }

            if (cmd.hasOption(CLI_VERSION)) {
                version();
                return false;
            }

            if (cmd.hasOption(CLI_LOG_LEVEL)) {
                setLogLevel(cmd.getOptionValue(CLI_LOG_LEVEL));
            }

            if (cmd.hasOption(CLI_MODE)) {
                try {
                    switch (cmd.getOptionValue(CLI_MODE).toLowerCase()) {
                        case "list":
                            printStrategies();
                            return false;
                        case "dumptree":
                            // User only wants to display the ASTs
                            context.setMergeStrategy(MergeStrategy.parse("structured"));
                            context.setDumpTree(true);
                            context.setGuiDump(false);
                            break;
                        case "dumpgraph":
                            // User only wants to display the ASTs
                            context.setMergeStrategy(MergeStrategy.parse("structured"));
                            context.setDumpTree(true);
                            context.setGuiDump(true);
                            break;
                        case "dumpfile":
                            // User only wants to display the files
                            context.setMergeStrategy(MergeStrategy.parse("linebased"));
                            context.setDumpFiles(true);
                            break;
                        case "prettyprint":
                            // User wants to parse and pretty-print file
                            context.setMergeStrategy(MergeStrategy.parse("structured"));
                            context.setDumpFiles(true);
                            break;
                        default:
                            context.setMergeStrategy(MergeStrategy.parse(cmd.getOptionValue(CLI_MODE)));
                    }
                } catch (StrategyNotFoundException e) {
                    LOG.log(Level.SEVERE, e, () -> "Strategy not found.");
                    return false;
                }

                if (context.getMergeStrategy() == null) {
                    JDimeConfig.printCLIHelp();
                    return false;
                }
            }

            if (cmd.hasOption(CLI_DIFFONLY)) {
                context.setDiffOnly(true);

                if (cmd.hasOption(CLI_CONSECUTIVE)) {
                    context.setConsecutive(true);
                }
            }

            if (cmd.hasOption(CLI_LOOKAHEAD)) {
                String lookAheadValue = cmd.getOptionValue(CLI_LOOKAHEAD);

                // initialize with the context's default.
                int lookAhead = context.getLookAhead();

                // parse the value provided by the user
                try {
                    lookAhead = Integer.parseInt(lookAheadValue);
                } catch (NumberFormatException e) {
                    switch (lookAheadValue) {
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

            context.collectStatistics(cmd.hasOption(CLI_STATS));
            context.setForceOverwriting(cmd.hasOption(CLI_FORCE_OVERWRITE));
            context.setRecursive(cmd.hasOption(CLI_RECURSIVE));

            if (cmd.hasOption(CLI_PRINT)) {
                context.setPretend(true);
                context.setQuiet(false);
            } else if (cmd.hasOption(CLI_QUIET)) {
                context.setQuiet(true);
            }

            context.setKeepGoing(cmd.hasOption(CLI_KEEPGOING));

            if (cmd.hasOption(CLI_SHOWCONFIG)) {
                showConfig(context);
                return false;
            }

            int numInputFiles = cmd.getArgList().size();

            if (!((context.isDumpTree() || context.isDumpFile()) || numInputFiles >= MergeType.MINFILES)) {
                JDimeConfig.printCLIHelp();
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
                    System.err.println("Input file not found: " + filename);
                }
            }

            context.setInputFiles(inputArtifacts);

            String outputFileName = null;
            if (cmd.hasOption(CLI_OUTPUT)) {
                // TODO[low priority]: The default should in a later,
                // rock-stable version be changed to be overwriting file1 so
                // that we are compatible with gnu merge call syntax
                outputFileName = cmd.getOptionValue(CLI_OUTPUT);
            }

            if (outputFileName != null) {
                Revision mergeRev = new Revision("merge");
                FileArtifact outArtifact = new FileArtifact(mergeRev, new File(outputFileName), true, targetIsFile);

                context.setOutputFile(outArtifact);
                context.setPretend(false);
            }
        } catch (ParseException e) {
            LOG.log(Level.SEVERE, e, () -> "Arguments could not be parsed!");
            return false;
        }

        return true;
    }

    /**
     * Print version information.
     */
    private static void version() {
        System.out.println(TOOLNAME + " VERSION " + VERSION);
    }

    /**
     * Prints configuration information.
     *
     * @param context
     *         merge context
     */
    private static void showConfig(final MergeContext context) {
        assert (context != null);
        System.out.println("Merge strategy: " + context.getMergeStrategy());
        System.out.println();
    }

    /**
     * Prints the available strategies.
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
     *         merge context
     * @throws InterruptedException
     *         If a thread is interrupted
     * @throws IOException
     *         If an input output exception occurs
     */
    public static void merge(MergeContext context) throws IOException, InterruptedException {
        ArtifactList<FileArtifact> inFiles = context.getInputFiles();
        FileArtifact outFile = context.getOutputFile();
        boolean conditional = context.isConditionalMerge();
        MergeOperation<FileArtifact> merge = new MergeOperation<>(inFiles, outFile, null, null, conditional);

        if (context.hasStatistics()) {
            context.getStatistics().setCurrentFileMergeScenario(merge.getMergeScenario());
        }

        merge.apply(context);
    }

    /**
     * Mainly used for debugging purposes.
     *
     * @param context
     *         merge context
     * @throws IOException
     *         If an input output exception occurs
     */
    @SuppressWarnings("unchecked")
    private static void dumpTrees(final MergeContext context) throws IOException {
        for (FileArtifact artifact : context.getInputFiles()) {
            MergeStrategy<FileArtifact> strategy = (MergeStrategy<FileArtifact>) context.getMergeStrategy();
            System.out.println(strategy.dumpTree(artifact, context.isGuiDump()));
        }
    }

    /**
     * Mainly used for debugging purposes.
     *
     * @param context
     *         merge context
     * @throws IOException
     *         If an input output exception occurs
     */
    @SuppressWarnings("unchecked")
    private static void dumpFiles(final MergeContext context) throws IOException {
        for (FileArtifact artifact : context.getInputFiles()) {
            MergeStrategy<FileArtifact> strategy = (MergeStrategy<FileArtifact>) context.getMergeStrategy();
            System.out.println(strategy.dumpFile(artifact, context.isGuiDump()));
        }
    }
}
