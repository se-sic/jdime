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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import de.fosd.jdime.artifact.file.FileArtifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.config.merge.MergeScenario;
import de.fosd.jdime.operations.MergeOperation;
import de.fosd.jdime.stats.MergeScenarioStatistics;
import de.fosd.jdime.stats.Runtime;
import de.fosd.jdime.stats.Statistics;

import static de.fosd.jdime.stats.Runtime.MERGE_LABEL;

/**
 * Performs a structured merge with auto-tuning.
 *
 * @author Olaf Lessenich
 *
 */
public class CombinedStrategy extends MergeStrategy<FileArtifact> {

    private static final Logger LOG = Logger.getLogger(CombinedStrategy.class.getCanonicalName());

    private List<MergeStrategy<FileArtifact>> strategies;

    /**
     * Constructs a new {@link CombinedStrategy} combining the given {@link MergeStrategy MergeStrategies}.
     *
     * @param strategies the {@link MergeStrategy MergeStrategies} to combine
     */
    public CombinedStrategy(List<MergeStrategy<FileArtifact>> strategies) {
        Objects.requireNonNull(strategies, "The list of merge strategies may not be null.");
        this.strategies = strategies;
    }

    /**
     * TODO: high-level documentation
     * @param operation the <code>MergeOperation</code> to perform
     * @param context the <code>MergeContext</code>
     */
    @Override @SuppressWarnings("try")
    public void merge(MergeOperation<FileArtifact> operation, MergeContext context) {
        LOG.fine(() -> {
            MergeScenario<FileArtifact> triple = operation.getMergeScenario();
            String leftPath = triple.getLeft().getFile().getPath();
            String basePath = triple.getBase().getFile().getPath();
            String rightPath = triple.getRight().getFile().getPath();

            return String.format("Merging:%nLeft: %s%nBase: %s%nRight: %s", leftPath, basePath, rightPath);
        });

        MergeContext subContext = null;

        Runtime runtime;
        List<Runtime> runtimes = new ArrayList<>();

        runtime = new Runtime(MERGE_LABEL);
        runtimes.add(runtime);

        Runtime.Measurement mergeMeasurement = runtime.time();

        for (MergeStrategy<FileArtifact> strategy : strategies) {
            subContext = new MergeContext(context);

            subContext.setMergeStrategy(strategy);

            subContext.collectStatistics(true);
            subContext.getStatistics().removeScenarioStatistics(operation.getMergeScenario());

            runtime = new Runtime(strategy.toString());
            runtimes.add(runtime);

            try (Runtime.Measurement m = runtime.time()) {
                strategy.merge(operation, subContext);
            }

            Statistics stats = subContext.getStatistics();

            if (stats.hasConflicts()) {
                long conflicts = subContext.getStatistics().getConflictStatistics().getSum();

                LOG.fine(() -> {
                    String noun = conflicts > 1 ? "conflicts" : "conflict";
                    return String.format("%s produced %d %s.", strategy, conflicts, noun);
                });
            } else {
                LOG.fine(() -> strategy + " produced no conflicts.");
                break;
            }
        }

        long mergeTime = mergeMeasurement.stop();
        LOG.fine(() -> String.format("Combined merge time was %d ms.", mergeTime));

        if (subContext != null && context.hasStatistics()) {
            Statistics statistics = context.getStatistics();
            Statistics subStatistics = subContext.getStatistics();
            MergeScenarioStatistics scenarioStats = subStatistics.getScenarioStatistics(operation.getMergeScenario());

            runtimes.forEach(scenarioStats::putRuntime);

            statistics.addScenarioStatistics(scenarioStats);
        }
    }
}
