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
package de.fosd.jdime.strategy;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.fosd.jdime.artifact.file.FileArtifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.config.merge.MergeScenario;
import de.fosd.jdime.operations.MergeOperation;
import de.fosd.jdime.stats.MergeScenarioStatistics;
import de.fosd.jdime.stats.Statistics;
import de.fosd.jdime.stats.parser.ParseResult;
import de.uni_passau.fim.seibt.gitwrapper.process.ProcessExecutor.ExecRes;
import de.uni_passau.fim.seibt.gitwrapper.repo.GitWrapper;
import org.apache.commons.io.FileUtils;

/**
 * Performs an unstructured, line based merge.
 * <p>
 * The current implementation uses the merge routine provided by <code>git</code>.
 *
 * @author Olaf Lessenich
 */
public class LinebasedStrategy extends MergeStrategy<FileArtifact> {

    private static final Logger LOG = Logger.getLogger(LinebasedStrategy.class.getCanonicalName());

    private static final File WORKING_DIR = new File(".");
    private static final String MERGE_FILE = "merge-file";
    private static final String QUIET = "-q";
    private static final String PRINT = "-p";

    private static final FileRepo repo = new FileRepo();

    /**
     * A repository of temporary files containing content snapshots for {@link FileArtifact FileArtifacts}.
     */
    private static class FileRepo {

        private static final Logger LOG = Logger.getLogger(FileRepo.class.getCanonicalName());

        private final Path parentDir;
        private final Map<String, File> cachedFiles;

        /**
         * Constructs a new {@link FileRepo} that stores its files in the temporary directory of the system.
         */
        public FileRepo() {
            this(FileUtils.getTempDirectory());
        }

        /**
         * Constructs a new {@link FileRepo} that stores its files under the given {@code parentDir}.
         *
         * @param parentDir
         *         the directory to store the temporary files in
         * @throws IllegalArgumentException
         *         if {@code parentDir} exists and is not a directory
         */
        public FileRepo(File parentDir) {

            if (parentDir.exists() && !parentDir.isDirectory()) {
                throw new IllegalArgumentException(parentDir + " must not exist or be a directory.");
            }

            this.parentDir = parentDir.toPath();
            this.cachedFiles = new ConcurrentHashMap<>();
        }

        /**
         * Returns an existing file containing the current contents of {@code #artifact}. Returns an empty optional
         * if {@code artifact} is a directory or if there is an exception writing the contents of {@code artifact}
         * to disk.
         *
         * @param artifact
         *         the {@link FileArtifact} to return an existing file for
         * @return optionally an existing file containing the current contents of {@code artifact}
         */
        public Optional<File> fileFor(FileArtifact artifact) {
            if (artifact.isDirectory()) {
                return Optional.empty();
            }

            return Optional.ofNullable(cachedFiles.computeIfAbsent(artifact.getContentHash(), hash -> {
                File file;

                try {
                    file = Files.createTempFile(parentDir, String.valueOf(hash), null).toFile();
                    FileUtils.forceDeleteOnExit(file);

                    try (PrintStream to = new PrintStream(FileUtils.openOutputStream(file))) {
                        artifact.outputContent(to);
                    }
                } catch (IOException e) {
                    LOG.log(Level.WARNING, e, () -> "Failed to write a temporary file for " + artifact);
                    return null;
                }

                return file;
            }));
        }
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
    @Override
    public void merge(MergeOperation<FileArtifact> operation, MergeContext context) {

        long runtime, startTime = System.currentTimeMillis();

        ExecRes execRes = mergeFiles(operation, context);

        runtime = System.currentTimeMillis() - startTime;
        LOG.fine(() -> String.format("%s merge time was %d ms.", getClass().getSimpleName(), runtime));

        if (!context.isDiffOnly()) {
            operation.getTarget().setContent(execRes.stdOut);
        }

        if (context.hasStatistics()) {
            Statistics statistics = context.getStatistics();
            MergeScenarioStatistics scenarioStatistics = new MergeScenarioStatistics(operation.getMergeScenario());
            ParseResult res = scenarioStatistics.setLineStatistics(execRes.stdOut);

            if (res.getConflicts() > 0) {
                scenarioStatistics.getFileStatistics().incrementNumOccurInConflic();
            }

            scenarioStatistics.setRuntime(runtime);
            statistics.addScenarioStatistics(scenarioStatistics);
        }
    }

    /**
     * Passes {@link MergeScenario#getLeft()}, {@link MergeScenario#getBase()} and {@link MergeScenario#getRight()}
     * to a call to {@code git merge-file} and returns the result of the command execution.
     *
     * @param op
     *         the {@link MergeOperation} containing the {@link FileArtifact FileAritfacts} to be merged
     * @param context
     *         the {@link MergeContext} containing the {@link GitWrapper} to be used
     * @return the result of the execution of {@code git merge-file}
     * @throws RuntimeException
     *         if the native git execution fails
     */
    public static ExecRes mergeFiles(MergeOperation<FileArtifact> op, MergeContext context) {
        Supplier<RuntimeException> failed = () -> new RuntimeException("Failed to merge using 'git merge-file'.");
        String left, base, right;

        {
            MergeScenario<FileArtifact> triple = op.getMergeScenario();
            Optional<File> oLeft = repo.fileFor(triple.getLeft());
            Optional<File> oBase = repo.fileFor(triple.getBase());
            Optional<File> oRight = repo.fileFor(triple.getRight());

            if (oLeft.isPresent() && oBase.isPresent() && oRight.isPresent()) {
                left = oLeft.get().getPath();
                base = oBase.get().getPath();
                right = oRight.get().getPath();
            } else {
                throw failed.get();
            }
        }

        GitWrapper git = context.getGit();

        Optional<ExecRes> oRes = git.exec(WORKING_DIR, MERGE_FILE, QUIET, PRINT, left, base, right);
        return oRes.map(r -> git.failedPrefix(r) ? null : r).orElseThrow(failed);
    }
}
