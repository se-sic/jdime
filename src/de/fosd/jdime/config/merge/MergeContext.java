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
package de.fosd.jdime.config.merge;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.artifact.ArtifactList;
import de.fosd.jdime.artifact.ast.ASTNodeArtifact;
import de.fosd.jdime.artifact.file.FileArtifact;
import de.fosd.jdime.config.CommandLineConfigSource;
import de.fosd.jdime.config.JDimeConfig;
import de.fosd.jdime.execption.AbortException;
import de.fosd.jdime.matcher.cost_model.CMMode;
import de.fosd.jdime.matcher.cost_model.CostModelMatcher;
import de.fosd.jdime.stats.KeyEnums;
import de.fosd.jdime.stats.MergeScenarioStatistics;
import de.fosd.jdime.stats.Statistics;
import de.fosd.jdime.strategy.LinebasedStrategy;
import de.fosd.jdime.strategy.MergeStrategy;
import de.fosd.jdime.strategy.NWayStrategy;
import de.fosd.jdime.strategy.StructuredStrategy;
import de.fosd.jdime.strdump.DumpMode;

import static de.fosd.jdime.config.CommandLineConfigSource.CLI_CM;
import static de.fosd.jdime.config.CommandLineConfigSource.CLI_CM_FIX_PERCENTAGE;
import static de.fosd.jdime.config.CommandLineConfigSource.CLI_CM_OPTIONS;
import static de.fosd.jdime.config.CommandLineConfigSource.CLI_CM_PARALLEL;
import static de.fosd.jdime.config.CommandLineConfigSource.CLI_CM_REMATCH_BOUND;
import static de.fosd.jdime.config.CommandLineConfigSource.CLI_CM_SEED;
import static de.fosd.jdime.config.CommandLineConfigSource.CLI_CONSECUTIVE;
import static de.fosd.jdime.config.CommandLineConfigSource.CLI_DIFFONLY;
import static de.fosd.jdime.config.CommandLineConfigSource.CLI_DUMP;
import static de.fosd.jdime.config.CommandLineConfigSource.CLI_EXIT_ON_ERROR;
import static de.fosd.jdime.config.CommandLineConfigSource.CLI_FORCE_OVERWRITE;
import static de.fosd.jdime.config.CommandLineConfigSource.CLI_INSPECT_ELEMENT;
import static de.fosd.jdime.config.CommandLineConfigSource.CLI_INSPECT_METHOD;
import static de.fosd.jdime.config.CommandLineConfigSource.CLI_KEEPGOING;
import static de.fosd.jdime.config.CommandLineConfigSource.CLI_LOOKAHEAD;
import static de.fosd.jdime.config.CommandLineConfigSource.CLI_MODE;
import static de.fosd.jdime.config.CommandLineConfigSource.CLI_OUTPUT;
import static de.fosd.jdime.config.CommandLineConfigSource.CLI_PRETEND;
import static de.fosd.jdime.config.CommandLineConfigSource.CLI_QUIET;
import static de.fosd.jdime.config.CommandLineConfigSource.CLI_RECURSIVE;
import static de.fosd.jdime.config.CommandLineConfigSource.CLI_STATS;
import static de.fosd.jdime.config.JDimeConfig.FILTER_INPUT_DIRECTORIES;
import static de.fosd.jdime.config.JDimeConfig.STATISTICS_XML_EXCLUDE_MSS_FIELDS;
import static de.fosd.jdime.config.JDimeConfig.TWOWAY_FALLBACK;
import static de.fosd.jdime.config.JDimeConfig.USE_MCESUBTREE_MATCHER;
import static java.util.logging.Level.WARNING;

/**
 * @author Olaf Lessenich
 */
public class MergeContext implements Cloneable {

    private static final Logger LOG = Logger.getLogger(MergeContext.class.getCanonicalName());

    /**
     * Do look at all nodes in the subtree even if the compared nodes are not
     * equal.
     */
    public static final int LOOKAHEAD_FULL = Integer.MAX_VALUE;

    /**
     * Stop looking for subtree matches if the two nodes compared are not equal.
     */
    public static final int LOOKAHEAD_OFF = 0;

    /**
     * The default git command.
     */
    public static final String DEFAULT_GIT_CMD = "git";

    /**
     * Whether merge inserts choice nodes instead of direct merging.
     */
    private boolean conditionalMerge;

    /**
     * Whether conditional merge should be performed outside of methods.
     */
    private boolean conditionalOutsideMethods;

    /**
     * Whether to run only the diff.
     */
    private boolean diffOnly;

    /**
     * Whether to treat two input versions as consecutive versions in the revision history.
     */
    private boolean consecutive;

    /**
     * If set the input <code>Artifact</code>s will be dumped in the given format instead of merging.
     */
    private DumpMode dumpMode;

    /**
     * The number of an artifact that should be inspected. If this is set, no merge will be executed.
     */
    private int inspectArtifact;

    /**
     * The scope of inspection.
     */
    private KeyEnums.Type inspectionScope;

    /**
     * Force overwriting of existing output files.
     */
    private boolean forceOverwriting;

    /**
     * Input Files.
     */
    private List<FileArtifact> inputFiles;

    /**
     * If true and the input files are directories, any files not representing java source code files or directories
     * (possibly indirectly) containing such files will be removed from the input FileArtifact trees.
     */
    private boolean filterInputDirectories;

    /**
     * If true, merging will continue (skipping the failed files) after exceptions if exit-on-error is not set.
     */
    private boolean keepGoing;

    /**
     * If true, merge will be aborted if there is an exception merging files.
     */
    private boolean exitOnError;

    /**
     * Strategy to apply for the merge.
     */
    private MergeStrategy<FileArtifact> mergeStrategy;

    /**
     * Output file.
     */
    private FileArtifact outputFile;

    /**
     * If true, the output is quiet.
     */
    private boolean quiet;

    /**
     * If true, output is not written to an output file.
     */
    private boolean pretend;

    /**
     * Merge directories recursively. Can be set with the '-r' argument.
     */
    private boolean recursive;

    /**
     * Whether to collect statistics after merging.
     */
    private boolean collectStatistics;
    private Statistics statistics;

    private List<Field> excludeStatisticsMSSFields;

    /**
     * Whether to use the <code>MCESubtreeMatcher</code> in the matching phase of the merge.
     */
    private boolean useMCESubtreeMatcher;

    /**
     * Whether {@link StructuredStrategy} act semi-structured, that is whether it should perform line based merging
     * on the configured {@link #semiStructuredLevel}.
     */
    private boolean semiStructured;
    private KeyEnums.Level semiStructuredLevel;

    /**
     * How many levels to keep searching for matches in the subtree if the
     * currently compared nodes are not equal. If there are no matches within
     * the specified number of levels, do not look for matches deeper in the
     * subtree. If this is set to LOOKAHEAD_OFF, the matcher will stop looking
     * for subtree matches if two nodes do not match. If this is set to
     * LOOKAHEAD_FULL, the matcher will look at the entire subtree.
     * The default ist to do no look-ahead matching.
     */
    private int lookAhead;
    private Map<KeyEnums.Type, Integer> lookAheads;

    private Map<MergeScenario<?>, Throwable> crashes;

    private CMMode cmMatcherMode;
    private float cmReMatchBound;
    private float wr, wn, wa, ws, wo;
    private float pAssign;
    private float fixLower, fixUpper;
    private Optional<Long> seed;
    private int costModelIterations;
    private boolean cmMatcherParallel;
    private boolean cmMatcherFixRandomPercentage;

    /**
     * Constructs a new <code>MergeContext</code> initializing all options to their default values.
     */
    public MergeContext() {
        this.conditionalMerge = false;
        this.conditionalOutsideMethods = true;
        this.diffOnly = false;
        this.consecutive = false;
        this.dumpMode = DumpMode.NONE;
        this.forceOverwriting = false;
        this.inputFiles = new ArtifactList<>();
        this.filterInputDirectories = true;
        this.keepGoing = false;
        this.exitOnError = false;
        this.mergeStrategy = new LinebasedStrategy();
        this.outputFile = null;
        this.quiet = false;
        this.pretend = true;
        this.recursive = false;
        this.collectStatistics = false;
        this.statistics = new Statistics();
        this.excludeStatisticsMSSFields = new ArrayList<>();
        this.useMCESubtreeMatcher = false;
        this.semiStructured = false;
        this.semiStructuredLevel = KeyEnums.Level.METHOD;
        this.lookAhead = MergeContext.LOOKAHEAD_OFF;
        this.lookAheads = new HashMap<>();
        this.crashes = new HashMap<>();
        this.cmMatcherMode = CMMode.OFF;
        this.cmReMatchBound = .3f;
        this.wr = 1;
        this.wn = 1;
        this.wa = 1;
        this.ws = 1;
        this.wo = 1;
        this.pAssign = .7f;
        this.fixLower = .25f;
        this.fixUpper = .50f;
        this.seed = Optional.of(42L);
        this.costModelIterations = 100;
        this.cmMatcherParallel = true;
        this.cmMatcherFixRandomPercentage = true;
    }

    /**
     * Copy constructor.
     *
     * @param toCopy
     *         the <code>MergeContext</code> to copy
     */
    public MergeContext(MergeContext toCopy) {
        this.conditionalMerge = toCopy.conditionalMerge;
        this.conditionalOutsideMethods = toCopy.conditionalOutsideMethods;
        this.diffOnly = toCopy.diffOnly;
        this.consecutive = toCopy.consecutive;
        this.dumpMode = toCopy.dumpMode;
        this.inspectArtifact = toCopy.inspectArtifact;
        this.inspectionScope = toCopy.inspectionScope;
        this.forceOverwriting = toCopy.forceOverwriting;

        this.inputFiles = new ArtifactList<>();
        this.inputFiles.addAll(toCopy.inputFiles.stream().map(FileArtifact::copy).collect(Collectors.toList()));

        this.filterInputDirectories = toCopy.filterInputDirectories;
        this.keepGoing = toCopy.keepGoing;
        this.exitOnError = toCopy.exitOnError;
        this.mergeStrategy = toCopy.mergeStrategy; // MergeStrategy should be stateless
        this.outputFile = toCopy.outputFile.copy();
        this.quiet = toCopy.quiet;
        this.pretend = toCopy.pretend;
        this.recursive = toCopy.recursive;
        this.collectStatistics = toCopy.collectStatistics;
        this.statistics = new Statistics(toCopy.statistics);
        this.excludeStatisticsMSSFields = new ArrayList<>(toCopy.excludeStatisticsMSSFields);
        this.useMCESubtreeMatcher = toCopy.useMCESubtreeMatcher;
        this.semiStructured = toCopy.semiStructured;
        this.semiStructuredLevel = toCopy.semiStructuredLevel;

        this.lookAhead = toCopy.lookAhead;
        this.lookAheads = new HashMap<>(toCopy.lookAheads);

        this.crashes = new HashMap<>(toCopy.crashes);
        this.cmMatcherMode = toCopy.cmMatcherMode;
        this.cmReMatchBound = toCopy.cmReMatchBound;
        this.wr = toCopy.wr;
        this.wn = toCopy.wn;
        this.wa = toCopy.wa;
        this.ws = toCopy.ws;
        this.wo = toCopy.wo;
        this.pAssign = toCopy.pAssign;
        this.fixLower = toCopy.fixLower;
        this.fixUpper = toCopy.fixUpper;
        this.seed = toCopy.seed;
        this.costModelIterations = toCopy.costModelIterations;
        this.cmMatcherParallel = toCopy.cmMatcherParallel;
        this.cmMatcherFixRandomPercentage = toCopy.cmMatcherFixRandomPercentage;
    }

    /**
     * Initializes the configuration options stored in the <code>MergeContext</code> from the given
     * <code>JDimeConfig</code>.
     *
     * @param config
     *         the <code>JDimeConfig</code> to query for config values
     */
    public void configureFrom(JDimeConfig config) {
        configDump(config);
        configInspect(config);
        configStatistics(config);
        configMerge(config);
        configErrorHandling(config);
        configInputOutput(config);
    }

    /**
     * Reads configuration options related to the {@link Artifact} inspection functionality.
     *
     * @param config
     *         the JDime configuration options
     */
    private void configInspect(JDimeConfig config) {
        config.getInteger(CLI_INSPECT_ELEMENT).ifPresent(elId -> {
            setInspectArtifact(elId);
            setInspectionScope(KeyEnums.Type.NODE);
        });

        config.getInteger(CLI_INSPECT_METHOD).ifPresent(mId -> {
            setInspectArtifact(mId);
            setInspectionScope(KeyEnums.Type.METHOD);
        });
    }

    /**
     * Reads configuration options related to the {@link Artifact} dump functionality.
     *
     * @param config
     *         the JDime configuration options
     */
    private void configDump(JDimeConfig config) {
        Function<String, Optional<DumpMode>> dmpModeParser = mode -> {

            try {
                return Optional.of(DumpMode.valueOf(mode.toUpperCase()));
            } catch (IllegalArgumentException e) {
                LOG.log(Level.WARNING, e, () -> "Invalid dump format " + mode);
                return Optional.empty();
            }
        };

        config.get(CLI_DUMP, dmpModeParser).ifPresent(this::setDumpMode);
    }

    /**
     * Reads configuration options related to the {@link Statistics}.
     *
     * @param config
     *         the JDime configuration options
     */
    private void configStatistics(JDimeConfig config) {
        config.getBoolean(CLI_STATS).ifPresent(this::collectStatistics);
        config.get(STATISTICS_XML_EXCLUDE_MSS_FIELDS, list -> {
            List<String> toExclude = Arrays.asList(list.split("\\s*,\\s*"));

            if (!toExclude.isEmpty()) {
                List<Field> fields = new ArrayList<>(toExclude.size());
                List<Field> mssFields = Arrays.asList(MergeScenarioStatistics.class.getDeclaredFields());

                for (String excludeName : toExclude) {
                    Predicate<Field> nameMatches = f -> excludeName.equals(f.getName()) ||
                            excludeName.equals(f.getName().replaceAll("Statistics$", ""));

                    mssFields.stream().filter(nameMatches).findFirst().ifPresent(fields::add);
                }

                return Optional.of(fields);
            } else {
                return Optional.empty();
            }
        }).ifPresent(list -> this.excludeStatisticsMSSFields = list);
    }

    /**
     * Reads configuration options determining how the merge is to be executed.
     *
     * @param config
     *         the JDime configuration options
     */
    private void configMerge(JDimeConfig config) {
        {
            Optional<String> oMode = config.get(CLI_MODE);

            if (oMode.isPresent()) {
                String mode = oMode.get();
                setMergeStrategy(MergeStrategy.parse(mode).orElseThrow(
                        () -> new AbortException("Invalid mode '" + mode + "'."))
                );
            } else if (!(getDumpMode() != DumpMode.NONE || isInspect())) {
                throw new AbortException("No mode given.");
            }
        }

        config.getBoolean(CLI_DIFFONLY).ifPresent(diffOnly -> {
            setDiffOnly(diffOnly);
            config.getBoolean(CLI_CONSECUTIVE).ifPresent(this::setConsecutive);
        });

        config.getBoolean(USE_MCESUBTREE_MATCHER).ifPresent(this::setUseMCESubtreeMatcher);

        config.get(CLI_LOOKAHEAD, val -> {
            String msg = "Invalid lookahead level '" + val + "'. Must be one of 'off', 'full' or a non-negative integer.";
            RuntimeException abort = new AbortException(msg);

            int lah;

            try {
                lah = Integer.parseInt(val);
            } catch (NumberFormatException e) {
                String lcVal = val.trim().toLowerCase();

                if ("off".equals(lcVal)) {
                    lah = MergeContext.LOOKAHEAD_OFF;
                } else if ("full".equals(lcVal)) {
                    lah = MergeContext.LOOKAHEAD_FULL;
                } else {
                    throw abort;
                }
            }

            if (lah < 0) {
                throw abort;
            }

            return Optional.of(lah);
        }).ifPresent(this::setLookAhead);

        for (KeyEnums.Type type : KeyEnums.Type.values()) {
            config.getInteger(JDimeConfig.LOOKAHEAD_PREFIX + type.name()).ifPresent(val -> setLookAhead(type, val));
        }

        configCostModelMatcher(config);
    }

    /**
     * Reads configuration options determining how to handle errors while merging.
     *
     * @param config
     *         the JDime configuration options
     */
    private void configErrorHandling(JDimeConfig config) {
        config.getBoolean(CLI_KEEPGOING).ifPresent(this::setKeepGoing);
        config.getBoolean(CLI_EXIT_ON_ERROR).ifPresent(this::setExitOnError);
    }

    /**
     * Reads configuration options determining how the {@link CostModelMatcher} operates.
     *
     * @param config
     *         the JDime configuration options
     */
    private void configCostModelMatcher(JDimeConfig config) {
        config.get(CLI_CM, mode -> {

            try {
                return Optional.of(CMMode.valueOf(mode.toUpperCase()));
            } catch (IllegalArgumentException e) {
                LOG.log(WARNING, e, () -> "Invalid CostModelMatcher mode " + mode);
                return Optional.empty();
            }
        }).ifPresent(this::setCmMatcherMode);

        config.getFloat(CLI_CM_REMATCH_BOUND).ifPresent(this::setCmReMatchBound);

        config.get(CLI_CM_OPTIONS).ifPresent(opts -> {
            String[] split = opts.trim().split("\\s*,\\s*");

            if (split.length != 7) {
                LOG.warning(() -> "The cost model options have an invalid format. Using defaults.");
                return;
            }

            int costModelIterations;
            float pAssign, wr, wn, wa, ws, wo;

            try {
                costModelIterations = Integer.parseInt(split[0]);
                pAssign = Float.parseFloat(split[1]);
                wr = Float.parseFloat(split[2]);
                wn = Float.parseFloat(split[3]);
                wa = Float.parseFloat(split[4]);
                ws = Float.parseFloat(split[5]);
                wo = Float.parseFloat(split[6]);
            } catch (NumberFormatException e) {
                LOG.log(WARNING, e, () -> "The cost model options have an invalid format. Using defaults.");
                return;
            }

            setCostModelIterations(costModelIterations);
            setpAssign(pAssign);
            setWr(wr);
            setWn(wn);
            setWa(wa);
            setWs(ws);
            setWo(wo);
        });

        config.getBoolean(CLI_CM_PARALLEL).ifPresent(this::setCmMatcherParallel);

        config.get(CLI_CM_FIX_PERCENTAGE).ifPresent(opts -> {
            String[] split = opts.trim().split("\\s*,\\s*");

            if (split.length != 2) {
                LOG.warning(() -> "The cost model fix percentages have an invalid format.");
                return;
            }

            float fixLower, fixUpper;

            try {
                fixLower = Float.parseFloat(split[0]);
                fixUpper = Float.parseFloat(split[1]);
            } catch (NumberFormatException e) {
                LOG.log(WARNING, e, () -> "The cost model fix percentages have an invalid format.");
                return;
            }

            setCmMatcherFixRandomPercentage(true);
            setFixLower(fixLower);
            setFixUpper(fixUpper);
        });

        config.get(CLI_CM_SEED).ifPresent(opt -> {

            if ("none".equals(opt.trim().toLowerCase())) {
                setSeed(Optional.empty());
            } else {
                try {
                    setSeed(Optional.of(Long.parseLong(opt)));
                } catch (NumberFormatException e) {
                    LOG.log(WARNING, e, () -> "The cost model seed has an invalid format. Using the default.");
                }
            }
        });
    }

    /**
     * Reads configuration options determining how to output the merge results and reads the additional arguments
     * to configure the input files for the merge.
     *
     * @param config
     *         the JDime configuration options
     */
    private void configInputOutput(JDimeConfig config) {
        config.getBoolean(FILTER_INPUT_DIRECTORIES).ifPresent(this::setFilterInputDirectories);
        config.getBoolean(CLI_FORCE_OVERWRITE).ifPresent(this::setForceOverwriting);
        config.getBoolean(CLI_RECURSIVE).ifPresent(this::setRecursive);

        config.getBoolean(CLI_PRETEND).ifPresent(this::setPretend);
        config.getBoolean(CLI_QUIET).ifPresent(this::setQuiet);

        Optional<String> args = config.get(CommandLineConfigSource.ARG_LIST);

        if (args.isPresent()) {
            List<File> inputFiles = Arrays.stream(args.get().split(CommandLineConfigSource.ARG_LIST_SEP))
                                          .map(String::trim).map(File::new).collect(Collectors.toCollection(ArrayList::new));
            List<File> nonExistent = inputFiles.stream().filter(f -> !f.exists()).collect(Collectors.toList());

            Boolean twFallback = config.getBoolean(TWOWAY_FALLBACK).orElse(false);

            if (!nonExistent.isEmpty()) {
                if (twFallback && inputFiles.size() == MergeType.THREEWAY_FILES && nonExistent.size() == 1
                        && inputFiles.get(1) == nonExistent.get(0)) {

                    File nonExistentBase = inputFiles.get(1);
                    LOG.warning(() -> "Base input file " + nonExistentBase + " does not exist. Falling back to two way merge.");

                    inputFiles.remove(nonExistentBase);
                } else {
                    nonExistent.forEach(f -> LOG.severe(() -> "Input file " + f + " does not exist."));
                    throw new AbortException("All input files must exist.");
                }
            }

            boolean allDirs = inputFiles.stream().allMatch(File::isDirectory);
            boolean allFiles = inputFiles.stream().allMatch(File::isFile);

            if (!(allDirs || allFiles)) {
                LOG.severe(() -> "Inconsistent input files. (Must all be all directories or all files.)");
                inputFiles.forEach(f -> LOG.severe(f::getAbsolutePath));
                throw new AbortException("Input files must be all directories or all files.");
            }

            Supplier<Revision> revSupplier;

            if (isConditionalMerge()) {
                revSupplier = new Revision.SuccessiveRevSupplier();
            } else {

                if (inputFiles.size() == MergeType.TWOWAY_FILES) {
                    revSupplier = Arrays.asList(MergeScenario.LEFT, MergeScenario.RIGHT).iterator()::next;
                } else if (inputFiles.size() == MergeType.THREEWAY_FILES) {
                    revSupplier = Arrays.asList(MergeScenario.LEFT, MergeScenario.BASE, MergeScenario.RIGHT).iterator()::next;
                } else {
                    revSupplier = new Revision.SuccessiveRevSupplier();
                }
            }

            List<FileArtifact> inputArtifacts = new ArtifactList<>(inputFiles.size());

            for (File file : inputFiles) {
                FileArtifact artifact = new FileArtifact(revSupplier.get(), file);
                inputArtifacts.add(artifact);
            }

            if (allFiles && !inputArtifacts.stream().allMatch(FileArtifact::isJavaFile)) {
                LOG.severe(() -> "Invalid input files. (Must all be java source code files.)");
                inputFiles.forEach(f -> LOG.severe(f::getAbsolutePath));
                throw new AbortException("All input files must be Java source code files.");
            }

            setInputFiles(inputArtifacts);
        } else {
            throw new AbortException("No input files given.");
        }

        boolean inputIsDirs = getInputFiles().stream().allMatch(FileArtifact::isDirectory);
        boolean inputIsFiles = getInputFiles().stream().allMatch(FileArtifact::isFile);

        FileArtifact.FileType type;

        if (inputIsDirs) {
            type = FileArtifact.FileType.DIR;
        } else if (inputIsFiles) {
            type = FileArtifact.FileType.FILE;
        } else { // This is prevented by a check above.
            type = null;
        }

        if (isPretend()) {
            setOutputFile(new FileArtifact(MergeScenario.MERGE, type));
        } else {
            Optional<File> oFile = config.get(CLI_OUTPUT).map(String::trim).map(File::new);

            if (oFile.isPresent()) {
                File outFile = oFile.get();

                if (outFile.exists()) {

                    if (!isForceOverwriting()) {
                        String msg = String.format("The output file or directory exists. Use -%s to force overwriting.", CLI_FORCE_OVERWRITE);
                        throw new AbortException(msg);
                    }

                    if (inputIsDirs && !outFile.isDirectory()) {
                        throw new AbortException("The output must be a directory when merging directories.");
                    }

                    if (inputIsFiles && !outFile.isFile()) {
                        throw new AbortException("The output must be a file when merging files.");
                    }
                }

                setOutputFile(new FileArtifact(MergeScenario.MERGE, outFile, false));
            } else if (!(getDumpMode() != DumpMode.NONE || isInspect())) {
                throw new AbortException("Not output file or directory given.");
            }
        }
    }

    /**
     * Returns the input files for the merge.
     *
     * @return the input files
     */
    public List<FileArtifact> getInputFiles() {
        return inputFiles;
    }

    /**
     * Sets the input files to the new value.
     *
     * @param inputFiles
     *         the new input files
     */
    public void setInputFiles(List<FileArtifact> inputFiles) {
        this.inputFiles = inputFiles;
    }

    /**
     * Returns the merge strategy.
     *
     * @return the merge strategy
     */
    public MergeStrategy<FileArtifact> getMergeStrategy() {
        return mergeStrategy;
    }

    /**
     * Sets the merge strategy.
     *
     * @param mergeStrategy
     *         merge strategy
     */
    public void setMergeStrategy(MergeStrategy<FileArtifact> mergeStrategy) {
        this.mergeStrategy = mergeStrategy;

        if (mergeStrategy instanceof NWayStrategy) {
            setConditionalMerge(true);
        }
    }

    /**
     * Returns the output file for the merge.
     *
     * @return the outputFile
     */
    public FileArtifact getOutputFile() {
        return outputFile;
    }

    /**
     * Sets the output file to the new value.
     *
     * @param outputFile
     *         the new output file
     */
    public void setOutputFile(FileArtifact outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * Returns the <code>Statistics</code> object used to collect statistical data. This method <u>may</u> return
     * <code>null</code> if {@link #hasStatistics()} returns <code>false</code>.
     *
     * @return the <code>Statistics</code> object currently in use
     */
    public Statistics getStatistics() {
        return statistics;
    }

    /**
     * Returns whether statistical data should be collected using the <code>Statistics</code> object returned by
     * {@link #getStatistics()}.
     *
     * @return whether statistical data should be collected
     */
    public boolean hasStatistics() {
        return collectStatistics;
    }

    /**
     * Returns the list of {@link Field Fields} of the {@link MergeScenarioStatistics} class that are to be excluded
     * when serializing the {@link Statistics}.
     *
     * @return the list of {@link Field Fields} to be omitted
     */
    public List<Field> getExcludeStatisticsMSSFields() {
        return excludeStatisticsMSSFields;
    }

    /**
     * Returns whether to only perform the diff stage of the merge.
     *
     * @return true iff only the diff stage shall be performed
     */
    public boolean isDiffOnly() {
        return diffOnly;
    }

    /**
     * Sets whether to only perform the diff stage of the merge.
     *
     * @param diffOnly
     *         whether to diff only
     */
    public void setDiffOnly(boolean diffOnly) {
        this.diffOnly = diffOnly;
    }

    /**
     * Returns the <code>DumpMode</code> if it is set.
     *
     * @return the <code>DumpMode</code>
     */
    public DumpMode getDumpMode() {
        return dumpMode;
    }

    /**
     * Sets the <code>DumpMode</code> to the new value.
     *
     * @param dumpMode
     *         the new <code>DumpMode</code>
     */
    public void setDumpMode(DumpMode dumpMode) {
        this.dumpMode = dumpMode;
    }

    /**
     * Returns true if overwriting of files in the output directory is forced.
     *
     * @return whether overwriting of output files is forced
     */
    public boolean isForceOverwriting() {
        return forceOverwriting;
    }

    /**
     * Sets whether overwriting of files in the output directory is forced.
     *
     * @param forceOverwriting
     *         overwrite files in the output directory
     */
    public void setForceOverwriting(boolean forceOverwriting) {
        this.forceOverwriting = forceOverwriting;
    }

    /**
     * Whether to filter out any <code>FileArtifact</code>s not representing java source code files or directories
     * (possibly indirectly) containing such files before merging. Defaults to true.
     *
     * @return true iff the filter should be applied to input directories before merging
     */
    public boolean isFilterInputDirectories() {
        return filterInputDirectories;
    }

    /**
     * Whether to filter out any <code>FileArtifact</code>s not representing java source code files or directories
     * (possibly indirectly) containing such files before merging. Defaults to true.
     *
     * @param filterInputDirectories
     *         whether to applie the filter to input directories before merging
     */
    public void setFilterInputDirectories(boolean filterInputDirectories) {
        this.filterInputDirectories = filterInputDirectories;
    }

    /**
     * If true, merging will continue (skipping the failed files) after exceptions if exit-on-error is not set.
     *
     * @return true iff the merge should keep going
     */
    public boolean isKeepGoing() {
        return keepGoing;
    }

    /**
     * Sets whether to keep going to the new value.
     *
     * @param keepGoing
     *         whether to keep going
     */
    public void setKeepGoing(boolean keepGoing) {
        this.keepGoing = keepGoing;
    }

    /**
     * Gets whether to abort the merge if merging a set of files fails.
     *
     * @return whether to abort
     */
    public boolean isExitOnError() {
        return exitOnError;
    }

    /**
     * Sets whether to abort the merge if merging a set of files fails.
     *
     * @param exitOnError
     *         the new value
     */
    public void setExitOnError(boolean exitOnError) {
        this.exitOnError = exitOnError;
    }

    /**
     * Returns true if the output is quiet.
     *
     * @return if output is quiet
     */
    public boolean isQuiet() {
        return quiet;
    }

    /**
     * Sets whether the output is quiet or not.
     *
     * @param quiet
     *         do not print merge results to stdout
     */
    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    /**
     * Returns true if the merge is only simulated but not written to an output file.
     *
     * @return true, if the merge is only simulated but not written to an output file.
     */
    public boolean isPretend() {
        return pretend;
    }

    /**
     * Sets whether the merge is only simulated and not written to an output file.
     *
     * @param pretend
     *         do not write the merge result to an output file
     */
    public void setPretend(boolean pretend) {
        this.pretend = pretend;
    }

    /**
     * Returns whether directories are merged recursively.
     *
     * @return true, if directories are merged recursively
     */
    public boolean isRecursive() {
        return recursive;
    }

    /**
     * Set whether directories are merged recursively.
     *
     * @param recursive
     *         directories are merged recursively
     */
    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    /**
     * Sets whether statistical data should be collected during the next run using this <code>MergeContext</code>
     *
     * @param collectStatistics
     *         whether to collect statistical data
     */
    public void collectStatistics(boolean collectStatistics) {
        this.collectStatistics = collectStatistics;
    }

    /**
     * Whether to treat two input versions as consecutive versions in the revision history.
     *
     * @return true iff two input versions should be treated as consecutive
     */
    public boolean isConsecutive() {
        return consecutive;
    }

    /**
     * Sets whether to do consecutive merging.
     *
     * @param consecutive
     *         whether to do consecutive merging
     */
    public void setConsecutive(boolean consecutive) {
        this.consecutive = consecutive;
    }

    /**
     * Whether merge inserts choice nodes instead of direct merging.
     *
     * @return whether choice nodes should be inserted instead of direct merging
     */
    public boolean isConditionalMerge() {
        return conditionalMerge;
    }

    /**
     * Whether merge the given artifact inserts choice nodes instead of direct merging.
     *
     * @param artifact
     *         the artifact to check
     * @return true iff conditional merge is enabled for the given artifact
     */
    public boolean isConditionalMerge(Artifact<?> artifact) {
        return conditionalMerge && (conditionalOutsideMethods || artifact instanceof ASTNodeArtifact && (
                (ASTNodeArtifact) artifact).isWithinMethod());
    }

    /**
     * Sets whether to insert choice nodes instead of direct merging where appropriate.
     *
     * @param conditionalMerge
     *         the new value
     */
    public void setConditionalMerge(boolean conditionalMerge) {
        this.conditionalMerge = conditionalMerge;
    }

    /**
     * Returns how many levels to keep searching for matches in the subtree if
     * the currently compared nodes are not equal. If there are no matches
     * within the specified number of levels, do not look for matches deeper in
     * the subtree. If this is set to LOOKAHEAD_OFF, the matcher will stop
     * looking for subtree matches if two nodes do not match. If this is set to
     * LOOKAHEAD_FULL, the matcher will look at the entire subtree. The default
     * ist to do no look-ahead matching.
     *
     * @return number of levels to look down for subtree matches if the
     * currently compared nodes do not match
     */
    public int getLookAhead() {
        return lookAhead;
    }

    /**
     * Returns the specific lookahead for the given type or the generic lookahead as returned by
     * {@link #getLookAhead()}.
     *
     * @param type
     *         the type to get the lookahead for
     * @return the lookahead
     */
    public int getLookahead(KeyEnums.Type type) {
        if (lookAheads.containsKey(type)) {
            return lookAheads.containsKey(type) ? lookAheads.get(type) : LOOKAHEAD_OFF;
        } else {
            return lookAhead;
        }
    }

    /**
     * Returns whether lookahead is enabled.
     *
     * @return true iff lookahead is enabled
     */
    public boolean isLookAhead() {
        return !lookAheads.isEmpty() || lookAhead != MergeContext.LOOKAHEAD_OFF;
    }

    /**
     * Sets how many levels to keep searching for matches in the subtree if
     * the currently compared nodes are not equal. If there are no matches
     * within the specified number of levels, do not look for matches deeper in
     * the subtree. If this is set to LOOKAHEAD_OFF, the matcher will stop
     * looking for subtree matches if two nodes do not match. If this is set to
     * LOOKAHEAD_FULL, the matcher will look at the entire subtree. The default
     * ist to do no look-ahead matching.
     *
     * @param lookAhead
     *         number of levels to look down for subtree matches if the
     *         currently compared nodes do not match
     */
    public void setLookAhead(int lookAhead) {
        this.lookAhead = lookAhead;
    }

    /**
     * Sets the specific lookahead for the given type.
     *
     * @param type
     *         the type whose lookahead is to be set
     * @param lookAhead
     *         the lookahead for the type
     */
    public void setLookAhead(KeyEnums.Type type, int lookAhead) {
        lookAheads.put(type, lookAhead);
    }

    /**
     * Returns whether conditional merging is used outside of methods.
     *
     * @return true if conditional merging is used outside of methods
     */
    public boolean isConditionalOutsideMethods() {
        return conditionalOutsideMethods;
    }

    /**
     * Sets whether conditional merging is used outside of methods.
     *
     * @param conditionalOutsideMethods
     *         use conditional merging outside of methods
     */
    public void setConditionalOutsideMethods(boolean conditionalOutsideMethods) {
        this.conditionalOutsideMethods = conditionalOutsideMethods;
    }

    /**
     * Returns the list of <code>MergeScenario</code>s on which JDime crashed.
     *
     * @return list of merge scenarios that crashed
     */
    public Map<MergeScenario<?>, Throwable> getCrashes() {
        return crashes;
    }

    /**
     * Add a <code>MergeScenario</code> to the list of crashed scenarios.
     *
     * @param scenario
     *         <code>MergeScenario</code> which crashed
     * @param t
     *         the crash that occurred
     */
    public void addCrash(MergeScenario<?> scenario, Throwable t) {
        crashes.put(scenario, t);
    }

    /**
     * Returns whether to use the <code>MCESubtreeMatcher</code> during the matching phase of the merge.
     *
     * @return true iff the matcher should be used
     */
    public boolean isUseMCESubtreeMatcher() {
        return useMCESubtreeMatcher;
    }

    /**
     * Sets whether to use the <code>MCESubtreeMatcher</code>.
     *
     * @param useMCESubtreeMatcher
     *         the new value
     */
    public void setUseMCESubtreeMatcher(boolean useMCESubtreeMatcher) {
        this.useMCESubtreeMatcher = useMCESubtreeMatcher;
    }

    /**
     * Returns whether the {@link StructuredStrategy} should act semi-structured, that is whether it should perform line
     * based merging on the configured {@link #semiStructuredLevel}.
     *
     * @return whether the {@link StructuredStrategy} should act semi-structured
     */
    public boolean isSemiStructured() {
        return semiStructured;
    }

    /**
     * Sets whether the {@link StructuredStrategy} should act semi-structured, that is whether it should perform line
     * based merging on the configured {@link #semiStructuredLevel}.
     *
     * @param semiStructured
     *         whether the {@link StructuredStrategy} should act semi-structured
     */
    public void setSemiStructured(boolean semiStructured) {
        this.semiStructured = semiStructured;
    }

    /**
     * Returns the level for which line based merging is used if {@link #semiStructured} is true.
     *
     * @return the level for which line based merging is used
     */
    public KeyEnums.Level getSemiStructuredLevel() {
        return semiStructuredLevel;
    }

    /**
     * Sets the level for which line based merging is used if {@link #semiStructured} is true.
     *
     * @param semiStructuredLevel
     *         the level for which line based merging is used
     */
    public void setSemiStructuredLevel(KeyEnums.Level semiStructuredLevel) {
        this.semiStructuredLevel = semiStructuredLevel;
    }

    /**
     * Returns the number of the artifact that should be inspected.
     *
     * @return number of artifact that should be inspected
     */
    public int getInspectArtifact() {
        return inspectArtifact;
    }

    /**
     * Returns the scope of inspection.
     *
     * @return scope of inspection
     */
    public KeyEnums.Type getInspectionScope() {
        return inspectionScope;
    }

    /**
     * Sets the artifact that should be inspected.
     * If this is set, no merge will be executed.
     *
     * @param inspectArtifact number of the artifact that should be inspected.
     */
    public void setInspectArtifact(int inspectArtifact) {
        this.inspectArtifact = inspectArtifact;
    }

    /**
     * Sets the scope of inspection.
     *
     * @param scope scope of inspection
     */
    public void setInspectionScope(KeyEnums.Type scope) {
        this.inspectionScope = scope;
    }

    /**
     * Whether to inspect an {@link Artifact} instead of merging.
     *
     * @return whether an {@link Artifact} should be inspected instead of merging
     */
    public boolean isInspect() {
        return inspectArtifact > 0;
    }

    public CMMode getCMMatcherMode() {
        return cmMatcherMode;
    }

    public void setCmMatcherMode(CMMode cmMatcher) {
        this.cmMatcherMode = cmMatcher;
    }

    public float getCmReMatchBound() {
        return cmReMatchBound;
    }

    public void setCmReMatchBound(float cmReMatchBound) {
        this.cmReMatchBound = cmReMatchBound;
    }

    public float getWr() {
        return wr;
    }

    public void setWr(float wr) {
        this.wr = wr;
    }

    public float getWn() {
        return wn;
    }

    public void setWn(float wn) {
        this.wn = wn;
    }

    public float getWa() {
        return wa;
    }

    public void setWa(float wa) {
        this.wa = wa;
    }

    public float getWs() {
        return ws;
    }

    public void setWs(float ws) {
        this.ws = ws;
    }

    public float getWo() {
        return wo;
    }

    public void setWo(float wo) {
        this.wo = wo;
    }

    public float getpAssign() {
        return pAssign;
    }

    public void setpAssign(float pAssign) {
        this.pAssign = pAssign;
    }

    public float getFixLower() {
        return fixLower;
    }

    public void setFixLower(float fixLower) {
        this.fixLower = fixLower;
    }

    public float getFixUpper() {
        return fixUpper;
    }

    public void setFixUpper(float fixUpper) {
        this.fixUpper = fixUpper;
    }

    public Optional<Long> getSeed() {
        return seed;
    }

    public void setSeed(Optional<Long> seed) {
        this.seed = seed;
    }

    public int getCostModelIterations() {
        return costModelIterations;
    }

    public void setCostModelIterations(int costModelIterations) {
        this.costModelIterations = costModelIterations;
    }

    public boolean isCmMatcherParallel() {
        return cmMatcherParallel;
    }

    public void setCmMatcherParallel(boolean cmMatcherParallel) {
        this.cmMatcherParallel = cmMatcherParallel;
    }

    public boolean isCmMatcherFixRandomPercentage() {
        return cmMatcherFixRandomPercentage;
    }

    public void setCmMatcherFixRandomPercentage(boolean cmMatcherFixRandomPercentage) {
        this.cmMatcherFixRandomPercentage = cmMatcherFixRandomPercentage;
    }
}
