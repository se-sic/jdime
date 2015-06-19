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
package de.fosd.jdime.common.operations;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.ArtifactList;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeScenario;
import de.fosd.jdime.common.MergeType;
import de.fosd.jdime.common.Revision;
import de.fosd.jdime.stats.Stats;
import de.fosd.jdime.stats.StatsElement;
import org.apache.commons.lang3.ClassUtils;
import org.apache.log4j.Logger;

/**
 * The operation merges <code>Artifact</code>s.
 *
 * @param <T>
 * 		type of artifact
 *
 * @author Olaf Lessenich
 */
public class MergeOperation<T extends Artifact<T>> extends Operation<T> {

	private static final Logger LOG = Logger.getLogger(ClassUtils.getShortClassName(MergeOperation.class));

	/**
	 * The <code>MergeScenario</code> containing the <code>Artifact</code>s to be merged.
	 */
	private MergeScenario<T> mergeScenario;

	/**
	 * The <code>Artifact</code> to output the result of the merge to.
	 */
	private T target;

	private boolean nway = false;
	private String leftCondition;
	private String rightCondition;

	/**
	 * Constructs a new <code>MergeOperation</code> merging the given <code>inputArtifacts</code>. The result
	 * will be output into <code>target</code> if output is enabled. <code>inputArtifacts</code> may not be
	 * <code>null</code>. <br><br>
	 *
	 * <code>inputArtifacts</code> must have either two or three elements which will be interpreted as
	 * [LeftArtifact, (BaseArtifact,) RightArtifact]. A two-way-merge will be performed for a list of length 2, a
	 * three-way-merge for one of length three.
	 *
	 * @param inputArtifacts
	 * 		the input artifacts
	 * @param target
	 * 		the output artifact
	 * @param leftCondition condition for left alternative
	 * @param rightCondition condition for right alternative
	 *
	 * @param nway
	 * @throws IllegalArgumentException
	 * 		if the size of <code>inputArtifacts</code> is invalid
	 * @throws IllegalArgumentException
	 * 		if the artifacts in <code>inputArtifacts</code> produce an invalid <code>MergeScenario</code> according to
	 * 		{@link MergeScenario#isValid()}
	 * @throws FileNotFoundException
	 * 		if the dummy file used as BaseArtifact in a two-way-merge can not be created
	 */
	public MergeOperation(ArtifactList<T> inputArtifacts, T target, String leftCondition,
						  String rightCondition, boolean nway) throws FileNotFoundException {
		Objects.requireNonNull(inputArtifacts, "inputArtifacts must not be null!");

		this.target = target;

		MergeType mergeType;
		T left, base, right;
		int numArtifacts = inputArtifacts.size();

		if (numArtifacts < MergeType.MINFILES) {
			String msg = String.format("Invalid number of artifacts (%d) for a MergeOperation.", numArtifacts);
			throw new IllegalArgumentException(msg);
		}

		if (nway) {
			mergeType = MergeType.NWAY;
			this.mergeScenario = new MergeScenario<>(mergeType, inputArtifacts);
			LOG.trace("Created N-way scenario");
		} else {

			if (numArtifacts == MergeType.TWOWAY_FILES) {
				left = inputArtifacts.get(0);
				base = left.createEmptyArtifact();
				right = inputArtifacts.get(1);
				mergeType = MergeType.TWOWAY;
				LOG.trace("Created TWO-way scenario");
			} else if (numArtifacts == MergeType.THREEWAY_FILES) {
				left = inputArtifacts.get(0);
				base = inputArtifacts.get(1);
				right = inputArtifacts.get(2);
				mergeType = MergeType.THREEWAY;
				LOG.trace("Created THREE-way scenario");
			} else {
				String msg = String.format("Invalid number of artifacts (%d) for a MergeOperation.", numArtifacts);
				throw new IllegalArgumentException(msg);
			}

			this.mergeScenario = new MergeScenario<>(mergeType, left, base, right);

			if (!mergeScenario.isValid()) {
				throw new IllegalArgumentException("The artifacts in inputArtifacts produced an invalid MergeScenario.");
			}
		}

		if (leftCondition != null || rightCondition != null) {
			this.leftCondition = leftCondition;
			this.rightCondition = rightCondition;
		}
	}

	/**
	 * Constructs a new <code>MergeOperation</code> using the given <code>mergeScenario</code> and <code>target</code>.
	 * <code>mergeScenario</code> may be <code>null</code>.
	 *
	 * @param mergeScenario
	 * 		the <code>Artifact</code>s to be merged
	 * @param target
	 * 		the output <code>Artifact</code>
	 * @param leftCondition condition for left alternative
	 * @param rightCondition condition for right alternative
	 *
	 * @throws IllegalArgumentException
	 * 		if <code>mergeScenario</code> is invalid
	 */
	public MergeOperation(MergeScenario<T> mergeScenario, T target, String leftCondition, String rightCondition) {
		Objects.requireNonNull(mergeScenario, "mergeScenario must not be null!");

		if (!mergeScenario.isValid()) {
			throw new IllegalArgumentException("mergeScenario is invalid.");
		}

		this.mergeScenario = mergeScenario;
		this.target = target;

		if (leftCondition != null || rightCondition != null) {
			this.leftCondition = leftCondition;
			this.rightCondition = rightCondition;
		}
	}

	@Override
	public void apply(MergeContext context) throws IOException, InterruptedException {
		if (!context.isConditionalMerge(mergeScenario.getLeft())) {
			assert (mergeScenario.getLeft().exists()) : "Left artifact does not exist: " + mergeScenario.getLeft();
			assert (mergeScenario.getRight().exists()) : "Right artifact does not exist: " + mergeScenario.getRight();
			assert (mergeScenario.getBase().isEmpty() || mergeScenario.getBase().exists()) :
					"Base artifact does not exist: " + mergeScenario.getBase();
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("Applying: " + this);
		}

		if (target != null && !target.exists()) {
			target.createArtifact(mergeScenario.getLeft().isLeaf());
		}

		if (LOG.isTraceEnabled() && target != null && !target.isEmpty()) {
			LOG.trace("Print target before merging:");
			LOG.trace(target.dumpRootTree());
		}

		mergeScenario.run(this, context);

		if (context.hasStats()) {
			Stats stats = context.getStats();
			if (stats != null) {
				stats.incrementOperation(this);
				StatsElement element = stats.getElement(mergeScenario.getLeft().getStatsKey(context));
				element.incrementMerged();
			}
		}
	}

	/**
	 * Returns the <code>MergeScenario</code> containing the <code>Artifact</code>s this <code>MergeOperation</code>
	 * is merging.
	 *
	 * @return the <code>MergeScenario</code>
	 */
	public MergeScenario<T> getMergeScenario() {
		return mergeScenario;
	}

	@Override
	public String getName() {
		return "MERGE";
	}

	/**
	 * Returns the target @code{Artifact}.
	 *
	 * @return the target
	 */
	public T getTarget() {
		return target;
	}

	@Override
	public String toString() {
		String dst = target == null ? "" : target.getId();
		String mScenarioString = mergeScenario.toString(true);
		MergeType mergeType = mergeScenario.getMergeType();

		return String.format("%s: %s %s %s INTO %s", getId(), getName(), mergeType, mScenarioString, dst);
	}
}
