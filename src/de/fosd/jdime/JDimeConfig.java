package de.fosd.jdime;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    public static final String JDIME_CONF_FILE = "JDime.properties";
    public static final String JDIME_DEFAULT_ARGS_KEY = "DEFAULT_ARGS";
    public static final String JDIME_DEFAULT_LEFT_KEY = "DEFAULT_LEFT";
    public static final String JDIME_DEFAULT_BASE_KEY = "DEFAULT_BASE";
    public static final String JDIME_DEFAULT_RIGHT_KEY = "DEFAULT_RIGHT";
    public static final String JDIME_EXEC_KEY = "JDIME_EXEC";
    public static final String JDIME_ALLOW_INVALID_KEY = "ALLOW_INVALID";
    public static final String JDIME_BUFFERED_LINES = "BUFFERED_LINES";
    public static final String USE_MCESUBTREE_MATCHER = "USE_MCESUBTREE_MATCHER";

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
     * Checks whether the current working directory contains a file called {@value #JDIME_CONF_FILE} and if so adds
     * a <code>PropFileConfigSource</code> to <code>config</code>.
     */
    private void loadConfigFile() {
        File configFile = new File(JDIME_CONF_FILE);

        if (configFile.exists()) {

            try {
                config.addSource(new PropFileConfigSource(2, configFile));
            } catch (IOException e) {
                LOG.log(Level.WARNING, e, () -> "Could not add a ConfigSource for " + configFile.getAbsolutePath());
            }
        }
    }
}
