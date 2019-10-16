/**
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2018 University of Passau, Germany
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

import de.fosd.jdime.artifact.file.FileArtifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.config.merge.MergeScenario;
import de.fosd.jdime.operations.MergeOperation;
import de.fosd.jdime.stats.MergeScenarioStatistics;
import de.fosd.jdime.stats.Runtime;
import de.fosd.jdime.stats.Statistics;
import de.fosd.jdime.util.parser.ConflictContent;
import de.fosd.jdime.util.parser.ParseResult;
import de.uni_passau.fim.seibt.GitMergeFileInput;
import de.uni_passau.fim.seibt.GitMergeFileOptions;
import de.uni_passau.fim.seibt.GitMergeFileResult;
import de.uni_passau.fim.seibt.LibGit2;

import java.util.logging.Logger;

import static de.fosd.jdime.stats.Runtime.MERGE_LABEL;
import static java.nio.charset.StandardCharsets.UTF_8;

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
     * Constructs a new {@link LinebasedStrategy}.
     */
    public LinebasedStrategy() {
        LOG.fine("Constructing a " + getClass().getSimpleName() + " using libgit2 " + LibGit2.git_libgit2_version());
    }

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
     */
    @Override @SuppressWarnings("try")
    public void merge(MergeOperation<FileArtifact> operation, MergeContext context) {
        Runtime merge = new Runtime(MERGE_LABEL);
        String mergeResult;

        try (Runtime.Measurement m = merge.time()) {
            mergeResult = mergeFiles(operation);
        }

        LOG.fine(() -> String.format("%s merge time was %d ms.", getClass().getSimpleName(), merge.getTimeMS()));

        if (!context.isDiffOnly()) {
            operation.getTarget().setContent(mergeResult);
        }

        // TODO this filters out method specific statistics in semistructured mode, they should instead be marked somehow but kept in the XML
        if (context.hasStatistics() && !context.isSemiStructured()) {
            Statistics statistics = context.getStatistics();
            MergeScenarioStatistics scenarioStatistics = statistics.getScenarioStatistics(operation.getMergeScenario());
            scenarioStatistics.setStrategy(getClass());

            ParseResult res = scenarioStatistics.setLineStatistics(mergeResult);

            if (res.getStats().getConflicts() > 0) {
                scenarioStatistics.getFileStatistics().incrementNumOccurInConflict();
            }

            scenarioStatistics.putRuntime(merge);
        }
    }

    /**
     * Merges the contents of the {@link FileArtifact FileArtifacts} contained in the {@link MergeScenario} that is
     * being merged in the {@link MergeOperation} {@code op}.
     *
     * @param op
     *         the current {@link MergeOperation}
     * @return the merged file contents
     */
    private String mergeFiles(MergeOperation<FileArtifact> op) {
        FileArtifact leftFile = op.getMergeScenario().getLeft();
        FileArtifact baseFile = op.getMergeScenario().getBase();
        FileArtifact rightFile = op.getMergeScenario().getRight();

        String leftL = leftFile.getFile().getPath();
        String baseL = baseFile.getFile().getPath();
        String rightL = rightFile.getFile().getPath();

        GitMergeFileOptions opts = new GitMergeFileOptions();
        GitMergeFileResult res = new GitMergeFileResult();

        GitMergeFileInput left = new GitMergeFileInput();
        left.setContent(leftFile.getContent(), UTF_8);
        opts.our_label = leftL;

        GitMergeFileInput base = new GitMergeFileInput();
        base.setContent(baseFile.getContent(), UTF_8);
        opts.ancestor_label = baseL;

        GitMergeFileInput right = new GitMergeFileInput();
        right.setContent(rightFile.getContent(), UTF_8);
        opts.their_label = rightL;

        opts.marker_size = ConflictContent.MARKER_SIZE;

        LibGit2.git_merge_file(res, base, left, right, opts);

        return res.getResult(UTF_8);
    }
}
