/**
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
import java.security.Permission;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import de.fosd.jdime.common.ASTNodeArtifact;
import de.fosd.jdime.common.ArtifactList;
import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeScenario;
import de.fosd.jdime.common.MergeType;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.config.CommandLineConfigSource;
import de.fosd.jdime.config.JDimeConfig;
import de.fosd.jdime.stats.Statistics;
import de.fosd.jdime.strategy.MergeStrategy;
import de.fosd.jdime.strategy.StrategyNotFoundException;
import de.fosd.jdime.strdump.DumpMode;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;

import static de.fosd.jdime.config.CommandLineConfigSource.CLI_DUMP;
import static de.fosd.jdime.config.CommandLineConfigSource.CLI_HELP;
import static de.fosd.jdime.config.CommandLineConfigSource.CLI_LOG_LEVEL;
import static de.fosd.jdime.config.CommandLineConfigSource.CLI_MODE;
import static de.fosd.jdime.config.CommandLineConfigSource.CLI_PROP_FILE;
import static de.fosd.jdime.config.CommandLineConfigSource.CLI_VERSION;
import static de.fosd.jdime.config.JDimeConfig.STATISTICS_HR_DEFAULT_NAME;
import static de.fosd.jdime.config.JDimeConfig.STATISTICS_HR_NAME;
import static de.fosd.jdime.config.JDimeConfig.STATISTICS_HR_OUTPUT;
import static de.fosd.jdime.config.JDimeConfig.STATISTICS_OUTPUT_OFF;
import static de.fosd.jdime.config.JDimeConfig.STATISTICS_OUTPUT_STDOUT;
import static de.fosd.jdime.config.JDimeConfig.STATISTICS_OUTPUT_USE_UNIQUE_FILES;
import static de.fosd.jdime.config.JDimeConfig.STATISTICS_XML_DEFAULT_NAME;
import static de.fosd.jdime.config.JDimeConfig.STATISTICS_XML_NAME;
import static de.fosd.jdime.config.JDimeConfig.STATISTICS_XML_OUTPUT;

/**
 * Contains the main method of the application.
 */
public final class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getCanonicalName());

    public static final String TOOLNAME = "jdime";
    public static final String VERSION = "0.4.0-develop";

    private static final String MODE_LIST = "list";

    private static JDimeConfig config;
    private static CommandLineConfigSource cmdLine;

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

        if (context.getDumpMode() != DumpMode.NONE) {

            for (FileArtifact artifact : context.getInputFiles()) {
                dump(artifact, context.getDumpMode());
            }
        } else {
            if (context.getInputFiles().size() < MergeType.MINFILES) {
                printCLIHelp();
                return;
            }

            merge(context);

            if (context.hasStatistics()) {
                outputStatistics(context.getStatistics());
            }
        }

        if (LOG.isLoggable(Level.CONFIG)) {
            Map<MergeScenario<?>, Throwable> crashes = context.getCrashes();
            String ls = System.lineSeparator();
            StringBuilder sb = new StringBuilder();

            sb.append(String.format("%d crashes occurred while merging:%n", crashes.size()));

            for (Map.Entry<MergeScenario<?>, Throwable> entry : crashes.entrySet()) {
                sb.append("* ").append(entry.getValue().toString()).append(ls);
                sb.append("    ").append(entry.getKey().toString().replace(" ", ls + "    ")).append(ls);
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
     */
    private static boolean parseCommandLineArgs(MergeContext context, String[] args) {
        LOG.fine(() -> "Parsing command line arguments: " + Arrays.toString(args));

        CommandLineConfigSource cmdLine;

        try {
            cmdLine = new CommandLineConfigSource(args, 3);
        } catch (ParseException e) {
            LOG.log(Level.SEVERE, e, () -> "Arguments could not be parsed!");
            return false;
        }

        JDimeConfig config;
        Optional<String> pFilePath = cmdLine.get(CLI_PROP_FILE);

        if (pFilePath.isPresent()) {
            config = new JDimeConfig(new File(pFilePath.get()));
        } else {
            config = new JDimeConfig();
        }

        config.addSource(cmdLine);

        Main.config = config;
        Main.cmdLine = cmdLine;

        if (config.getBoolean(CLI_HELP).orElse(false)) {
            printCLIHelp();
            return false;
        }

        if (config.getBoolean(CLI_VERSION).orElse(false)) {
            System.out.println(TOOLNAME + " VERSION " + VERSION);
            return false;
        }

        config.get(CLI_LOG_LEVEL).ifPresent(JDimeConfig::setLogLevel);

        Function<String, Optional<DumpMode>> dmpModeParser = mode -> {

            try {
                return Optional.of(DumpMode.valueOf(mode.toUpperCase()));
            } catch (IllegalArgumentException e) {
                LOG.log(Level.WARNING, e, () -> "Invalid dump format " + mode);
                return Optional.of(DumpMode.NONE);
            }
        };

        context.setDumpMode(config.get(CLI_DUMP, dmpModeParser).orElse(DumpMode.NONE));

        Optional<String> mode = config.get(CLI_MODE).map(String::toLowerCase);

        if (mode.isPresent()) {

            if (MODE_LIST.equals(mode.get())) {
                printStrategies();
                return false;
            } else {

                try {
                    context.setMergeStrategy(MergeStrategy.parse(mode.get()));
                } catch (StrategyNotFoundException e) {
                    LOG.log(Level.SEVERE, e, () -> "Strategy not found.");
                    return false;
                }
            }
        }

        context.configureFrom(config);

        return true;
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
     * Dumps the given <code>FileArtifact</code> using the <code>mode</code>.
     *
     * @param artifact
     *         the <code>Artifact</code> to dump
     * @param mode
     *         the dump format
     */
    private static void dump(FileArtifact artifact, DumpMode mode) {

        if (mode == DumpMode.NONE) {
            return;
        }

        if (mode == DumpMode.FILE_DUMP) {
            System.out.println(artifact.dump(mode));
        } else {
            SecurityManager prevSecManager = System.getSecurityManager();
            SecurityManager noExitManager = new SecurityManager() {
                @Override
                public void checkPermission(Permission perm) {
                    // allow anything.
                }

                @Override
                public void checkPermission(Permission perm, Object context) {
                    // allow anything.
                }

                @Override
                public void checkExit(int status) {
                    super.checkExit(status);
                    throw new SecurityException("Captured attempt to exit JVM.");
                }
            };

            ASTNodeArtifact astArtifact;

            System.setSecurityManager(noExitManager);

            try {
                astArtifact = new ASTNodeArtifact(artifact);
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, e, () -> "Could not parse " + artifact + " to an ASTNodeArtifact.");
                return;
            } finally {
                System.setSecurityManager(prevSecManager);
            }

            System.out.println(astArtifact.dump(mode));
        }
    }

    /**
     * Prints usage information and a help text about the command line options to <code>System.out</code>.
     */
    private static void printCLIHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(Main.TOOLNAME, cmdLine.getOptions(), true);
    }
}
