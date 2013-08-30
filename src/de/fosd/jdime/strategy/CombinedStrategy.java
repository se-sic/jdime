/*******************************************************************************
 * Copyright (c) 2013 Olaf Lessenich.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Olaf Lessenich - initial API and implementation
 ******************************************************************************/
/**
 * 
 */
package de.fosd.jdime.strategy;

import java.io.IOException;

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

	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(CombinedStrategy.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.strategy.MergeInterface#merge(
	 * de.fosd.jdime.common.operations.MergeOperation,
	 * de.fosd.jdime.common.MergeContext)
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
			target = (FileArtifact) operation.getTarget();
			assert (!target.exists() || target.isEmpty()) 
				: "Would be overwritten: " + target;
		}

		if (LOG.isInfoEnabled()) {
			MergeTriple<FileArtifact> triple = operation.getMergeTriple();
			assert (triple != null);
			assert (triple.isValid()) : "The merge triple is not valid!";
			LOG.info("Merging: " + triple.getLeft().getPath() + " "
					+ triple.getBase().getPath() + " "
					+ triple.getRight().getPath());
		}

		MergeContext subContext = (MergeContext) context.clone();
		subContext.setOutputFile(null);

		if (LOG.isInfoEnabled()) {
			LOG.info("Trying linebased strategy.");
		}

		long cmdStart = System.currentTimeMillis();
		MergeStrategy<FileArtifact> s = new LinebasedStrategy();
		subContext.setMergeStrategy(s);
		subContext.setSaveStats(true);
		s.merge(operation, subContext);

		int conflicts = subContext.getStats().getConflicts();
		if (conflicts > 0) {
			// merge not successful. we need another strategy.
			if (LOG.isInfoEnabled()) {
				String noun = conflicts > 1 ? "conflicts" : "conflict";
				LOG.info("Got " + conflicts + " " + noun
						+ ". Need to use structured strategy.");
			}
			subContext = (MergeContext) context.clone();
			subContext.setOutputFile(null);

			s = new StructuredStrategy();
			subContext.setMergeStrategy(s);
			subContext.setSaveStats(true);
			s.merge(operation, subContext);
		} else {
			if (LOG.isInfoEnabled()) {
				LOG.info("Linebased strategy worked fine.");
			}
		}

		long cmdStop = System.currentTimeMillis();
		long runtime = cmdStop - cmdStart;

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
			if (subContext.hasStats()) {
				Stats stats = context.getStats();
				Stats substats = subContext.getStats();
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
							subscenariostats.getLines(), runtime);
					stats.addScenarioStats(scenariostats);
				}

				context.addStats(substats);
			}
		}

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
	public final void dump(final FileArtifact artifact, final boolean graphical)
			throws IOException {
		throw new NotYetImplementedException();
	}
}
