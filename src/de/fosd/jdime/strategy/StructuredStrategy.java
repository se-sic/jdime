/*******************************************************************************
 * Copyright (C) 2013-2015 Olaf Lessenich.
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
 *******************************************************************************/
package de.fosd.jdime.strategy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import de.fosd.jdime.common.ASTNodeArtifact;
import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.LangElem;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeTriple;
import de.fosd.jdime.common.NotYetImplementedException;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.stats.ASTStats;
import de.fosd.jdime.stats.MergeTripleStats;
import de.fosd.jdime.stats.Stats;
import de.fosd.jdime.stats.StatsElement;
import org.apache.commons.lang3.ClassUtils;
import org.apache.log4j.Logger;

/**
 * Performs a structured merge on <code>FileArtifacts</code>.
 *
 * @author Olaf Lessenich
 */
public class StructuredStrategy extends MergeStrategy<FileArtifact> {

	private static final Logger LOG = Logger.getLogger(ClassUtils.getShortClassName(StructuredStrategy.class));

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
	 * @param operation
	 * @param context
	 */
	@Override
	public final void merge(MergeOperation<FileArtifact> operation, MergeContext context) {

		assert (operation != null);
		assert (context != null);

		MergeTriple<FileArtifact> triple = operation.getMergeTriple();

		assert (triple != null);
		assert (triple.isValid()) : "The merge triple is not valid!";

		FileArtifact leftFile = triple.getLeft();
		FileArtifact rightFile = triple.getRight();
		FileArtifact baseFile = triple.getBase();
		String lPath = leftFile.getPath();
		String bPath = baseFile.getPath();
		String rPath = rightFile.getPath();
		
		assert (leftFile.exists() && !leftFile.isDirectory());
		assert ((baseFile.exists() && !baseFile.isDirectory()) || baseFile.isEmptyDummy());
		assert (rightFile.exists() && !rightFile.isDirectory());

		context.resetStreams();

		FileArtifact target = operation.getTarget();

		if (!context.isDiffOnly() && target != null) {
			assert (!target.exists() || target.isEmpty()) : "Would be overwritten: " + target;
		}
		
		/* ASTNodeArtifacts are created from the input files.
		 * Then, a ASTNodeStrategy can be applied.
		 * The result is pretty printed and can be written into the output file.
		 */
		ASTNodeArtifact left, base, right;
		ArrayList<Long> runtimes = new ArrayList<>();
		MergeContext mergeContext;
		int conflicts = 0;
		int loc = 0;
		int cloc = 0;
		ASTStats astStats = null;
		ASTStats leftStats = null;
		ASTStats rightStats = null;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("Merging:%nLeft: %s%nBase: %s%nRight: %s", lPath, bPath, rPath));
		}
		
		try {
			for (int i = 0; i < context.getBenchmarkRuns() + 1 && (i == 0 || context.isBenchmark()); i++) {
				if (i == 0 && (!context.isBenchmark() || context.hasStats())) {
					mergeContext = context;
				} else {
					mergeContext = (MergeContext) context.clone();
					mergeContext.setSaveStats(false);
					mergeContext.setOutputFile(null);
				}

				long cmdStart = System.currentTimeMillis();

				left = new ASTNodeArtifact(leftFile);
				base = new ASTNodeArtifact(baseFile);
				right = new ASTNodeArtifact(rightFile);

				ASTNodeArtifact targetNode = ASTNodeArtifact.createProgram(left);
				targetNode.setRevision(left.getRevision());
				targetNode.forceRenumbering();

				if (LOG.isTraceEnabled()) {
					LOG.trace("target.dumpTree(:");
					System.out.println(targetNode.dumpTree());
				}

				MergeTriple<ASTNodeArtifact> nodeTriple = new MergeTriple<>(triple.getMergeType(), left, base, right);

				MergeOperation<ASTNodeArtifact> astMergeOp = new MergeOperation<>(nodeTriple, targetNode);

				if (LOG.isTraceEnabled()) {
					LOG.trace("ASTMOperation.apply(context)");
				}

				astMergeOp.apply(mergeContext);

				if (i == 0 && (!context.isBenchmark() || context.hasStats())) {
					if (LOG.isTraceEnabled()) {
						LOG.trace("Structured merge finished.");

						if (!context.isDiffOnly()) {
							LOG.trace("target.dumpTree():");
							System.out.println(targetNode.dumpTree());
						}

						LOG.trace("Pretty-printing left:");
						System.out.println(left.prettyPrint());
						LOG.trace("Pretty-printing right:");
						System.out.println(right.prettyPrint());

						if (!context.isDiffOnly()) {
							LOG.trace("Pretty-printing merge:");
							if (mergeContext.isQuiet()) {
								System.out.print(targetNode.prettyPrint());
							}
						}
					}

					if (!context.isDiffOnly()) {
						try (
								// process input stream
								BufferedReader buf = new BufferedReader(new StringReader(targetNode.prettyPrint()))) {
							boolean conflict = false;
							boolean afterconflict = false;
							boolean inleft = false;
							boolean inright = false;

							int tmp = 0;
							String line;
							StringBuffer leftlines = null;
							StringBuffer rightlines = null;

							while ((line = buf.readLine()) != null) {
								if (line.matches("^$") || line.matches("^\\s*$")) {
									// skip empty lines
									if (!conflict && !afterconflict) {
										mergeContext.appendLine(line);
									}
									continue;
								}

								if (line.matches("^\\s*<<<<<<<.*")) {
									conflict = true;
									tmp = cloc;
									conflicts++;
									inleft = true;

									if (!afterconflict) {
										// new conflict or new chain of
										// conflicts
										leftlines = new StringBuffer();
										rightlines = new StringBuffer();
									} else {
										// is directly after a previous conflict
										// lets merge them
										conflicts--;
									}
								} else if (line.matches("^\\s*=======.*")) {
									inleft = false;
									inright = true;
								} else if (line.matches("^\\s*>>>>>>>.*")) {
									conflict = false;
									afterconflict = true;
									if (tmp == cloc) {
										// only empty lines
										conflicts--;
									}
									inright = false;
								} else {
									loc++;
									if (conflict) {
										cloc++;
										if (inleft) {
											assert (leftlines != null);
											leftlines.append(line).append(System.lineSeparator());
										} else if (inright) {
											assert (rightlines != null);
											rightlines.append(line).append(System.lineSeparator());
										}
									} else {
										if (afterconflict) {
											assert (leftlines != null);
											assert (rightlines != null);
											// need to print the previous
											// conflict(s)
											mergeContext.appendLine("<<<<<<< " + lPath);
											mergeContext.append(leftlines.toString());
											mergeContext.appendLine("=======");
											mergeContext.append(rightlines.toString());
											mergeContext.appendLine(">>>>>>> " + rPath);
										}
										afterconflict = false;
										mergeContext.appendLine(line);
									}
								}
							}
						}
					}
				}

				long runtime = System.currentTimeMillis() - cmdStart;
				runtimes.add(runtime);

				// collect stats
				leftStats = left.getStats(right.getRevision(), LangElem.TOPLEVELNODE, false);
				rightStats = right.getStats(left.getRevision(), LangElem.TOPLEVELNODE, false);
				ASTStats targetStats = targetNode.getStats(null, LangElem.TOPLEVELNODE, false);

				assert (leftStats.getDiffStats(LangElem.NODE.toString()).getMatches() == rightStats
						.getDiffStats(LangElem.NODE.toString()).getMatches()) :
						"Number of matches should be equal in left and " + "right revision.";

				astStats = ASTStats.add(leftStats, rightStats);
				astStats.setConflicts(targetStats);

				if (LOG.isDebugEnabled() && context.hasStats()) {
					System.out.println("---------- left ----------");
					System.out.println(leftStats);
					System.out.println("---------- right ----------");
					System.out.println(rightStats);
					System.out.println("---------- target ----------");
					System.out.println(targetStats);
				}

				if (LOG.isDebugEnabled()) {
					String sep = " / ";
					int nodes = astStats.getDiffStats(LangElem.NODE.toString()).getElements();
					int matches = astStats.getDiffStats(LangElem.NODE.toString()).getMatches();
					int changes = astStats.getDiffStats(LangElem.NODE.toString()).getAdded();
					int removals = astStats.getDiffStats(LangElem.NODE.toString()).getDeleted();
					int conflictnodes = astStats.getDiffStats(LangElem.NODE.toString()).getConflicting();
					LOG.info("Absolute (nodes" + sep + "matches" + sep + "changes" + sep + "removals" + sep
							+ "conflicts): ");
					LOG.info(nodes + sep + matches + sep + changes + sep + removals + sep + conflictnodes);

					if (nodes > 0) {
						LOG.info("Relative (nodes" + sep + "matches" + sep + "changes" + sep + "removals" + sep
								+ "conflicts): ");
						LOG.info(100.0 + sep + 100.0 * matches / nodes + sep + 100.0 * changes / nodes + sep
								+ 100.0 * removals / nodes + sep + 100.0 * conflictnodes / nodes);
					}
				}

				if (context.hasStats()) {
					Stats stats = context.getStats();
					stats.addASTStats(astStats);
					stats.addLeftStats(leftStats);
					stats.addRightStats(rightStats);
				}

				if (LOG.isInfoEnabled() && context.isBenchmark() && context.hasStats()) {
					if (i == 0) {
						LOG.info("Initial run: " + runtime + " ms");
					} else {
						LOG.info("Run " + i + " of " + context.getBenchmarkRuns() + ": " + runtime + " ms");
					}
				}
			}
			if (context.isBenchmark() && runtimes.size() > 1) {
				// remove first run as it took way longer due to all the
				// counting
				runtimes.remove(0);
			}

			Long runtime = MergeContext.median(runtimes);
			LOG.debug("Structured merge time was " + runtime + " ms.");

			if (context.hasErrors()) {
				System.err.println(context.getStdErr());
			}

			// write output
			if (target != null) {
				assert (target.exists());
				target.write(context.getStdIn());
			}
			// add statistical data to context
			if (context.hasStats()) {
				assert (cloc <= loc);

				Stats stats = context.getStats();
				StatsElement linesElement = stats.getElement("lines");
				assert (linesElement != null);
				StatsElement newElement = new StatsElement();
				newElement.setMerged(loc);
				newElement.setConflicting(cloc);
				linesElement.addStatsElement(newElement);

				if (conflicts > 0) {
					assert (cloc > 0);
					stats.addConflicts(conflicts);
					StatsElement filesElement = stats.getElement("files");
					assert (filesElement != null);
					filesElement.incrementConflicting();
				} else {
					assert (cloc == 0);
				}

				stats.increaseRuntime(runtime);

				assert (leftStats != null);
				assert (rightStats != null);

				MergeTripleStats scenariostats =
						new MergeTripleStats(triple, conflicts, cloc, loc, runtime, astStats, leftStats, rightStats);
				stats.addScenarioStats(scenariostats);
			}
		} catch (Throwable t) {
			LOG.fatal(String.format("Exception while merging:%nLeft: %s%nBase: %s%nRight: %s", lPath, bPath, rPath), t);
			
			if (!context.isKeepGoing()) {
				throw new Error(t);
			} else {
				if (context.hasStats()) {
					MergeTripleStats scenarioStats = new MergeTripleStats(triple, t.toString());
					context.getStats().addScenarioStats(scenarioStats);
				}
			}
		}
	}

	@Override
	public final String toString() {
		return "structured";
	}

	@Override
	public final Stats createStats() {
		return new Stats(new String[] {"directories", "files", "lines", "nodes"});
	}

	@Override
	public final String getStatsKey(FileArtifact artifact) {
		// FIXME: remove me when implementation is complete!
		throw new NotYetImplementedException("StructuredStrategy: Implement me!");
	}

	@Override
	public final void dumpTree(FileArtifact artifact, boolean graphical) throws IOException {
		new ASTNodeStrategy().dumpTree(new ASTNodeArtifact(artifact), graphical);
	}

	@Override
	public void dumpFile(FileArtifact artifact, boolean graphical) throws IOException {
		new ASTNodeStrategy().dumpFile(new ASTNodeArtifact(artifact), graphical);
	}
}
