/**
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2018 University of Passau, Germany
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

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.artifact.ast.ASTNodeArtifact;
import de.fosd.jdime.artifact.file.FileArtifact;
import de.fosd.jdime.config.JDimeConfig;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.config.merge.MergeScenario;
import de.fosd.jdime.execption.AbortException;
import de.fosd.jdime.matcher.Matcher;
import de.fosd.jdime.matcher.matching.Color;
import de.fosd.jdime.matcher.matching.Matching;
import de.fosd.jdime.matcher.matching.Matchings;
import de.fosd.jdime.operations.MergeOperation;
import de.fosd.jdime.stats.KeyEnums;
import de.fosd.jdime.stats.Statistics;
import de.fosd.jdime.strdump.DumpMode;
import de.uni_passau.fim.seibt.LibGit2;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.Permission;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static de.fosd.jdime.config.CommandLineConfigSource.CLI_HELP;
import static de.fosd.jdime.config.CommandLineConfigSource.CLI_VERSION;
import static de.fosd.jdime.config.JDimeConfig.*;

/**
 * Contains the main method of the application.
 */
public final class Main {

    private static final Logger LOG;

    /**
     * Values used for configuring the <code>LogManager</code> in {@link #readLoggingConfig()}.
     */
    private static final String LOGGING_CONFIG_FILE_PROPERTY = "java.util.logging.config.file";
    private static final String LOGGING_CONFIG_FILE = "JDimeLogging.properties";
    private static final String DEFAULT_LOGGING_CONFIG_FILE = "DefaultLogging.properties";

    static {
        /*
         * Logging has to be configured FIRST before any classes are loaded whose initialization of static
         * fields includes calls to Logger#getLogger. Loggers constructed before the correct configuration was
         * read will not be fixed by a later call to LogManager#readConfiguration. There is no easy way to
         * manually fix all constructed Logger instances.
         */

        readLoggingConfig();
        LOG = Logger.getLogger(Main.class.getCanonicalName());
    }

    private static final SecurityManager SYS_SEC_MANAGER = System.getSecurityManager();
    private static final SecurityManager NO_EXIT_SEC_MANAGER = new SecurityManager() {
        @Override
        public void checkPermission(Permission perm) {
            // Allow everything
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything
        }

        @Override
        public void checkExit(int status) {
            // Disallow halting the JVM (ExtendJ is known to do this...)
            super.checkExit(status);
            throw new SecurityException("Captured attempt to exit JVM.");
        }
    };

    public static final String TOOLNAME = "jdime";
    public static final String VERSION = "0.5-develop";

    public static final int EXIT_SUCCESS = 0;
    public static final int EXIT_ABORTED = 200;
    public static final int EXIT_FAILURE = 210;

    // Exit codes used for --mode compare
    public static final int EXIT_COMPARE_EQUAL = EXIT_SUCCESS;
    public static final int EXIT_COMPARE_NOT_EQUAL = 220;

    private static JDimeConfig config;

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
            int exitCode;

            try {
                System.setSecurityManager(NO_EXIT_SEC_MANAGER);
                exitCode = run(args);
            } finally {
                System.setSecurityManager(SYS_SEC_MANAGER);
            }

            System.exit(exitCode);
        } catch (AbortException e) {

            if (e.getCause() != null) {
                LOG.log(Level.SEVERE, e.getCause(), () -> "Aborting the merge.");
            } else {
                System.err.println(e.getMessage());
                LOG.log(Level.FINE, e, () -> "Aborting the merge.");
            }

            System.exit(EXIT_ABORTED);
        } catch (Throwable e) {
            LOG.log(Level.SEVERE, e, () -> "Uncaught exception.");
            System.exit(EXIT_FAILURE);
        }
    }

    /**
     * Perform a merge operation on the input files or directories.
     *
     * @param args command line arguments
     *
     * @return the exit code for the program;
     *         a positive value if stats are enabled and there were conflicts (the sum, truncated to 127),
     *         a value >= 200 if there was an error ({@link #EXIT_FAILURE}, {@link #EXIT_ABORTED}),
     *         {@value EXIT_SUCCESS} otherwise
     */
    public static int run(String[] args) {
        MergeContext context = new MergeContext();

        JDimeConfig config;

        try {
            config = new JDimeConfig(args);
        } catch (ParseException e) {
            System.err.println("Failed to parse the command line arguments " + Arrays.toString(args));
            System.err.println(e.getMessage());
            return EXIT_FAILURE;
        }

        Main.config = config;

        if (args.length == 0 || config.getBoolean(CLI_HELP).orElse(false)) {
            printCLIHelp();
            return EXIT_SUCCESS;
        }

        if (config.getBoolean(CLI_VERSION).orElse(false)) {
            Optional<String> commit = config.get(JDIME_COMMIT);

            System.out.printf("%s version %s", TOOLNAME, VERSION);
            commit.ifPresent(s -> System.out.printf(" commit %s", s));

            System.out.printf(" using libgit2 %s%n", LibGit2.git_libgit2_version());
            return EXIT_SUCCESS;
        }

        context.configureFrom(config);

        List<FileArtifact> inputFiles = context.getInputFiles();

        if (context.isInspect()) {
            inspectElement(inputFiles.get(0), context.getInspectArtifact(), context.getInspectionScope());
            return EXIT_SUCCESS;
        }

        if (context.getDumpMode() != DumpMode.NONE) {
            inputFiles.forEach(artifact -> dump(artifact, context.getDumpMode()));
            return EXIT_SUCCESS;
        }

        if (context.isCompare()) {
            return compare(context);
        }

        try {
            merge(context);
            output(context);
        } finally {
            outputStatistics(context);
        }

        if (LOG.isLoggable(Level.FINE)) {
            Map<MergeScenario<?>, Throwable> crashes = context.getCrashes();

            if (crashes.isEmpty()) {
                LOG.fine("No crashes occurred while merging.");
            } else {
                String ls = System.lineSeparator();
                StringBuilder sb = new StringBuilder();

                sb.append(String.format("%d crashes occurred while merging:%n", crashes.size()));

                for (Map.Entry<MergeScenario<?>, Throwable> entry : crashes.entrySet()) {
                    sb.append("* ").append(entry.getValue().toString()).append(ls);
                    sb.append("    ").append(entry.getKey().toString().replace(" ", ls + "    ")).append(ls);
                }

                LOG.fine(sb.toString());
            }
        }

        if (context.hasStatistics()) {
            long conflicts = context.getStatistics().getConflictStatistics().getSum();
            final int MAX_CONFLICTS = 127;

            if (conflicts > MAX_CONFLICTS) {
                LOG.warning("Produced " + conflicts + " conflicts. Truncating to " + MAX_CONFLICTS +
                            " to comply with 'git merge-file'.");
                return MAX_CONFLICTS;
            } else {
                return (int) conflicts;
            }
        } else {
            LOG.fine("Statistics are not enabled, exiting with code 0 even though there might have been conflicts.");
            return EXIT_SUCCESS;
        }
    }

    /**
     * Outputs the merge result to the filesystem or stdout depending on the {@link MergeContext} configuration.
     *
     * @param context
     *         the {@link MergeContext} whose {@link MergeContext#getOutputFile()} to use
     */
    private static void output(MergeContext context) {
        FileArtifact outFile = context.getOutputFile();

        if (context.isPretend()) {
            if (!context.isQuiet()) {
                outFile.outputContent(System.out);
            }
        } else {
            try {
                outFile.writeContent();
            } catch (IOException e) {
                LOG.log(Level.WARNING, e, () -> "Could not write the merge result to the filesystem.");
            }
        }
    }

    /**
     * Outputs the {@link Statistics} in the given {@link MergeContext}. Does nothing if the {@link MergeContext} does
     * not contain {@link Statistics}.
     *
     * @param context
     *         the {@link MergeContext} containing the {@link Statistics} to output
     */
    private static void outputStatistics(MergeContext context) {

        if (!context.hasStatistics()) {
            return;
        }

        Statistics statistics = context.getStatistics();

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
                statistics.printXML(System.out, context);
                System.out.println();
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
                    statistics.printXML(f, context);
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
            extension = fullName.substring(pos);
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
     * Ensures that logging is configured. If the system property is set to an existing file then nothing is done as
     * that config was already read at JVM startup. If not, a file named {@value LOGGING_CONFIG_FILE} in
     * the working directory is used if it exists. If it does not, the default configuration file is read from the
     * classpath.
     */
    private static void readLoggingConfig() {

        {
            String logConfigProperty = System.getProperty(LOGGING_CONFIG_FILE_PROPERTY);

            if (logConfigProperty != null && new File(logConfigProperty).exists()) {
                // The config file was already read at JVM startup.
                return;
            }
        }

        try {
            File configFile = new File(LOGGING_CONFIG_FILE);
            InputStream is;

            if (configFile.exists()) {
                is = FileUtils.openInputStream(configFile);
            } else {
                System.err.println("Logging configuration file " + configFile + " does not exist. " +
                                   "Falling back to defaults.");

                is = Main.class.getResourceAsStream(DEFAULT_LOGGING_CONFIG_FILE);

                if (is == null) {
                    System.err.println("Could not find the default logging configuration.");
                    return;
                }
            }

            try {
                LogManager.getLogManager().readConfiguration(is);
            } finally {
                try { is.close(); } catch (IOException ignored) { }
            }
        } catch (IOException e) {
            System.err.println("Failed to configure logging.");
            e.printStackTrace();
        }
    }

    /**
     * Merges the input files.
     *
     * @param context
     *         merge context
     */
    public static void merge(MergeContext context) {
        List<FileArtifact> inFiles = context.getInputFiles();
        FileArtifact outFile = context.getOutputFile();

        if (context.isFilterInputDirectories() && !context.isAcceptNonJava()) {
            inFiles.forEach(FileArtifact::filterNonJavaFiles);
        }

        boolean conditional = context.isConditionalMerge();
        MergeOperation<FileArtifact> merge = new MergeOperation<>(inFiles, outFile, conditional);
        Optional.ofNullable(context.getMergeScenarioLabel()).ifPresent(l -> merge.getMergeScenario().setLabel(l));

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

        if (mode == DumpMode.FILE_DUMP || artifact.isDirectory()) {
            System.out.println(artifact.dump(mode));
        } else {
            ASTNodeArtifact astArtifact;

            try {
                astArtifact = new ASTNodeArtifact(artifact);
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, e, () -> "Could not parse " + artifact + " to an ASTNodeArtifact.");
                return;
            }

            System.out.println(astArtifact.dump(mode));
        }
    }

    /**
     * Parses the given <code>artifact</code> to an AST and attempts to find a node with the given <code>number</code>
     * in the tree. If found, the {@link DumpMode#PRETTY_PRINT_DUMP} will be used to dump the node to standard out.
     * If <code>scope</code> is not {@link KeyEnums.Type#NODE}, the method will walk up the tree to find a node that
     * fits the requested <code>scope</code> and dump it instead.
     *
     * @param artifact
     *         the <code>FileArtifact</code> to parse to an AST
     * @param number
     *         the number of the <code>artifact</code> in the AST to find
     * @param scope
     *         the scope to dump
     */
    private static void inspectElement(FileArtifact artifact, int number, KeyEnums.Type scope) {
        ASTNodeArtifact astArtifact = new ASTNodeArtifact(artifact);
        Optional<Artifact<ASTNodeArtifact>> foundNode = astArtifact.find(number);

        if (foundNode.isPresent()) {
            Artifact<ASTNodeArtifact> element = foundNode.get();

            if (scope != KeyEnums.Type.NODE) {
                // walk tree upwards until scope fits
                while (scope != element.getType() && !element.isRoot()) {
                    element = element.getParent();
                }
            }

            System.out.println(element.dump(DumpMode.PRETTY_PRINT_DUMP));
        } else {
            LOG.log(Level.WARNING, () -> "Could not find a node with number " + number + ".");
        }
    }

    /**
     * Expects exactly two input {@link FileArtifact.FileType#FILE files} and uses the {@link Matcher} to compare them.
     * If the ASTs fully match, the method returns {@link #EXIT_COMPARE_EQUAL}, otherwise
     * {@link #EXIT_COMPARE_NOT_EQUAL}.
     *
     * @param context the {@link MergeContext} containing the input files
     * @return whether the ASTs of the input files fully match
     */
    private static int compare(MergeContext context) {
        List<FileArtifact> inputFiles = context.getInputFiles();

        // MergeContext checks that there are exactly two input files for compare mode
        FileArtifact left = inputFiles.get(0);
        FileArtifact right = inputFiles.get(1);

        ASTNodeArtifact leftAST = new ASTNodeArtifact(left);
        ASTNodeArtifact rightAST = new ASTNodeArtifact(right);

        Matcher<ASTNodeArtifact> matcher = new Matcher<>(leftAST, rightAST);
        Matchings<ASTNodeArtifact> matches = matcher.match(context, Color.DEFAULT);

        Optional<Matching<ASTNodeArtifact>> optRootMatching = matches.get(leftAST, rightAST);

        if (!optRootMatching.isPresent()) {
            return EXIT_COMPARE_NOT_EQUAL;
        }

        Matching<ASTNodeArtifact> rootMatching = optRootMatching.get();

        if (rootMatching.hasFullyMatched()) {
            return EXIT_COMPARE_EQUAL;
        } else {
            return EXIT_COMPARE_NOT_EQUAL;
        }
    }

    /**
     * Prints usage information and a help text about the command line options to <code>System.out</code>.
     */
    private static void printCLIHelp() {
        String ls = System.lineSeparator();
        HelpFormatter formatter = new HelpFormatter();

        String syntax = Main.TOOLNAME + " <left file> [<base file>] <right file>";
        String header = ls + "Perform a two or three way merge between the input files." + ls + ls;
        String footer = ls + "Please report issues at https://github.com/se-passau/jdime/issues";

        formatter.printHelp(120, syntax, header, config.getCmdLine().getOptions(), footer, true);
    }
}
