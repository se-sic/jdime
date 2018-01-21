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
package de.fosd.jdime.strategy;

import de.fosd.jdime.artifact.ast.ASTNodeArtifact;
import de.fosd.jdime.artifact.file.FileArtifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.config.merge.MergeScenario;
import de.fosd.jdime.operations.MergeOperation;
import de.fosd.jdime.stats.MergeScenarioStatistics;
import de.fosd.jdime.stats.Runtime;
import de.fosd.jdime.stats.Statistics;
import de.fosd.jdime.stats.StatisticsInterface;
import de.fosd.jdime.stats.parser.ParseResult;

import java.security.Permission;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static de.fosd.jdime.stats.Runtime.MERGE_LABEL;
import static de.fosd.jdime.strdump.DumpMode.PLAINTEXT_TREE;

/**
 * Performs a structured merge on <code>FileArtifacts</code>.
 *
 * @author Olaf Lessenich
 */
public class StructuredStrategy extends MergeStrategy<FileArtifact> {

    private static final Logger LOG = Logger.getLogger(StructuredStrategy.class.getCanonicalName());

    private static final String PARSE_LABEL = "parse";
    private static final String SEMISTRUCTURE_LABEL = "semistructure";

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
    @Override @SuppressWarnings("try")
    public void merge(MergeOperation<FileArtifact> operation, MergeContext context) {
        /*
         * The method creates ASTNodeArtifacts from the input files. An ASTNodeStrategy is then applied.
         * The result is pretty printed and possibly written to the output file.
         */

        MergeScenario<FileArtifact> triple = operation.getMergeScenario();

        FileArtifact target = operation.getTarget();
        FileArtifact leftFile = triple.getLeft();
        FileArtifact rightFile = triple.getRight();
        FileArtifact baseFile = triple.getBase();

        String lPath = leftFile.getFile().getPath();
        String bPath = baseFile.getFile().getPath();
        String rPath = rightFile.getFile().getPath();

        System.setSecurityManager(noExitManager);

        LOG.fine(() -> String.format("Merging:%nLeft: %s%nBase: %s%nRight: %s", lPath, bPath, rPath));

        try {
            Runtime parse = new Runtime(PARSE_LABEL);
            Runtime semistructure = new Runtime(SEMISTRUCTURE_LABEL);
            Runtime merge = new Runtime(MERGE_LABEL);

            ASTNodeArtifact left;
            ASTNodeArtifact base;
            ASTNodeArtifact right;

            try (Runtime.Measurement m = parse.time())  {
                left = new ASTNodeArtifact(leftFile);
                base = new ASTNodeArtifact(baseFile);
                right = new ASTNodeArtifact(rightFile);
            }

            if (context.isSemiStructured()) {
                try (Runtime.Measurement m = semistructure.time()) {
                    left = SemiStructuredStrategy.makeSemiStructured(left, context.getSemiStructuredLevel(), leftFile);
                    base = SemiStructuredStrategy.makeSemiStructured(base, context.getSemiStructuredLevel(), baseFile);
                    right = SemiStructuredStrategy.makeSemiStructured(right, context.getSemiStructuredLevel(), rightFile);
                }
            }

            ASTNodeArtifact targetNode = left.copy();

            MergeScenario<ASTNodeArtifact> nodeTriple = new MergeScenario<>(triple.getMergeType(), left, base, right);
            MergeOperation<ASTNodeArtifact> astMergeOp = new MergeOperation<>(nodeTriple, targetNode);

            LOG.finest("Applying an ASTNodeArtifact MergeOperation.");

            try (Runtime.Measurement m = merge.time()) {
                astMergeOp.apply(context);
            }

            // TODO: find clusters of microconflicts and restructure them to larger conflicts
            targetNode.collapseConflicts();

            targetNode.setRevision(MergeScenario.TARGET, true); // TODO do this somewhere else?

            if (!context.isDiffOnly()) {
                target.setContent(targetNode.prettyPrint());
            }

            LOG.fine("Structured merge finished.");
            LOG.fine(() -> String.format("%s merge time was %d ms.", getClass().getSimpleName(), merge.getTimeMS()));

            if (!context.isDiffOnly()) {
                LOG.fine(() -> String.format("Tree dump of target node:%n%s", targetNode.dump(PLAINTEXT_TREE)));
            }

            ASTNodeArtifact finalLeft = left;
            LOG.finest(() -> String.format("Pretty-printing left:%n%s", finalLeft.prettyPrint()));
            ASTNodeArtifact finalRight = right;
            LOG.finest(() -> String.format("Pretty-printing right:%n%s", finalRight.prettyPrint()));

            if (!context.isDiffOnly()) {
                LOG.finest(() -> String.format("Pretty-printing merge result:%n%s", target.getContent()));
            }

            if (context.hasStatistics()) {
                Statistics statistics = context.getStatistics();
                MergeScenarioStatistics scenarioStatistics = new MergeScenarioStatistics(triple);

                if (!context.isDiffOnly()) {
                    ParseResult parseResult = scenarioStatistics.setLineStatistics(target.getContent());

                    if (parseResult.getConflicts() > 0) {
                        scenarioStatistics.getFileStatistics().incrementNumOccurInConflict();
                    }
                }

                scenarioStatistics.add(StatisticsInterface.getASTStatistics(left, right.getRevision()));
                scenarioStatistics.add(StatisticsInterface.getASTStatistics(right, left.getRevision()));
                scenarioStatistics.add(StatisticsInterface.getASTStatistics(targetNode, null));
                Stream.of(parse, semistructure, merge).filter(Runtime::isMeasured).forEach(scenarioStatistics::putRuntime);

                statistics.addScenarioStatistics(scenarioStatistics);
            }
        } finally {
            System.setSecurityManager(systemSecurityManager);
        }
    }
}
