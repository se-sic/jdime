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

import java.io.IOException;
import java.util.Objects;

import de.fosd.jdime.common.*;
import de.fosd.jdime.common.MergeScenario;
import de.fosd.jdime.stats.Stats;
import de.fosd.jdime.stats.StatsElement;
import org.apache.commons.lang3.ClassUtils;
import java.util.logging.Logger;

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
	 *
	 * @throws IllegalArgumentException
	 * 		if the size of <code>inputArtifacts</code> is invalid
	 * @throws IllegalArgumentException
	 * 		if the artifacts in <code>inputArtifacts</code> produce an invalid <code>MergeScenario</code> according to
	 * 		{@link MergeScenario#isValid()}
	 * @throws IOException
	 * 		if the dummy file used as BaseArtifact in a two-way-merge can not be created
	 */
	public MergeOperation(ArtifactList<T> inputArtifacts, T target) throws IOException {
		Objects.requireNonNull(inputArtifacts, "inputArtifacts must not be null!");

		this.target = target;

		MergeType mergeType;
		T left, base, right;
		int numArtifacts = inputArtifacts.size();

		if (numArtifacts == MergeType.TWOWAY.getNumFiles()) {
			left = inputArtifacts.get(0);
			base = left.createEmptyArtifact();
			right = inputArtifacts.get(1);
			mergeType = MergeType.TWOWAY;
		} else if (numArtifacts == MergeType.THREEWAY.getNumFiles()) {
			left = inputArtifacts.get(0);
			base = inputArtifacts.get(1);
			right = inputArtifacts.get(2);
			mergeType = MergeType.THREEWAY;
		} else {
			String msg = String.format("Invalid number of artifacts (%d) for a MergeOperation.", numArtifacts);
			throw new IllegalArgumentException(msg);
		}

		this.mergeScenario = new MergeScenario<>(mergeType, left, base, right);

		if (!mergeScenario.isValid()) {
			throw new IllegalArgumentException("The artifacts in inputArtifacts produced an invalid MergeScenario.");
		}

		left.setRevision(new Revision(MergeType.THREEWAY.getRevision(0)), true);
		base.setRevision(new Revision(MergeType.THREEWAY.getRevision(1)), true);
		right.setRevision(new Revision(MergeType.THREEWAY.getRevision(2)), true);
	}

	/**
	 * Constructs a new <code>MergeOperation</code> using the given <code>mergeScenario</code> and <code>target</code>.
	 * <code>mergeScenario</code> may be <code>null</code>.
	 *
	 * @param mergeScenario
	 * 		the <code>Artifact</code>s to be merged
	 * @param target
	 * 		the output <code>Artifact</code>
	 *
	 * @throws IllegalArgumentException
	 * 		if <code>mergeScenario</code> is invalid
	 */
	public MergeOperation(MergeScenario<T> mergeScenario, T target) {
		Objects.requireNonNull(mergeScenario, "mergeScenario must not be null!");

		if (!mergeScenario.isValid()) {
			throw new IllegalArgumentException("mergeScenario is invalid.");
		}

		this.mergeScenario = mergeScenario;
		this.target = target;
	}

	@Override
	public void apply(MergeContext context) throws IOException, InterruptedException {
		assert (mergeScenario.getLeft().exists()) : "Left artifact does not exist: " + mergeScenario.getLeft();
		assert (mergeScenario.getRight().exists()) : "Right artifact does not exist: " + mergeScenario.getRight();
		assert (mergeScenario.getBase().isEmpty() || mergeScenario.getBase().exists()) :
				"Base artifact does not exist: " + mergeScenario.getBase();

		LOG.fine(() -> "Applying: " + this);

		if (target != null) {
			assert (target.exists()) : this + ": target " + target.getId()  + " does not exist.";
		}

		mergeScenario.getLeft().merge(this, context);

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
		String mTripleString = mergeScenario.toString(true);
		MergeType mergeType = mergeScenario.getMergeType();

		return String.format("%s: %s %s %s INTO %s", getId(), getName(), mergeType, mTripleString, dst);
	}
}
