package de.fosd.jdime;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.fosd.jdime.matcher.ordered.mceSubtree.MCESubtreeMatcher;
import de.uni_passau.fim.seibt.kvconfig.Config;
import de.uni_passau.fim.seibt.kvconfig.sources.PropFileConfigSource;
import de.uni_passau.fim.seibt.kvconfig.sources.SysEnvConfigSource;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Contains the singleton <code>Config</code> instance containing the configuration options for JDime. All
 * keys used for retrieving config options should be declared as static final <code>String</code>s in this class.
 */
public final class JDimeConfig {

    private static final Logger LOG = Logger.getLogger(JDimeConfig.class.getCanonicalName());

    /**
     * The file name of the JDime configuration file.
     */
    private static final String CONFIG_FILE_NAME = "JDime.properties";

    /**
     * The default value for the 'Args' text field in the GUI.
     */
    public static final String DEFAULT_ARGS = "DEFAULT_ARGS";

    /**
     * The default value for the 'Left' text field in the GUI.
     */
    public static final String DEFAULT_LEFT = "DEFAULT_LEFT";

    /**
     * The default value for the 'Base' text field in the GUI.
     */
    public static final String DEFAULT_BASE = "DEFAULT_BASE";

    /**
     * The default value for the 'Right' text field in the GUI.
     */
    public static final String DEFAULT_RIGHT = "DEFAULT_RIGHT";

    /**
     * The default value for the 'JDime' text field in the GUI.
     */
    public static final String DEFAULT_JDIME_EXEC = "DEFAULT_JDIME_EXEC";

    /**
     * Whether to allow invalid values (such as non-existent files) for the text fields in the GUI. Must be either
     * 'true' or 'false'.
     */
    public static final String ALLOW_INVALID = "ALLOW_INVALID";

    /**
     * How many lines of JDime output to buffer before adding them to the displayed lines in the GUI. Must
     * be a number parseable by {@link Integer#parseInt(String)}.
     */
    public static final String BUFFERED_LINES = "BUFFERED_LINES";

    /**
     * Whether to use the {@link MCESubtreeMatcher} when diffing. Must be either 'true' or 'false'.
     */
    public static final String USE_MCESUBTREE_MATCHER = "USE_MCESUBTREE_MATCHER";

    /**
     * Whether to append a number to the file name to ensure that no file of the same name is overwritten when
     * writing the statistics. Must be either 'true' or 'false'. Defaults to true.
     */
    public static final String STATISTICS_OUTPUT_USE_UNIQUE_FILES = "STATISTICS_OUTPUT_USE_UNIQUE_FILES";

    /**
     * Using this value for {@link #STATISTICS_HR_OUTPUT} or {@link #STATISTICS_XML_OUTPUT} disables the output.
     */
    public static final String STATISTICS_OUTPUT_OFF = "off";

    /**
     * Using this value for {@link #STATISTICS_HR_OUTPUT} or {@link #STATISTICS_XML_OUTPUT} sends the output to standard
     * out.
     */
    public static final String STATISTICS_OUTPUT_STDOUT = "stdout";

    /**
     * Where to send the human readable statistics output if '-stats' is given on the command line. If the value denotes
     * a file this file will be written to, if it denotes a directory a file will be created there using the pattern
     * specified in {@link #STATISTICS_HR_NAME}. Paths are relative to the current working directory.
     * Defaults to {@link #STATISTICS_OUTPUT_STDOUT}.
     *
     * @see #STATISTICS_OUTPUT_OFF
     * @see #STATISTICS_OUTPUT_STDOUT
     * @see #STATISTICS_OUTPUT_USE_UNIQUE_FILES
     */
    public static final String STATISTICS_HR_OUTPUT = "STATISTICS_HR_OUTPUT";

    /**
     * A {@link String#format(Locale, String, Object...)} pattern to be used when creating a new file to write
     * the human readable statistics output to. The current {@link Date} will be passed to the format method as its
     * first parameter after the format <code>String</code>. Defaults to {@link #STATISTICS_HR_DEFAULT_NAME}.
     */
    public static final String STATISTICS_HR_NAME = "STATISTICS_HR_NAME";

    /**
     * The default name pattern when {@link #STATISTICS_HR_NAME} is not given.
     */
    public static final String STATISTICS_HR_DEFAULT_NAME = "Statistics_HR.txt";

    /**
     * Where to send the XML statistics output if '-stats' is given on the command line. If the value denotes
     * a file this file will be written to, if it denotes a directory a file will be created there using the pattern
     * specified in {@link #STATISTICS_XML_NAME}. Paths are relative to the current working directory.
     * Defaults to {@link #STATISTICS_OUTPUT_OFF}.
     *
     * @see #STATISTICS_OUTPUT_OFF
     * @see #STATISTICS_OUTPUT_STDOUT
     * @see #STATISTICS_OUTPUT_USE_UNIQUE_FILES
     */
    public static final String STATISTICS_XML_OUTPUT = "STATISTICS_XML_OUTPUT";

    /**
     * A {@link String#format(Locale, String, Object...)} pattern to be used when creating a new file to write
     * the XML statistics output to. The current {@link Date} will be passed to the format method as its
     * first parameter after the format <code>String</code>. Defaults to {@link #STATISTICS_XML_DEFAULT_NAME}.
     */
    public static final String STATISTICS_XML_NAME = "STATISTICS_XML_NAME";

    /**
     * The default name pattern when {@link #STATISTICS_XML_NAME} is not given.
     */
    public static final String STATISTICS_XML_DEFAULT_NAME = "Statistics_XML.xml";

    /*
     * These constants define the (short) parameter names expected on the command line. Corresponding Options
     * are constructed in buildCliOptions().
     */
    public static final String CLI_LOG_LEVEL = "log";
    public static final String CLI_CONSECUTIVE = "c";
    public static final String CLI_DIFFONLY = "d";
    public static final String CLI_FORCE_OVERWRITE = "f";
    public static final String CLI_HELP = "h";
    public static final String CLI_KEEPGOING = "k";
    public static final String CLI_LOOKAHEAD = "lah";
    public static final String CLI_MODE = "m";
    public static final String CLI_OUTPUT = "o";
    public static final String CLI_RECURSIVE = "r";
    public static final String CLI_SHOWCONFIG = "sc";
    public static final String CLI_STATS = "s";
    public static final String CLI_PRINT = "p";
    public static final String CLI_QUIET = "q";
    public static final String CLI_VERSION = "v";

    /**
     * The singleton is implicitly synchronized because the <code>InstanceHolder</code> class is only initialized by
     * the classloader when the {@link #getConfig()} method is first called.
     */
    private static final class InstanceHolder {
        private static final JDimeConfig INSTANCE = new JDimeConfig();
    }

    /**
     * Returns the singleton <code>Config</code> instance containing the configuration options for JDime.
     *
     * @return the <code>Config</code> instance
     */
    public static Config getConfig() {
        return InstanceHolder.INSTANCE.config;
    }

    /**
     * Returns a <code>CommandLine</code> instance containing the parsed command line options.
     *
     * @param args
     *         the command line arguments to parse
     * @return the resulting <code>CommandLine</code> instance
     * @throws ParseException
     *         if there is an exception parsing <code>args</code>
     */
    public static CommandLine parseArgs(String[] args) throws ParseException {
        return new DefaultParser().parse(InstanceHolder.INSTANCE.cliOptions, args);
    }

    /**
     * Prints usage information and a help text about the command line options to <code>System.out</code>.
     */
    public static void printCLIHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(Main.TOOLNAME, InstanceHolder.INSTANCE.cliOptions, true);
    }

    private Config config;
    private Options cliOptions;

    /**
     * Private constructor to prevent outside instantiation.
     */
    private JDimeConfig() {
        config = new Config();
        config.addSource(new SysEnvConfigSource(1));
        loadConfigFile();
        cliOptions = buildCliOptions();
    }

    /**
     * Builds the <code>Options</code> instance describing the JDime command line configuration options.
     *
     * @return the <code>Options</code> instance
     */
    private Options buildCliOptions() {
        Options options = new Options();
        Option o;

        o = Option.builder(CLI_LOG_LEVEL)
                .longOpt("log-level")
                .desc("Set the logging level to one of (OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL).")
                .hasArg()
                .argName("level")
                .build();

        options.addOption(o);

        o = Option.builder(CLI_CONSECUTIVE)
                .longOpt("consecutive")
                .desc("Requires diffonly mode. Treats versions as consecutive versions.")
                .hasArg(false)
                .build();

        options.addOption(o);

        o = Option.builder(CLI_DIFFONLY)
                .longOpt("diffonly")
                .desc("Only perform the diff stage.")
                .hasArg(false)
                .build();

        options.addOption(o);

        o = Option.builder(CLI_FORCE_OVERWRITE)
                .longOpt("force-overwrite")
                .desc("Force overwriting of output files.")
                .hasArg(false)
                .build();

        options.addOption(o);

        o = Option.builder(CLI_HELP)
                .longOpt("help")
                .desc("Print this message.")
                .hasArg(false)
                .build();

        options.addOption(o);

        o = Option.builder(CLI_KEEPGOING)
                .longOpt("keepgoing")
                .desc("Keep running after exceptions.")
                .hasArg(false)
                .build();

        options.addOption(o);

        o = Option.builder(CLI_LOOKAHEAD)
                .longOpt("lookahead")
                .desc("Use heuristics for matching. Supply off, full, or a number as argument.")
                .hasArg()
                .argName("level")
                .build();

        options.addOption(o);

        o = Option.builder(CLI_MODE)
                .longOpt("mode")
                .desc("Set the mode to one of (unstructured, structured, autotuning, dumptree, dumpgraph, dumpfile, " +
                        "prettyprint, nway)")
                .hasArg()
                .argName("mode")
                .build();

        options.addOption(o);

        o = Option.builder(CLI_OUTPUT)
                .longOpt("output")
                .desc("Set the output directory/file.")
                .hasArg()
                .argName("file")
                .build();

        options.addOption(o);

        o = Option.builder(CLI_RECURSIVE)
                .longOpt("recursive")
                .desc("Merge directories recursively.")
                .hasArg(false)
                .build();

        options.addOption(o);

        o = Option.builder(CLI_SHOWCONFIG)
                .longOpt("showconfig")
                .desc("Print configuration information.")
                .hasArg(false)
                .build();

        options.addOption(o);

        o = Option.builder(CLI_STATS)
                .longOpt("stats")
                .desc("Collect statistical data about the merge.")
                .hasArg(false)
                .build();

        options.addOption(o);

        o = Option.builder(CLI_PRINT)
                .longOpt("print")
                .desc("(print/pretend) Prints the merge result to stdout instead of an output file.")
                .hasArg(false)
                .build();

        options.addOption(o);

        o = Option.builder(CLI_QUIET)
                .longOpt("quiet")
                .desc("Do not print the merge result to stdout.")
                .hasArg(false)
                .build();

        options.addOption(o);

        o = Option.builder(CLI_VERSION)
                .longOpt("version")
                .desc("Print the version information and exit.")
                .hasArg(false)
                .build();

        options.addOption(o);

        return options;
    }

    /**
     * Checks whether the current working directory contains a file called {@value #CONFIG_FILE_NAME} and if so adds
     * a <code>PropFileConfigSource</code> to <code>config</code>.
     */
    private void loadConfigFile() {
        File configFile = new File(CONFIG_FILE_NAME);

        if (configFile.exists()) {

            try {
                config.addSource(new PropFileConfigSource(2, configFile));
            } catch (IOException e) {
                LOG.log(Level.WARNING, e, () -> "Could not add a ConfigSource for " + configFile.getAbsolutePath());
            }
        }
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
    public static void setLogLevel(String logLevel) {
        Level level;

        try {
            level = Level.parse(logLevel.toUpperCase());
        } catch (IllegalArgumentException e) {
            LOG.warning(() -> "Invalid log level %s. Must be one of OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST or ALL.");
            return;
        }

        Logger root = Logger.getLogger(Main.class.getPackage().getName());
        root.setLevel(level);

        for (Handler handler : root.getHandlers()) {
            handler.setLevel(level);
        }
    }
}
