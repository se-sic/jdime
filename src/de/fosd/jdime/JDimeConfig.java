package de.fosd.jdime;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.fosd.jdime.matcher.ordered.mceSubtree.MCESubtreeMatcher;
import de.uni_passau.fim.seibt.kvconfig.Config;
import de.uni_passau.fim.seibt.kvconfig.PropFileConfigSource;
import de.uni_passau.fim.seibt.kvconfig.SysEnvConfigSource;

/**
 * Contains the singleton <code>Config</code> instance containing the configuration options for JDime. All
 * keys used for retrieving config options should be declared as static final <code>String</code>s in this class.
 */
public final class JDimeConfig {

    private static final Logger LOG = Logger.getLogger(JDimeConfig.class.getCanonicalName());

    /**
     * The file name of the JDime configuration file.
     */
    public static final String CONFIG_FILE_NAME = "JDime.properties";

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
    public static final String DEFAULT_JDIME_EXECUTABLE = "JDIME_EXEC";

    /**
     * Whether to allow invalid values (such as non-existent files) for the text fields in the GUI.
     */
    public static final String ALLOW_INVALID = "ALLOW_INVALID";

    /**
     * How many lines of JDime output to buffer before adding them to the displayed lines in the GUI.
     */
    public static final String BUFFERED_LINES = "BUFFERED_LINES";

    /**
     * Whether to use the {@link MCESubtreeMatcher} when diffing.
     */
    public static final String USE_MCESUBTREE_MATCHER = "USE_MCESUBTREE_MATCHER";

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
}
