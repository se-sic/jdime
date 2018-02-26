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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import de.fosd.jdime.matcher.cost_model.CMMode;
import de.fosd.jdime.strategy.MergeStrategy;
import de.fosd.jdime.strdump.DumpMode;
import de.uni_passau.fim.seibt.kvconfig.sources.ConfigSource;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * A <code>ConfigSource</code> backed by a <code>CommandLine</code> instance. Its {@link #getMapping(String)} method
 * will (for both long and short option names) return the first argument to the option if it is set and has arguments
 * or "true" for options that are set but have no arguments. Otherwise an empty Optional is returned.
 * The left over arguments on the command line ({@link CommandLine#getArgList()}) can be retrieved using the key
 * {@link #ARG_LIST}.
 */
public class CommandLineConfigSource extends ConfigSource {

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
    public static final String CLI_INSPECT_ELEMENT = "ie";
    public static final String CLI_INSPECT_METHOD = "im";
    public static final String CLI_MODE = "m";
    public static final String CLI_DUMP = "dmp";
    public static final String CLI_OUTPUT = "o";
    public static final String CLI_OPTIMIZE_MULTI_CONFLICTS = "omc";
    public static final String CLI_RECURSIVE = "r";
    public static final String CLI_STATS = "s";
    public static final String CLI_PRETEND = "p";
    public static final String CLI_QUIET = "q";
    public static final String CLI_VERSION = "v";
    public static final String CLI_PROP_FILE = "pf";
    public static final String CLI_EXIT_ON_ERROR = "eoe";
    public static final String CLI_CM = "cm";
    public static final String CLI_CM_REMATCH_BOUND = "cmbound";
    public static final String CLI_CM_OPTIONS = "cmopts";
    public static final String CLI_CM_PARALLEL = "cmpar";
    public static final String CLI_CM_FIX_PERCENTAGE = "cmfix";
    public static final String CLI_CM_SEED = "cmseed";

    public static final String ARG_LIST = "ARG_LIST";
    public static final String ARG_LIST_SEP = ",";

    private Options options;
    private CommandLine cmdLine;

    /**
     * Constructs a new <code>CommandLineConfigSource</code> from the given <code>args</code>.
     *
     * @param args
     *         the command line arguments to parse
     * @throws ParseException
     *         if there is an exception parsing the arguments
     */
    public CommandLineConfigSource(String[] args) throws ParseException {
        this(args, DEFAULT_PRIORITY);
    }

    /**
     * Constructs a new <code>CommandLineConfigSource</code> from the given <code>args</code>.
     *
     * @param args
     *         the command line arguments to parse
     * @param priority
     *         the priority for this <code>ConfigSource</code>
     * @throws ParseException
     *         if there is an exception parsing the arguments
     */
    public CommandLineConfigSource(String[] args, int priority) throws ParseException {
        super(priority, null, null);

        this.options = buildCliOptions();
        this.cmdLine = new DefaultParser().parse(options, args);
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
                .longOpt("keep-going")
                .desc("Whether to skip a set of files if there is an exception merging them.")
                .hasArg(false)
                .build();

        options.addOption(o);

        o = Option.builder(CLI_LOOKAHEAD)
                .longOpt("lookahead")
                .desc("Use heuristics for matching. Supply 'off', 'full', or a non-negative integer as the argument.")
                .hasArg()
                .argName("level")
                .build();

        options.addOption(o);

        o = Option.builder(CLI_INSPECT_ELEMENT)
                .longOpt("inspect-element")
                .desc("Inspect an AST element. Supply number of element.")
                .hasArg()
                .argName("element")
                .build();

        options.addOption(o);

        o = Option.builder(CLI_INSPECT_METHOD)
                .longOpt("inspect-method")
                .desc("Inspect the method of an AST element. Supply number of element.")
                .hasArg()
                .argName("element")
                .build();

        options.addOption(o);

        {
            String strategies = String.join(", ", MergeStrategy.listStrategies());

            o = Option.builder(CLI_MODE)
                            .longOpt("mode")
                            .desc("Set the mode to one of (" + strategies + ") or a comma separated combination " +
                                    "thereof. In the latter case the strategies will be executed in order until one " +
                                    "does not produce conflicts.")
                            .hasArg()
                            .argName("mode")
                            .build();

            options.addOption(o);
        }

        {
            String formats = Arrays.stream(DumpMode.values()).map(DumpMode::name).reduce("", (s, s2) -> s + " " + s2);

            o = Option.builder(CLI_DUMP)
                    .longOpt("dump")
                    .desc("Dumps the inputs using one of the formats: " + formats)
                    .hasArg()
                    .argName("format")
                    .build();

            options.addOption(o);
        }

        o = Option.builder(CLI_OUTPUT)
                .longOpt("output")
                .desc("Set the output directory/file.")
                .hasArg()
                .argName("file")
                .build();

        options.addOption(o);

        o = Option.builder(CLI_OPTIMIZE_MULTI_CONFLICTS)
                .longOpt("optimize-multi-conflicts")
                .desc("Merge successive conflicts after running structured strategy.")
                .hasArg(false)
                .build();

        options.addOption(o);

        o = Option.builder(CLI_RECURSIVE)
                .longOpt("recursive")
                .desc("Merge directories recursively.")
                .hasArg(false)
                .build();

        options.addOption(o);

        o = Option.builder(CLI_STATS)
                .longOpt("stats")
                .desc("Collect statistical data about the merge.")
                .hasArg(false)
                .build();

        options.addOption(o);

        o = Option.builder(CLI_PRETEND)
                .longOpt("pretend")
                .desc("Prints the merge result to stdout instead of an output file.")
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

        o = Option.builder(CLI_PROP_FILE)
                .longOpt("properties-file")
                .desc("Set the path to the properties file to use for additional configuration options.")
                .hasArg()
                .argName("path")
                .build();

        options.addOption(o);

        o = Option.builder(CLI_EXIT_ON_ERROR)
                .longOpt("exit-on-error")
                .desc("Whether to end the merge if there is an exception merging a set of files. If neither this " +
                        "option nor keep-going is set the fallback line based strategy will be tried.")
                .hasArg(false)
                .build();

        options.addOption(o);

        {
            String modes = Arrays.stream(CMMode.values()).map(CMMode::name).reduce("", (s, s2) -> s + " " + s2);

            o = Option.builder(CLI_CM)
                            .longOpt("cost-model-matcher")
                            .desc("Sets the cost model matcher operation mode to one of " + modes)
                            .hasArg(true)
                            .build();

            options.addOption(o);
        }

        o = Option.builder(CLI_CM_REMATCH_BOUND)
                .longOpt("cost-model-rematch-bound")
                .desc("If the cost model matcher operation mode is " + CMMode.INTEGRATED + " the cost model matcher will " +
                        "be used to try and improve subtree matches with a percentage lower than this bound. " +
                        "Should be from (0, 1]. The default is 30%.")
                .hasArg(true)
                .build();

        options.addOption(o);

        o = Option.builder(CLI_CM_OPTIONS)
                .longOpt("cost-model-options")
                .desc("Accepts a comma separated list of parameters for the cost model matcher. The list must have " +
                        "the form: <int iterations>,<float pAssign>,<float wr>,<float wn>,<float wa>,<float ws>,<float wo>")
                .hasArg(true)
                .build();

        options.addOption(o);

        o = Option.builder(CLI_CM_PARALLEL)
                .longOpt("cost-model-parallel")
                .desc("Whether to speed up the cost model matcher by calculating the edge costs in parallel.")
                .hasArg(false)
                .build();

        options.addOption(o);

        o = Option.builder(CLI_CM_FIX_PERCENTAGE)
                .longOpt("cost-model-fix-percentage")
                .desc("Accepts a comma separated list of two percentages. <float fixLower>,<float fixUpper> both " +
                        "from the range [0, 1]. If these percentages are given, a random number (from the given range) " +
                        "of matchings from the previous iteration will be fixed for the next.")
                .hasArg(true)
                .build();

        options.addOption(o);

        o = Option.builder(CLI_CM_SEED)
                .longOpt("cost-model-seed")
                .desc("The seed for the PRNG used by the cost model matcher. If set to \"none\" a random seed will " +
                        "be used. Otherwise the default is 42.")
                .hasArg(true)
                .build();

        options.addOption(o);


        return options;
    }

    /**
     * Returns the <code>Options</code> describing the command line options.
     *
     * @return the <code>Options</code>
     */
    public Options getOptions() {
        return options;
    }

    @Override
    protected Optional<String> getMapping(String key) {

        if (ARG_LIST.equals(key)) {
            List<String> argList = cmdLine.getArgList();

            if (argList.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(String.join(ARG_LIST_SEP, argList));
            }
        }

        if (!options.hasOption(key)) {
            return Optional.empty();
        }

        Option opt = options.getOption(key);
        String optName = opt.getOpt();

        if (opt.hasArg()) {
            return Optional.ofNullable(cmdLine.getOptionValue(optName));
        } else {
            return Optional.of(cmdLine.hasOption(optName) ? "true" : "false");
        }
    }
}
