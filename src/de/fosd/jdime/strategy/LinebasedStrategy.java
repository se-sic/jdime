/*
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
package de.fosd.jdime.strategy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeScenario;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.stats.MergeScenarioStatistics;
import de.fosd.jdime.stats.Statistics;
import de.fosd.jdime.stats.Stats;
import de.fosd.jdime.stats.parser.ParseResult;

/**
 * Performs an unstructured, line based merge.
 * <p>
 * The current implementation uses the merge routine provided by <code>git</code>.
 *
 * @author Olaf Lessenich
 */
public class LinebasedStrategy extends MergeStrategy<FileArtifact> {

    private static final Logger LOG = Logger.getLogger(LinebasedStrategy.class.getCanonicalName());
    
    /**
     * The command to use for merging.
     */
    private static final String BASECMD = "git";
    
    /**
     * The arguments for <code>BASECMD</code>.
     */
    private static final List<String> BASEARGS = Arrays.asList("merge-file", "-q", "-p");

    /**
     * This line-based <code>merge</code> method uses the merging routine of
     * the external tool <code>git</code>.
     * <p>
     * Basically, the input <code>FileArtifacts</code> are passed as arguments to
     * `git merge-file -q -p`.
     * <p>
     * In a common run, the number of processed lines of code, the number of
     * conflicting situations, and the number of conflicting lines of code will
     * be counted. Empty lines and comments are skipped to keep
     * <code>MergeStrategies</code> comparable, as JDime does (in its current
     * implementation) not respect comments.
     * <p>
     * In case of a performance benchmark, the output is simply ignored for the
     * sake of speed, and the merge will be run the specified amount of times,
     * aiming to allow the computation of a reasonable mean runtime.
     *
     * @param operation <code>MergeOperation</code> that is executed by this strategy
     * @param context <code>MergeContext</code> that is used to retrieve environmental parameters
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Override
    public void merge(MergeOperation<FileArtifact> operation, MergeContext context) throws IOException, InterruptedException {
        MergeScenario<FileArtifact> triple = operation.getMergeScenario();
        FileArtifact target = null;

        if (operation.getTarget() != null) {
            target = operation.getTarget();

            if (target.exists() && !target.isEmpty()) {
                throw new AssertionError(String.format("Would be overwritten: %s", target));
            }
        }

        context.resetStreams();

        List<String> cmd = new ArrayList<>();
        cmd.add(BASECMD);
        cmd.addAll(BASEARGS);
        cmd.addAll(triple.asList().stream().limit(3).map(FileArtifact::getPath).collect(Collectors.toList()));

        ProcessBuilder pb = new ProcessBuilder(cmd);

        LOG.fine(() -> "Running external command: " + String.join(" ", cmd));
        long runtime, startTime = System.currentTimeMillis();
        Process pr = pb.start();

        StringBuilder processOutput = new StringBuilder();
        StringBuilder processErrorOutput = new StringBuilder();
        String ls = System.lineSeparator();

        try (BufferedReader r = new BufferedReader(new InputStreamReader(pr.getInputStream()))) {
            String line;

            while ((line = r.readLine()) != null) {
                processOutput.append(line).append(ls);
            }
        }

        try (BufferedReader r = new BufferedReader(new InputStreamReader(pr.getErrorStream()))) {
            String line;

            while ((line = r.readLine()) != null) {
                processErrorOutput.append(line).append(ls);
            }
        }

        context.append(processOutput.toString());
        context.appendError(processErrorOutput.toString());

        pr.waitFor();
        runtime = System.currentTimeMillis() - startTime;

        LOG.fine(() -> String.format("%s merge time was %d ms.", getClass().getSimpleName(), runtime));

        if (context.hasErrors()) {
            LOG.severe(() -> String.format("Errors occurred while calling '%s'%n%s", String.join(" ", cmd), context.getStdErr()));
        }

        if (!context.isPretend() && target != null) {
            LOG.fine("Writing output to: " + target.getFullPath());
            target.write(context.getStdIn());
        }

        if (context.hasStatistics()) {
            Statistics statistics = context.getStatistics();
            MergeScenarioStatistics scenarioStatistics = new MergeScenarioStatistics(triple);
            ParseResult res = scenarioStatistics.addLineStatistics(processOutput.toString());

            if (res.getConflicts() > 0) {
                statistics.getFileStatistics().incrementNumOccurInConflic();
            }

            scenarioStatistics.setRuntime(runtime);
            statistics.addScenarioStatistics(scenarioStatistics);
        }
    }

    @Override
    public final Stats createStats() {
        return new Stats(new String[] { "directories", "files", "lines" });
    }

    @Override
    public final String toString() {
        return "linebased";
    }

    @Override
    public final String getStatsKey(FileArtifact artifact) {
        return "lines";
    }

    /**
     * Throws <code>UnsupportedOperationException</code>. You should use a structured strategy to dump a tree.
     * 
     * @param artifact
     *            artifact to dump
     * @param graphical
     *            output option
     */
    @Override
    public final String dumpTree(FileArtifact artifact, boolean graphical) {
        throw new UnsupportedOperationException("Use a structured strategy to dump a tree.");
    }

    @Override
    public String dumpFile(FileArtifact artifact, boolean graphical) throws IOException { //TODO: optionally save to outputfile
        List<String> lines = Files.readAllLines(artifact.getFile().toPath(), StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();

        for (String line : lines) {
            sb.append(line);
            sb.append(System.lineSeparator());
        }

        return sb.toString();
    }
}
