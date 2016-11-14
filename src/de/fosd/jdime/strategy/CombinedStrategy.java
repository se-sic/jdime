/**
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2015 University of Passau, Germany
 * <p>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 * <p>
 * Contributors:
 * Olaf Lessenich <lessenic@fim.uni-passau.de>
 * Georg Seibt <seibt@fim.uni-passau.de>
 */
package de.fosd.jdime.strategy;

import java.util.logging.Logger;

import de.fosd.jdime.artifact.file.FileArtifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.config.merge.MergeScenario;
import de.fosd.jdime.operations.MergeOperation;
import de.fosd.jdime.stats.MergeScenarioStatistics;
import de.fosd.jdime.stats.Statistics;

/**
 * Performs a structured merge with auto-tuning.
 *
 * @author Olaf Lessenich
 *
 */
public class CombinedStrategy extends MergeStrategy<FileArtifact> {

    private static final Logger LOG = Logger.getLogger(CombinedStrategy.class.getCanonicalName());

    /**
     * TODO: high-level documentation
     * @param operation the <code>MergeOperation</code> to perform
     * @param context the <code>MergeContext</code>
     */
    @Override
    public void merge(MergeOperation<FileArtifact> operation, MergeContext context) {
        LOG.fine(() -> {
            MergeScenario<FileArtifact> triple = operation.getMergeScenario();
            String leftPath = triple.getLeft().getPath();
            String basePath = triple.getBase().getPath();
            String rightPath = triple.getRight().getPath();

            return String.format("Merging:%nLeft: %s%nBase: %s%nRight: %s", leftPath, basePath, rightPath);
        });

        long startTime = System.currentTimeMillis();

        MergeContext subContext = new MergeContext(context);
        MergeStrategy<FileArtifact> strategy = new LinebasedStrategy();

        subContext.setOutputFile(null);
        subContext.setMergeStrategy(strategy);
        subContext.collectStatistics(true);

        LOG.fine("Trying line based strategy.");

        strategy.merge(operation, subContext);

        if (subContext.getStatistics().hasConflicts()) {
            long conflicts = subContext.getStatistics().getConflictStatistics().getSum();

            LOG.fine(() -> {
                String noun = conflicts > 1 ? "conflicts" : "conflict";
                return String.format("Got %d %s. Need to use structured strategy.", conflicts, noun);
            });

            subContext = new MergeContext(context);
            strategy = new StructuredStrategy();

            subContext.setMergeStrategy(strategy);
            subContext.collectStatistics(true);

            strategy.merge(operation, subContext);
        } else {
            LOG.fine("Line based strategy worked fine.");
        }

        long runtime = System.currentTimeMillis() - startTime;
        LOG.fine(() -> String.format("Combined merge time was %d ms.", runtime));

        if (context.hasStatistics()) {
            Statistics statistics = context.getStatistics();
            Statistics subStatistics = subContext.getStatistics();
            MergeScenarioStatistics scenarioStats = subStatistics.getScenarioStatistics(operation.getMergeScenario());

            scenarioStats.setRuntime(runtime);
            statistics.addScenarioStatistics(scenarioStats);
        }
    }
}
