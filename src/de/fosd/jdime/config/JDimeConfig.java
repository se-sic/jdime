/**
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2017 University of Passau, Germany
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
package de.fosd.jdime.config;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.fosd.jdime.Main;
import de.fosd.jdime.artifact.file.FileArtifact;
import de.fosd.jdime.matcher.ordered.mceSubtree.MCESubtreeMatcher;
import de.fosd.jdime.stats.KeyEnums;
import de.fosd.jdime.stats.MergeScenarioStatistics;
import de.fosd.jdime.stats.Statistics;
import de.uni_passau.fim.seibt.kvconfig.Config;
import de.uni_passau.fim.seibt.kvconfig.sources.PropFileConfigSource;
import de.uni_passau.fim.seibt.kvconfig.sources.SysEnvConfigSource;
import org.apache.commons.cli.ParseException;

import static de.fosd.jdime.config.CommandLineConfigSource.CLI_LOG_LEVEL;
import static de.fosd.jdime.config.CommandLineConfigSource.CLI_PROP_FILE;

/**
 * Contains the singleton <code>Config</code> instance containing the configuration options for JDime. All
 * keys used for retrieving config options should be declared as static final <code>String</code>s in this class.
 */
public final class JDimeConfig extends Config {

    private static final Logger LOG = Logger.getLogger(JDimeConfig.class.getCanonicalName());

    /**
     * The file name of the JDime configuration file.
     */
    private static final String CONFIG_FILE_NAME = "JDime.properties";

    /**
     * Whether to filter out any <code>FileArtifact</code>s not representing java source code files or directories
     * (possibly indirectly) containing such files before merging. Defaults to true.
     */
    public static final String FILTER_INPUT_DIRECTORIES = "FILTER_INPUT_DIRECTORIES";

    /**
     * Whether to fall back to a two way merge if three inputs are given but the base {@link FileArtifact} does not
     * exist. Defaults to false.
     */
    public static final String TWOWAY_FALLBACK = "TWOWAY_FALLBACK";

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
     * Whether to merge successive conflicts after running structured strategy.
     */
    public static final String OPTIMIZE_MULTI_CONFLICTS = "OPTIMIZE_MULTI_CONFLICTS";

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
     * A comma separated list of field names from the {@link MergeScenarioStatistics} class that should be excluded
     * when serializing the {@link Statistics}. The 'Statistics' word at the end of field names may be omitted.
     */
    public static final String STATISTICS_XML_EXCLUDE_MSS_FIELDS = "STATISTICS_XML_EXCLUDE_MSS_FIELDS";

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
     * The commit that was checked out at the time JDime was built. The build script will add this value to
     * JDime.properties.
     */
    public static final String JDIME_COMMIT = "JDIME_COMMIT";

    private CommandLineConfigSource cmdLine;

    /**
     * Constructs a new <code>JDimeConfig</code> that assumes no command line arguments were given.
     */
    public JDimeConfig() {
        try {
            addConfigSources(new String[] {});
        } catch (ParseException ignored) {
            // the ParseException will not be thrown for an empty arguments array
        }
    }

    /**
     * Constructs a new <code>JDimeConfig</code>. A <code>CommandLineConfigSource</code> will be added for the
     * given command line arguments. Furthermore a <code>PropFileConfigSource</code> will be added referencing
     * the file specified on the command line or the default properties file (if it exists). Lastly a
     * <code>SysEnvConfigSource</code> will also be added with the lowest priority of all. The constructor will also
     * set the log level using {@link #setLogLevel(String)}.
     *
     * @param args
     *         the command line arguments
     * @throws ParseException
     *         if there is an exception parsing the command line arguments
     */
    public JDimeConfig(String[] args) throws ParseException {
        addConfigSources(args);
    }

    /**
     * Adds the three <code>ConfigSource</code>s.
     *
     * @param args
     *         the command line arguments
     */
    private void addConfigSources(String[] args) throws ParseException {
        addSource(cmdLine = new CommandLineConfigSource(args, 3));
        get(CLI_LOG_LEVEL).ifPresent(JDimeConfig::setLogLevel);

        loadConfigFile(cmdLine.get(CLI_PROP_FILE).map(File::new).orElse(new File(CONFIG_FILE_NAME)));
        get(CLI_LOG_LEVEL).ifPresent(JDimeConfig::setLogLevel);

        addSource(new SysEnvConfigSource(1));
        get(CLI_LOG_LEVEL).ifPresent(JDimeConfig::setLogLevel);
    }

    /**
     * Checks whether the given file exists and if so adds a <code>PropFileConfigSource</code> for it.
     *
     * @param configFile
     *         the file to check
     */
    private void loadConfigFile(File configFile) {

        if (configFile.exists()) {

            try {
                addSource(new PropFileConfigSource(2, configFile));
            } catch (IOException e) {
                LOG.log(Level.WARNING, e, () -> "Could not add a ConfigSource for " + configFile.getAbsolutePath());
            }
        } else {
            LOG.log(Level.WARNING, () -> String.format("%s can not be used as a config file as it does not exist.", configFile));
        }
    }

    /**
     * Returns the <code>CommandLineConfigSource</code> used by this <code>JDimeConfig</code> to retrieve options
     * from the command line.
     *
     * @return the <code>ConfigSource</code>
     */
    public CommandLineConfigSource getCmdLine() {
        return cmdLine;
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
