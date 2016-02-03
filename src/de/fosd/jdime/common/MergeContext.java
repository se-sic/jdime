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
package de.fosd.jdime.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import de.fosd.jdime.config.CommandLineConfigSource;
import de.fosd.jdime.config.JDimeConfig;
import de.fosd.jdime.stats.KeyEnums;
import de.fosd.jdime.stats.Statistics;
import de.fosd.jdime.strategy.LinebasedStrategy;
import de.fosd.jdime.strategy.MergeStrategy;
import de.fosd.jdime.strategy.NWayStrategy;
import de.fosd.jdime.strdump.DumpMode;

import static de.fosd.jdime.config.CommandLineConfigSource.*;
import static de.fosd.jdime.config.JDimeConfig.USE_MCESUBTREE_MATCHER;

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
     * Force overwriting of existing output files.
     */
    private boolean forceOverwriting;

    /**
     * Input Files.
     */
    private ArtifactList<FileArtifact> inputFiles;

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

    /**
     * Whether to use the <code>MCESubtreeMatcher</code> in the matching phase of the merge.
     */
    private boolean useMCESubtreeMatcher;

    /**
     * The standard out/error streams used during the merge.
     */
    private StringWriter stdErr;
    private StringWriter stdIn;

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
        this.keepGoing = false;
        this.exitOnError = false;
        this.mergeStrategy = new LinebasedStrategy();
        this.outputFile = null;
        this.quiet = false;
        this.pretend = true;
        this.recursive = false;
        this.collectStatistics = false;
        this.statistics = null;
        this.useMCESubtreeMatcher = false;
        this.stdErr = new StringWriter();
        this.stdIn = new StringWriter();
        this.lookAhead = MergeContext.LOOKAHEAD_OFF;
        this.lookAheads = new HashMap<>();
        this.crashes = new HashMap<>();
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
        this.forceOverwriting = toCopy.forceOverwriting;

        this.inputFiles = new ArtifactList<>();
        this.inputFiles.addAll(toCopy.inputFiles.stream().map(FileArtifact::clone).collect(Collectors.toList()));

        this.keepGoing = toCopy.keepGoing;
        this.exitOnError = toCopy.exitOnError;
        this.mergeStrategy = toCopy.mergeStrategy; // MergeStrategy should be stateless
        this.outputFile = (toCopy.outputFile != null) ? toCopy.outputFile.clone() : null;
        this.quiet = toCopy.quiet;
        this.pretend = toCopy.pretend;
        this.recursive = toCopy.recursive;
        this.collectStatistics = toCopy.collectStatistics;
        this.statistics = (toCopy.statistics != null) ? new Statistics(toCopy.statistics) : null;
        this.useMCESubtreeMatcher = toCopy.useMCESubtreeMatcher;

        this.stdErr = new StringWriter();
        this.stdErr.append(toCopy.stdErr.toString());

        this.stdIn = new StringWriter();
        this.stdIn.append(toCopy.stdIn.toString());

        this.lookAhead = toCopy.lookAhead;
        this.lookAheads = new HashMap<>(toCopy.lookAheads);

        this.crashes = new HashMap<>(toCopy.crashes);
    }

    /**
     * Initializes the configuration options stored in the <code>MergeContext</code> from the given
     * <code>JDimeConfig</code>.
     *
     * @param config
     *         the <code>JDimeConfig</code> to query for config values
     */
    public void configureFrom(JDimeConfig config) {

        setUseMCESubtreeMatcher(config.getBoolean(USE_MCESUBTREE_MATCHER).orElse(false));

        config.getBoolean(CLI_DIFFONLY).ifPresent(diffOnly -> {
            setDiffOnly(diffOnly);
            config.getBoolean(CLI_CONSECUTIVE).ifPresent(this::setConsecutive);
        });

        config.get(CLI_LOOKAHEAD, val -> {
            try {
                return Optional.of(Integer.parseInt(val));
            } catch (NumberFormatException e) {

                if ("off".equals(val)) {
                    return Optional.of(MergeContext.LOOKAHEAD_OFF);
                } else if ("full".equals(val)) {
                    return Optional.of(MergeContext.LOOKAHEAD_FULL);
                } else {
                    return Optional.empty();
                }
            }
        }).ifPresent(this::setLookAhead);

        for (KeyEnums.Type type : KeyEnums.Type.values()) {
            Optional<Integer> lah = config.getInteger(JDimeConfig.LOOKAHEAD_PREFIX + type.name());
            lah.ifPresent(val -> setLookAhead(type, val));
        }

        config.getBoolean(CLI_STATS).ifPresent(this::collectStatistics);
        config.getBoolean(CLI_FORCE_OVERWRITE).ifPresent(this::setForceOverwriting);
        config.getBoolean(CLI_RECURSIVE).ifPresent(this::setRecursive);

        if (config.getBoolean(CLI_PRINT).orElse(false)) {
            setPretend(true);
            setQuiet(false);
        } else if (config.getBoolean(CLI_QUIET).orElse(false)) {
            setQuiet(true);
        }

        config.getBoolean(CLI_KEEPGOING).ifPresent(this::setKeepGoing);

        config.getBoolean(CLI_EXIT_ON_ERROR).ifPresent(this::setExitOnError);

        Optional<String> args = config.get(CommandLineConfigSource.ARG_LIST);

        if (args.isPresent()) {
            List<String> paths = Arrays.asList(args.get().split(CommandLineConfigSource.ARG_LIST_SEP));

            ArtifactList<FileArtifact> inputArtifacts = new ArtifactList<>();
            char cond = 'A';

            for (String fileName : paths) {

                try {
                    FileArtifact newArtifact = new FileArtifact(new File(fileName));

                    if (isConditionalMerge()) {
                        newArtifact.setRevision(new Revision(String.valueOf(cond++)));
                    }

                    inputArtifacts.add(newArtifact);
                } catch (FileNotFoundException e) {
                    LOG.log(Level.SEVERE, () -> "Input file " + fileName + " not found.");
                    throw new AbortException(e);
                } catch (IOException e) {
                    LOG.log(Level.SEVERE, () -> "Input file " + fileName + " could not be accessed.");
                    throw new AbortException(e);
                }
            }

            setInputFiles(inputArtifacts);
        }

        /*
         * TODO[low priority]
         * The default should in a later, rock-stable version be changed to be overwriting file1 so that we are
         * compatible with gnu merge call syntax.
         */
        config.get(CLI_OUTPUT).ifPresent(outputFileName -> {
            boolean targetIsFile = inputFiles.stream().anyMatch(FileArtifact::isFile);

            try {
                File out = new File(outputFileName);
                FileArtifact outArtifact = new FileArtifact(MergeScenario.MERGE, out, true, targetIsFile);

                setOutputFile(outArtifact);
                setPretend(false);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, e, () -> "Could not create the output FileArtifact.");
            }
        });
    }

    /**
     * Append a String to stdIN.
     *
     * @param s
     *         String to append
     */
    public void append(String s) {
        stdIn.append(s);
    }

    /**
     * Append a String to stdERR.
     *
     * @param s
     *         String to append
     */
    public void appendError(String s) {
        stdErr.append(s);
    }

    /**
     * Appends a line to the saved stdin buffer.
     *
     * @param line
     *         to be appended
     */
    public void appendLine(String line) {
        stdIn.append(line);
        stdIn.append(System.lineSeparator());
    }

    /**
     * Appends a line to the saved stderr buffer.
     *
     * @param line
     *         to be appended
     */
    public void appendErrorLine(String line) {
        stdErr.append(line);
        stdErr.append(System.lineSeparator());
    }

    /**
     * Returns the input files for the merge.
     *
     * @return the input files
     */
    public ArtifactList<FileArtifact> getInputFiles() {
        return inputFiles;
    }

    /**
     * Sets the input files to the new value.
     *
     * @param inputFiles
     *         the new input files
     */
    public void setInputFiles(ArtifactList<FileArtifact> inputFiles) {
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
     * Returns the saved standard error buffer as a <code>String</code>.
     *
     * @return the stdErr buffer as a <code>String</code>
     */
    public String getStdErr() {
        return stdErr.toString();
    }

    /**
     * Returns the saved standard input buffer as a <code>String</code>.
     *
     * @return the stdIn buffer as a <code>String</code>
     */
    public String getStdIn() {
        return stdIn.toString();
    }

    /**
     * Returns true if stdErr is not empty.
     *
     * @return true if stdErr is not empty
     */
    public boolean hasErrors() {
        return stdErr.getBuffer().length() != 0;
    }

    /**
     * Returns true if stdIn is not empty.
     *
     * @return true if stdIn is not empty
     */
    public boolean hasOutput() {
        return stdIn.getBuffer().length() != 0;
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
     * Resets the input streams.
     */
    public void resetStreams() {
        stdIn = new StringWriter();
        stdErr = new StringWriter();
    }

    /**
     * Sets whether statistical data should be collected during the next run using this <code>MergeContext</code>
     *
     * @param collectStatistics
     *         whether to collect statistical data
     */
    public void collectStatistics(boolean collectStatistics) {
        this.collectStatistics = collectStatistics;

        if (collectStatistics && statistics == null) {
            statistics = new Statistics();
        }
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
     * Returns the number of the artifact that should be inspected.
     *
     * @return number of artifact that should be inspected
     */
    public int getInspectArtifact() {
        return inspectArtifact;
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
     * Whether to inspect an artifact instead of merging.
     */
    public boolean isInspect() {
        return inspectArtifact > 0;
    }
}
