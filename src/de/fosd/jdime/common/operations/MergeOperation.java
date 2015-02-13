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
package de.fosd.jdime.common.operations;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.ArtifactList;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeTriple;
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
	 * The <code>MergeTriple</code> containing the <code>Artifact</code>s to be merged.
	 */
	private MergeTriple<T> mergeTriple;

	/**
	 * The <code>Artifact</code> to output the result of the merge to.
	 */
	private T target;

	/**
	 * Constructs a new <code>MergeOperation</code> merging the given <code>inputArtifacts</code>. The result
	 * will be output into <code>target</code>. Neither <code>inputArtifacts</code> nor <code>target</code> may be
	 * <code>null</code>.
	 * <p>
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
	 * 		if the artifacts in <code>inputArtifacts</code> produce an invalid <code>MergeTriple</code> according to
	 * 		{@link MergeTriple#isValid()}
	 * @throws FileNotFoundException
	 * 		if the dummy file used as BaseArtifact in a two-way-merge can not be created
	 */
	public MergeOperation(ArtifactList<T> inputArtifacts, T target) throws FileNotFoundException {
		Objects.requireNonNull(inputArtifacts, "inputArtifacts must not be null!");
		Objects.requireNonNull(target, "target must not be null!");

		this.target = target;

		MergeType mergeType;
		T left, base, right;
		int numArtifacts = inputArtifacts.size();

		if (numArtifacts == MergeType.TWOWAY.getNumFiles()) {
			left = inputArtifacts.get(0);
			base = left.createEmptyDummy();
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

		this.mergeTriple = new MergeTriple<>(mergeType, left, base, right);

		if (!mergeTriple.isValid()) {
			throw new IllegalArgumentException("The artifacts in inputArtifacts produced an invalid MergeTriple.");
		}

		left.setRevision(new Revision(MergeType.THREEWAY.getRevision(0)));
		base.setRevision(new Revision(MergeType.THREEWAY.getRevision(1)));
		right.setRevision(new Revision(MergeType.THREEWAY.getRevision(2)));
	}

	/**
	 * Constructs a new <code>MergeOperation</code> using the given <code>mergeTriple</code> and <code>target</code>.
	 * Neither <code>mergeTriple</code> nor <code>target</code> may be <code>null</code>.
	 *
	 * @param mergeTriple
	 * 		the <code>Artifact</code>s to be merged
	 * @param target
	 * 		the output <code>Artifact</code>
	 *
	 * @throws IllegalArgumentException
	 * 		if <code>mergeTriple</code> is invalid
	 */
	public MergeOperation(MergeTriple<T> mergeTriple, T target) {
		Objects.requireNonNull(mergeTriple, "mergeTriple must not be null!");
		Objects.requireNonNull(target, "target must not be null!");

		if (!mergeTriple.isValid()) {
			throw new IllegalArgumentException("mergeTriple is invalid.");
		}

		this.mergeTriple = mergeTriple;
		this.target = target;
	}

	@Override
	public void apply(MergeContext context) throws IOException, InterruptedException {
		assert (mergeTriple.getLeft().exists()) : "Left artifact does not exist: " + mergeTriple.getLeft();
		assert (mergeTriple.getRight().exists()) : "Right artifact does not exist: " + mergeTriple.getRight();
		assert (mergeTriple.getBase().isEmptyDummy() || mergeTriple.getBase().exists()) :
				"Base artifact does not exist: " + mergeTriple.getBase();

		if (LOG.isDebugEnabled()) {
			LOG.debug("Applying: " + this);
		}

		if (target != null && !target.exists()) {
			target.createArtifact(mergeTriple.getLeft().isLeaf());
		}

		mergeTriple.getLeft().merge(this, context);

		if (context.hasStats()) {
			Stats stats = context.getStats();
			if (stats != null) {
				stats.incrementOperation(this);
				StatsElement element = stats.getElement(mergeTriple.getLeft().getStatsKey(context));
				element.incrementMerged();
			}
		}
	}

	/**
	 * Returns the <code>MergeTriple</code> containing the <code>Artifact</code>s this <code>MergeOperation</code>
	 * is merging.
	 *
	 * @return the <code>MergeTriple</code>
	 */
	public MergeTriple<T> getMergeTriple() {
		return mergeTriple;
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
		assert (mergeTriple != null);
		String dst = target == null ? "" : target.getId();
		return getId() + ": " + getName() + " " + mergeTriple.getMergeType() + " " + mergeTriple.toString(true)
				+ " INTO " + dst;
	}
}
