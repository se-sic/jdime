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

import java.io.FileWriter;
import java.io.IOException;
import java.security.Permission;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.fosd.jdime.common.ASTNodeArtifact;
import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeScenario;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.stats.MergeScenarioStatistics;
import de.fosd.jdime.stats.Statistics;
import de.fosd.jdime.stats.parser.ParseResult;

/**
 * Performs a structured merge on <code>FileArtifacts</code>.
 *
 * @author Olaf Lessenich
 */
public class StructuredStrategy extends MergeStrategy<FileArtifact> {

    private static final Logger LOG = Logger.getLogger(StructuredStrategy.class.getCanonicalName());
    private static final String CONFLICT_START = "<<<<<<<";
    private static final String CONFLICT_DELIM = "=======";
    private static final String CONFLICT_END = ">>>>>>>";

    private SecurityManager systemSecurityManager = System.getSecurityManager();
    private SecurityManager noExitManager = new SecurityManager() {
            @Override
            public void checkPermission(Permission perm) {
                // allow anything.
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // allow anything.
            }

            @Override
            public void checkExit(int status) {
                super.checkExit(status);
                throw new SecurityException("Captured attempt to exit JVM.");
            }
        };

    /**
     * The source <code>FileArtifacts</code> are extracted from the
     * <code>MergeOperation</code>, parsed by the <code>JastAddJ</code> parser
     * into abstract syntax trees, and on the fly encapsulated into
     * <code>ASTNodeArtifacts</code>.
     * <p>
     * A new <code>MergeOperation</code>, encapsulating
     * <code>ASTNodeArtifacts</code> as source and target nodes, is created and applied.
     *
     * TODO: more high-level documentation.
     *
     * @param operation the <code>MergeOperation</code> to perform
     * @param context the <code>MergeContext</code>
     */
    @Override
    public final void merge(MergeOperation<FileArtifact> operation, MergeContext context) throws IOException, InterruptedException {
        /**
         * The method creates ASTNodeArtifacts from the input files. An ASTNodeStrategy is then applied.
         * The result is pretty printed and possibly written to the output file.
         */

        MergeScenario<FileArtifact> triple = operation.getMergeScenario();

        FileArtifact leftFile = triple.getLeft();
        FileArtifact rightFile = triple.getRight();
        FileArtifact baseFile = triple.getBase();
        FileArtifact target = null;

        String lPath = leftFile.getPath();
        String bPath = baseFile.getPath();
        String rPath = rightFile.getPath();

        if (!context.isDiffOnly() && operation.getTarget() != null) {
            target = operation.getTarget();

            if (target.exists() && !target.isEmpty()) {
                throw new AssertionError(String.format("Would be overwritten: %s", target));
            }
        }

        context.resetStreams();
        System.setSecurityManager(noExitManager);

        LOG.fine(() -> String.format("Merging:%nLeft: %s%nBase: %s%nRight: %s", lPath, bPath, rPath));

        try {
            long startTime = System.currentTimeMillis();

            ASTNodeArtifact left = new ASTNodeArtifact(leftFile);
            ASTNodeArtifact base = new ASTNodeArtifact(baseFile);
            ASTNodeArtifact right = new ASTNodeArtifact(rightFile);

            ASTNodeArtifact targetNode = ASTNodeArtifact.createProgram(left);
            targetNode.setRevision(left.getRevision());
            targetNode.renumberTree();

            String lCond = left.getRevision().getName();
            String rCond = right.getRevision().getName();
            MergeScenario<ASTNodeArtifact> nodeTriple = new MergeScenario<>(triple.getMergeType(), left, base, right);
            MergeOperation<ASTNodeArtifact> astMergeOp = new MergeOperation<>(nodeTriple, targetNode, lCond, rCond);

            LOG.finest(() -> String.format("Tree dump of target node:%n%s", targetNode.dumpTree()));
            LOG.finest(() -> String.format("MergeScenario:%n%s", nodeTriple.toString()));
            LOG.finest("Applying an ASTNodeArtifact MergeOperation.");

            astMergeOp.apply(context);

            long runtime = System.currentTimeMillis() - startTime;

            LOG.fine("Structured merge finished.");

            if (!context.isDiffOnly()) {
                LOG.finest(() -> String.format("Tree dump of target node:%n%s", targetNode.dumpTree()));
            }

            LOG.finest(() -> String.format("Pretty-printing left:%n%s", left.prettyPrint()));
            LOG.finest(() -> String.format("Pretty-printing right:%n%s", right.prettyPrint()));

            String prettyPrint = "";

            if (!context.isDiffOnly()) {
                prettyPrint = targetNode.prettyPrint();

                final String finalPrettyPrint = prettyPrint;
                LOG.finest(() -> String.format("Pretty-printing merge result:%n%s", finalPrettyPrint));
            }

            LOG.fine(() -> String.format("%s merge time was %d ms.", getClass().getSimpleName(), runtime));

            if (context.hasErrors()) {
                LOG.severe(() -> String.format("Errors occurred while merging structurally.%n%s", context.getStdErr()));
            }

            if (!context.isPretend() && target != null) {
                LOG.fine("Writing output to: " + target.getFullPath());
                target.write(prettyPrint);
            }

            if (context.hasStatistics()) {
                if (LOG.isLoggable(Level.FINE)) {
                    String fileName = leftFile + ".dot";
                    LOG.fine("Dumping the target node tree to " + fileName);

                    try (FileWriter file = new FileWriter(fileName)) {
                        file.write(new ASTNodeStrategy().dumpTree(targetNode, true));
                    }
                }

                Statistics statistics = context.getStatistics();
                MergeScenarioStatistics scenarioStatistics = new MergeScenarioStatistics(triple);

                if (!context.isDiffOnly()) {
                    ParseResult parseResult = scenarioStatistics.setLineStatistics(prettyPrint);

                    if (parseResult.getConflicts() > 0) {
                        scenarioStatistics.getFileStatistics().incrementNumOccurInConflic();
                    }
                }

                scenarioStatistics.add(StatisticsInterface.getASTStatistics(left, right.getRevision()));
                scenarioStatistics.add(StatisticsInterface.getASTStatistics(right, left.getRevision()));

                if (target != null) {
                    scenarioStatistics.add(StatisticsInterface.getASTStatistics(target, null));
                }

                scenarioStatistics.setRuntime(runtime);
                statistics.addScenarioStatistics(scenarioStatistics);
            }
        } catch (SecurityException e) {
            LOG.log(Level.SEVERE, e, () -> "SecurityException while merging.");
            context.addCrash(triple, e);
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, t, () -> String.format("Exception while merging:%nLeft: %s%nBase: %s%nRight: %s", lPath, bPath, rPath));
            context.addCrash(triple, t);

            if (!context.isKeepGoing()) {
                throw t;
            }
        } finally {
            System.setSecurityManager(systemSecurityManager);
        }
    }

    @Override
    public final String toString() {
        return "structured";
    }

    @Override
    public final String dumpTree(FileArtifact artifact, boolean graphical) throws IOException {
        return new ASTNodeStrategy().dumpTree(new ASTNodeArtifact(artifact), graphical);
    }

    @Override
    public String dumpFile(FileArtifact artifact, boolean graphical) throws IOException {
        System.setSecurityManager(noExitManager);
        try {
            return new ASTNodeStrategy().dumpFile(new ASTNodeArtifact(artifact), graphical);
        } catch (SecurityException e) {
            return e.toString();
        } finally {
            System.setSecurityManager(systemSecurityManager);
        }
    }
}
