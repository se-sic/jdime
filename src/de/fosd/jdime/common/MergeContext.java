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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.fosd.jdime.stats.Statistics;
import de.fosd.jdime.strategy.LinebasedStrategy;
import de.fosd.jdime.strategy.MergeStrategy;
import de.fosd.jdime.strategy.NWayStrategy;

/**
 * @author Olaf Lessenich
 */
public class MergeContext implements Cloneable {

    /**
     * Do look at all nodes in the subtree even if the compared nodes are not
     * equal.
     */
    public static final int LOOKAHEAD_FULL = -1;

    /**
     * Stop looking for subtree matches if the two nodes compared are not equal.
     */
    public static final int LOOKAHEAD_OFF = 0;

    /**
     * Whether merge inserts choice nodes instead of direct merging.
     */
    private boolean conditionalMerge = false;

    /**
     * Whether conditional merge should be performed outside of methods.
     */
    private boolean conditionalOutsideMethods = true;

    /**
     * Whether to run only the diff.
     */
    private boolean diffOnly = false;

    /**
     * Whether to treat two input versions as consecutive versions in the
     * revision history.
     */
    private boolean consecutive = false;

    /**
     * Whether to dump files instead of merging.
     */
    private boolean dumpFiles = false;

    /**
     * Whether to dump ASTs instead of merging.
     */
    private boolean dumpTree = false;

    /**
     * Force overwriting of existing output files.
     */
    private boolean forceOverwriting = false;

    /**
     * Whether to use graphical output while dumping.
     */
    private boolean guiDump = false;

    /**
     * Input Files.
     */
    private ArtifactList<FileArtifact> inputFiles;

    /**
     * If true, merging will be continued after exceptions.
     */
    private boolean keepGoing = false;

    /**
     * Strategy to apply for the merge.
     */
    private MergeStrategy<?> mergeStrategy = new LinebasedStrategy();

    /**
     * Output file.
     */
    private FileArtifact outputFile;

    /**
     * Timestamp of program start.
     */
    private long programStart;

    /**
     * If true, the output is quiet.
     */
    private boolean quiet = false;

    /**
     * If true, output is not written to an output file.
     */
    private boolean pretend = true;

    /**
     * Merge directories recursively. Can be set with the '-r' argument.
     */
    private boolean recursive = false;

    private boolean collectStatistics = false;
    private Statistics statistics = null;

    /**
     * StdOut of a merge operation.
     */
    private StringWriter stdErr = new StringWriter();

    /**
     * StdIn of a merge operation.
     */
    private StringWriter stdIn = new StringWriter();

    /**
     * How many levels to keep searching for matches in the subtree if the
     * currently compared nodes are not equal. If there are no matches within
     * the specified number of levels, do not look for matches deeper in the
     * subtree. If this is set to LOOKAHEAD_OFF, the matcher will stop looking
     * for subtree matches if two nodes do not match. If this is set to
     * LOOKAHEAD_FULL, the matcher will look at the entire subtree.
     * The default ist to do no look-ahead matching.
     */
    private int lookAhead = MergeContext.LOOKAHEAD_OFF;
    private Map<MergeScenario<?>, Throwable> crashes = new HashMap<>();

    /**
     * Class constructor.
     */
    public MergeContext() {
        programStart = System.currentTimeMillis();
    }

    /**
     * Returns the median of a list of long values.
     *
     * @param values
     *         list of values for which to compute the median
     * @return median
     */
    public static long median(ArrayList<Long> values) {
        Collections.sort(values);

        if (values.size() % 2 == 1) {
            return values.get((values.size() + 1) / 2 - 1);
        } else {
            double lower = values.get(values.size() / 2 - 1);
            double upper = values.get(values.size() / 2);

            return Math.round((lower + upper) / 2.0);
        }
    }

    /**
     * Append a String to stdIN.
     *
     * @param s
     *         String to append
     */
    public void append(String s) {
        if (stdIn != null) {
            stdIn.append(s);
        }
    }

    /**
     * Append a String to stdERR.
     *
     * @param s
     *         String to append
     */
    public void appendError(String s) {
        if (stdErr != null) {
            stdErr.append(s);
        }
    }

    /**
     * Appends a line to the saved stderr buffer.
     *
     * @param line
     *         to be appended
     */
    public void appendErrorLine(String line) {
        if (stdErr != null) {
            stdErr.append(line);
            stdErr.append(System.getProperty("line.separator"));
        }
    }

    /**
     * Appends a line to the saved stdin buffer.
     *
     * @param line
     *         to be appended
     */
    public void appendLine(String line) {
        if (stdIn != null) {
            stdIn.append(line);
            stdIn.append(System.lineSeparator());
        }
    }

    /**
     * @return the inputFiles
     */
    public ArtifactList<FileArtifact> getInputFiles() {
        return inputFiles;
    }

    /**
     * @param inputFiles
     *         the inputFiles to set
     */
    public void setInputFiles(ArtifactList<FileArtifact> inputFiles) {
        this.inputFiles = inputFiles;
    }

    /**
     * Returns the merge strategy.
     *
     * @return the merge strategy
     */
    public MergeStrategy<?> getMergeStrategy() {
        return mergeStrategy;
    }

    /**
     * Sets the merge strategy.
     *
     * @param mergeStrategy
     *         merge strategy
     */
    public void setMergeStrategy(MergeStrategy<?> mergeStrategy) {
        this.mergeStrategy = mergeStrategy;

        if (mergeStrategy instanceof NWayStrategy) {
            conditionalMerge = true;
        }
    }

    /**
     * @return the outputFile
     */
    public FileArtifact getOutputFile() {
        return outputFile;
    }

    /**
     * @param outputFile
     *         the outputFile to set
     */
    public void setOutputFile(FileArtifact outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * @return timestamp of program start
     */
    public long getProgramStart() {
        return programStart;
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
        return stdErr != null && stdErr.getBuffer().length() != 0;
    }

    /**
     * Returns true if stdIn is not empty.
     *
     * @return true if stdIn is not empty
     */
    public boolean hasOutput() {
        return stdIn != null && stdIn.getBuffer().length() != 0;
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
     * @return the diffOnly
     */
    public boolean isDiffOnly() {
        return diffOnly;
    }

    /**
     * @param diffOnly
     *         whether to run only diff
     */
    public void setDiffOnly(boolean diffOnly) {
        this.diffOnly = diffOnly;
    }

    /**
     * @return the dumpFiles
     */
    public boolean isDumpFile() {
        return dumpFiles;
    }

    /**
     * @return the dumpTree
     */
    public boolean isDumpTree() {
        return dumpTree;
    }

    /**
     * @param dumpTree
     *         the dumpTree to set
     */
    public void setDumpTree(boolean dumpTree) {
        this.dumpTree = dumpTree;
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
     * @return the guiDump
     */
    public boolean isGuiDump() {
        return guiDump;
    }

    /**
     * @param guiDump
     *         the guiDump to set
     */
    public void setGuiDump(boolean guiDump) {
        this.guiDump = guiDump;
    }

    /**
     * @return the keepGoing
     */
    public boolean isKeepGoing() {
        return keepGoing;
    }

    /**
     * @param keepGoing
     *         the keepGoing to set
     */
    public void setKeepGoing(boolean keepGoing) {
        this.keepGoing = keepGoing;
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
     * @param dumpFiles
     *         the dumpFiles to set
     */
    public void setDumpFiles(boolean dumpFiles) {
        this.dumpFiles = dumpFiles;
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
     * @return whether consecutive diffing
     */
    public boolean isConsecutive() {
        return consecutive;
    }

    /**
     * @param consecutive
     *         consecutive diffing
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
     * Whether merge inserts choice nodes instead of direct merging of artifact.
     */
    public boolean isConditionalMerge(Artifact<?> artifact) {
        return conditionalMerge && (conditionalOutsideMethods || artifact instanceof ASTNodeArtifact && ((ASTNodeArtifact) artifact).isWithinMethod());
    }

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

    public boolean isLookAhead() {
        return lookAhead != MergeContext.LOOKAHEAD_OFF;
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

    @Override
    public Object clone() {
        MergeContext clone = new MergeContext();
        clone.forceOverwriting = forceOverwriting;
        clone.mergeStrategy = mergeStrategy;
        clone.inputFiles = inputFiles;
        clone.outputFile = outputFile;
        clone.quiet = quiet;
        clone.recursive = recursive;
        clone.collectStatistics = collectStatistics;
        clone.keepGoing = keepGoing;
        clone.diffOnly = diffOnly;
        clone.lookAhead = lookAhead;
        return clone;
    }
}
