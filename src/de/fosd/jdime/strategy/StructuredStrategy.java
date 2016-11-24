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

import java.io.FileWriter;
import java.io.IOException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.fosd.jdime.artifact.ast.ASTNodeArtifact;
import de.fosd.jdime.artifact.ast.SemiStructuredArtifact;
import de.fosd.jdime.artifact.file.FileArtifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.config.merge.MergeScenario;
import de.fosd.jdime.operations.MergeOperation;
import de.fosd.jdime.stats.KeyEnums;
import de.fosd.jdime.stats.MergeScenarioStatistics;
import de.fosd.jdime.stats.Statistics;
import de.fosd.jdime.stats.StatisticsInterface;
import de.fosd.jdime.stats.parser.ParseResult;

import static de.fosd.jdime.stats.KeyEnums.Type.BLOCK;
import static de.fosd.jdime.strdump.DumpMode.GRAPHVIZ_TREE;
import static de.fosd.jdime.strdump.DumpMode.PLAINTEXT_TREE;

/**
 * Performs a structured merge on <code>FileArtifacts</code>.
 *
 * @author Olaf Lessenich
 */
public class StructuredStrategy extends MergeStrategy<FileArtifact> {

    private static final Logger LOG = Logger.getLogger(StructuredStrategy.class.getCanonicalName());

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
    public void merge(MergeOperation<FileArtifact> operation, MergeContext context) {
        /**
         * The method creates ASTNodeArtifacts from the input files. An ASTNodeStrategy is then applied.
         * The result is pretty printed and possibly written to the output file.
         */

        MergeScenario<FileArtifact> triple = operation.getMergeScenario();

        FileArtifact target = operation.getTarget();
        FileArtifact leftFile = triple.getLeft();
        FileArtifact rightFile = triple.getRight();
        FileArtifact baseFile = triple.getBase();

        String lPath = leftFile.getPath();
        String bPath = baseFile.getPath();
        String rPath = rightFile.getPath();

        System.setSecurityManager(noExitManager);

        LOG.fine(() -> String.format("Merging:%nLeft: %s%nBase: %s%nRight: %s", lPath, bPath, rPath));

        try {
            long startTime = System.currentTimeMillis();

            ASTNodeArtifact left = new ASTNodeArtifact(leftFile);
            ASTNodeArtifact base = new ASTNodeArtifact(baseFile);
            ASTNodeArtifact right = new ASTNodeArtifact(rightFile);

            if (context.isSemiStructured()) {
                left = makeSemistructured(left, context.getSemiStructuredLevel());
                base = makeSemistructured(base, context.getSemiStructuredLevel());
                right = makeSemistructured(right, context.getSemiStructuredLevel());
            }

            ASTNodeArtifact targetNode = ASTNodeArtifact.createProgram(left);

            MergeScenario<ASTNodeArtifact> nodeTriple = new MergeScenario<>(triple.getMergeType(), left, base, right);
            MergeOperation<ASTNodeArtifact> astMergeOp = new MergeOperation<>(nodeTriple, targetNode);

            LOG.finest(() -> String.format("Tree dump of target node:%n%s", targetNode.dump(PLAINTEXT_TREE)));
            LOG.finest(() -> String.format("MergeScenario:%n%s", nodeTriple.toString()));
            LOG.finest("Applying an ASTNodeArtifact MergeOperation.");

            astMergeOp.apply(context);
            targetNode.setRevision(MergeScenario.TARGET, true); // TODO do this somewhere else?

            if (!context.isDiffOnly()) {
                target.setContent(targetNode.prettyPrint());
            }

            long runtime = System.currentTimeMillis() - startTime;

            LOG.fine("Structured merge finished.");
            LOG.fine(() -> String.format("%s merge time was %d ms.", getClass().getSimpleName(), runtime));

            if (!context.isDiffOnly()) {
                LOG.finest(() -> String.format("Tree dump of target node:%n%s", targetNode.dump(PLAINTEXT_TREE)));
            }

            ASTNodeArtifact finalLeft = left;
            LOG.finest(() -> String.format("Pretty-printing left:%n%s", finalLeft.prettyPrint()));
            ASTNodeArtifact finalRight = right;
            LOG.finest(() -> String.format("Pretty-printing right:%n%s", finalRight.prettyPrint()));

            if (!context.isDiffOnly()) {
                LOG.finest(() -> String.format("Pretty-printing merge result:%n%s", target.getContent()));
            }

            if (context.hasStatistics()) {
                if (LOG.isLoggable(Level.FINE)) {
                    String fileName = leftFile + ".dot";
                    LOG.fine("Dumping the target node tree to " + fileName);

                    try (FileWriter fw = new FileWriter(fileName)) {
                        fw.write(targetNode.dump(GRAPHVIZ_TREE));
                    } catch (IOException e) {
                        LOG.log(Level.WARNING, e, () -> "Can not write the graphviz representation of " + leftFile);
                    }
                }

                Statistics statistics = context.getStatistics();
                MergeScenarioStatistics scenarioStatistics = new MergeScenarioStatistics(triple);

                if (!context.isDiffOnly()) {
                    ParseResult parseResult = scenarioStatistics.setLineStatistics(target.getContent());

                    if (parseResult.getConflicts() > 0) {
                        scenarioStatistics.getFileStatistics().incrementNumOccurInConflic();
                    }
                }

                scenarioStatistics.add(StatisticsInterface.getASTStatistics(left, right.getRevision()));
                scenarioStatistics.add(StatisticsInterface.getASTStatistics(right, left.getRevision()));
                scenarioStatistics.add(StatisticsInterface.getASTStatistics(targetNode, null));
                scenarioStatistics.setRuntime(runtime);

                statistics.addScenarioStatistics(scenarioStatistics);
            }
        } finally {
            System.setSecurityManager(systemSecurityManager);
        }
    }

    private ASTNodeArtifact makeSemistructured(ASTNodeArtifact artifact, KeyEnums.Level level) {
        ASTNodeArtifact transformed;

        if (artifact.getType() == BLOCK && artifact.getLevel() == level) {
            transformed = new SemiStructuredArtifact(artifact);
        } else {
            List<ASTNodeArtifact> children = new ArrayList<>(artifact.getChildren());
            children.forEach(c -> makeSemistructured(c, level));

            transformed = artifact;
        }

        return transformed;
    }
}
