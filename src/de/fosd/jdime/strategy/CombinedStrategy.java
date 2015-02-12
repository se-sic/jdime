/*******************************************************************************
 * Copyright (C) 2013, 2014 Olaf Lessenich.
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
 *******************************************************************************/
package de.fosd.jdime.strategy;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.lang3.ClassUtils;
import org.apache.log4j.Logger;

import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeTriple;
import de.fosd.jdime.common.NotYetImplementedException;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.stats.MergeTripleStats;
import de.fosd.jdime.stats.Stats;

/**
 * Performs a structured merge with auto-tuning.
 *
 * @author Olaf Lessenich
 *
 */
public class CombinedStrategy extends MergeStrategy<FileArtifact> {

	private static final Logger LOG = Logger.getLogger(ClassUtils
			.getShortClassName(CombinedStrategy.class));

	/**
	 * TODO: high-level documentation
	 * @param operation
	 * @param context
	 *
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Override
	public final void merge(final MergeOperation<FileArtifact> operation,
			final MergeContext context) throws IOException,
			InterruptedException {
		assert (operation != null);
		assert (context != null);

		context.resetStreams();

		FileArtifact target = null;

		if (operation.getTarget() != null) {
			assert (operation.getTarget() instanceof FileArtifact);
			target = operation.getTarget();
			assert (!target.exists() || target.isEmpty()) : "Would be overwritten: "
					+ target;
		}

		if (LOG.isInfoEnabled()) {
			MergeTriple<FileArtifact> triple = operation.getMergeTriple();
			assert (triple != null);
			assert (triple.isValid()) : "The merge triple is not valid!";
			LOG.info("Merging: " + triple.getLeft().getPath() + " "
					+ triple.getBase().getPath() + " "
					+ triple.getRight().getPath());
		}

		ArrayList<Long> runtimes = new ArrayList<>();
		MergeContext subContext = null;
		Stats substats = null;

		for (int i = 0; i < context.getBenchmarkRuns() + 1
				&& (i == 0 || context.isBenchmark()); i++) {
			long cmdStart = System.currentTimeMillis();
			subContext = (MergeContext) context.clone();
			subContext.setOutputFile(null);

			if (LOG.isInfoEnabled() && i == 0) {
				LOG.info("Trying linebased strategy.");
			}

			MergeStrategy<FileArtifact> s = new LinebasedStrategy();
			subContext.setMergeStrategy(s);
			subContext.setSaveStats(true);
			s.merge(operation, subContext);

			int conflicts = subContext.getStats().getConflicts();
			if (conflicts > 0) {
				// merge not successful. we need another strategy.
				if (LOG.isInfoEnabled() && i == 0) {
					String noun = conflicts > 1 ? "conflicts" : "conflict";
					LOG.info("Got " + conflicts + " " + noun
							+ ". Need to use structured strategy.");
				}

				// clean target file
				if (LOG.isInfoEnabled()) {
					LOG.info("Deleting: " + target);
				}

				if (target != null) {
					boolean isLeaf = target.isLeaf();
					target.remove();
					target.createArtifact(isLeaf);
				}

				subContext = (MergeContext) context.clone();
				subContext.setOutputFile(null);

				s = new StructuredStrategy();
				subContext.setMergeStrategy(s);
				if (i == 0) {
					subContext.setSaveStats(true);
				} else {
					subContext.setSaveStats(false);
					subContext.setBenchmark(true);
					subContext.setBenchmarkRuns(0);
				}
				s.merge(operation, subContext);
			} else {
				if (LOG.isInfoEnabled() && i == 0) {
					LOG.info("Linebased strategy worked fine.");
				}
			}

			if (i == 0) {
				substats = subContext.getStats();
			}

			long runtime = System.currentTimeMillis() - cmdStart;
			runtimes.add(runtime);

			if (LOG.isInfoEnabled() && context.isBenchmark()) {
				if (i == 0) {
					LOG.info("Initial run: " + runtime + " ms");
				} else {
					LOG.info("Run " + i + " of " + context.getBenchmarkRuns()
							+ ": " + runtime + " ms");
				}
			}
		}

		if (context.isBenchmark() && runtimes.size() > 1) {
			// remove first run as it took way longer due to all the counting
			runtimes.remove(0);
		}

		Long runtime = MergeContext.median(runtimes);
		LOG.debug("Combined merge time was " + runtime + " ms.");

		assert (subContext != null);

		if (subContext.hasOutput()) {
			context.append(subContext.getStdIn());
		}

		if (subContext.hasErrors()) {
			context.appendError(subContext.getStdErr());
		}

		// write output
		if (target != null) {
			assert (target.exists());
			target.write(context.getStdIn());
		}

		// add statistical data to context
		if (context.hasStats()) {
			assert (substats != null);

			Stats stats = context.getStats();
			substats.setRuntime(runtime);
			MergeTripleStats subscenariostats = substats.getScenariostats()
					.remove(0);
			assert (substats.getScenariostats().isEmpty());

			if (subscenariostats.hasErrors()) {
				stats.addScenarioStats(subscenariostats);
			} else {
				MergeTripleStats scenariostats = new MergeTripleStats(
						subscenariostats.getTriple(),
						subscenariostats.getConflicts(),
						subscenariostats.getConflictingLines(),
						subscenariostats.getLines(), runtime,
						subscenariostats.getASTStats(),
						subscenariostats.getLeftASTStats(),
						subscenariostats.getRightASTStats());
				stats.addScenarioStats(scenariostats);
			}

			context.addStats(substats);
		}
		System.gc();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.strategy.MergeStrategy#toString()
	 */
	@Override
	public final String toString() {
		return "combined";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.strategy.StatsInterface#createStats()
	 */
	@Override
	public final Stats createStats() {
		return new Stats(new String[] { "directories", "files", "lines",
				"nodes" });
	}

	@Override
	public final String getStatsKey(final FileArtifact artifact) {
		throw new NotYetImplementedException();
	}

	@Override
	public final void dumpTree(final FileArtifact artifact, final boolean graphical)
			throws IOException {
		new StructuredStrategy().dumpTree(artifact, graphical);
	}

	@Override
	public void dumpFile(final FileArtifact artifact, final boolean graphical)
			throws IOException {
		new LinebasedStrategy().dumpFile(artifact, graphical);
	}
}
