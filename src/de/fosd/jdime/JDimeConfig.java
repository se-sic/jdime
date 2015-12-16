package de.fosd.jdime;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.fosd.jdime.matcher.ordered.mceSubtree.MCESubtreeMatcher;
import de.fosd.jdime.stats.KeyEnums;
import de.uni_passau.fim.seibt.kvconfig.Config;
import de.uni_passau.fim.seibt.kvconfig.sources.PropFileConfigSource;
import de.uni_passau.fim.seibt.kvconfig.sources.SysEnvConfigSource;

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

    /**
     * This prefix followed by one of the names of the variants in {@link KeyEnums.Type} can be used to define the
     * lookahead to be applied when encountering non-matching nodes of the given type.
     */
    public static final String LOOKAHEAD_PREFIX = "LAH_";

    /**
     * The singleton is implicitly synchronized because the <code>InstanceHolder</code> class is only initialized by
     * the classloader when the {@link #getConfig()} method is fist called.
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

    private Config config;

    /**
     * Private constructor to prevent outside instantiation.
     */
    private JDimeConfig() {
        config = new Config();
        config.addSource(new SysEnvConfigSource(1));
        loadConfigFile();
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
     * Optionally returns the lookahead defined in the <code>Config</code> for the given <code>type</code> of node.
     *
     * @param type
     *         the <code>type</code> of node to get the lookahead for
     * @return optionally the lookahead
     */
    public static Optional<Integer> getLookahead(KeyEnums.Type type) {
        return getConfig().getInteger(LOOKAHEAD_PREFIX + type.name()); //TODO cache this?
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
