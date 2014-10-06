/*******************************************************************************
 * Copyright (C) 2013 Olaf Lessenich.
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
 *     Olaf Lessenich - initial API and implementation
 ******************************************************************************/
package de.fosd.jdime.common.operations;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.ArtifactList;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeTriple;
import de.fosd.jdime.common.MergeType;
import de.fosd.jdime.common.Revision;
import de.fosd.jdime.common.UnsupportedMergeTypeException;
import de.fosd.jdime.stats.Stats;
import de.fosd.jdime.stats.StatsElement;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.commons.lang3.ClassUtils;
import org.apache.log4j.Logger;

/**
 * The operation merges <code>Artifact</code>s.
 * 
 * @author Olaf Lessenich
 * 
 * @param <T>
 *            type of artifact
 * 
 */
public class MergeOperation<T extends Artifact<T>> extends Operation<T> {

	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(ClassUtils.getShortClassName(MergeOperation.class));
	/**
	 * /** The merge triple containing the <code>Artifact</code>s.
	 */
	private MergeTriple<T> mergeTriple;
	/**
	 * Output Artifact.
	 */
	private T target;

	/**
	 * Class constructor.
	 * 
	 * @param inputArtifacts
	 *            input artifacts
	 * @param target
	 *            output artifact
	 * @throws FileNotFoundException
	 *             If a file cannot be found
	 */
	public MergeOperation(final ArtifactList<T> inputArtifacts, final T target)
			throws FileNotFoundException {
		super();
		assert (inputArtifacts != null);
		assert inputArtifacts.size() >= MergeType.MINFILES 
				: "Too few input files!";
		assert inputArtifacts.size() <= MergeType.MAXFILES 
				: "Too many input files!";

		// Determine whether we have to perform a 2-way or a 3-way merge.
		MergeType mergeType =
				inputArtifacts.size() == 2 ? MergeType.TWOWAY
						: MergeType.THREEWAY;

		this.target = target;

		T left, base, right;

		if (mergeType == MergeType.TWOWAY) {
			left = inputArtifacts.get(0);
			base = left.createEmptyDummy();
			right = inputArtifacts.get(1);
		} else if (mergeType == MergeType.THREEWAY) {
			left = inputArtifacts.get(0);
			base = inputArtifacts.get(1);
			right = inputArtifacts.get(2);
		} else {
			throw new UnsupportedMergeTypeException();
		}

		assert (left.getClass().equals(right.getClass())) 
			: "Only artifacts of the same type can be merged";
		assert (base.isEmptyDummy() || base.getClass().equals(left.getClass())) 
			: "Only artifacts of the same type can be merged";

		left.setRevision(new Revision("left"));
		base.setRevision(new Revision("base"));
		right.setRevision(new Revision("right"));

		mergeTriple = new MergeTriple<>(mergeType, left, base, right);
		assert (mergeTriple != null);
		assert (mergeTriple.isValid());
	}

	/**
	 * Class constructor.
	 * 
	 * @param mergeTriple
	 *            triple containing <code>Artifact</code>s
	 * @param target
	 *            output <code>Artifact</code>
	 */
	public MergeOperation(final MergeTriple<T> mergeTriple, final T target) {
		super();
		this.mergeTriple = mergeTriple;
		this.target = target;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.operations.Operation#apply()
	 */
	@Override
	public final void apply(final MergeContext context) throws IOException,
			InterruptedException {
		assert (mergeTriple.getLeft().exists()) 
			: "Left artifact does not exist: " + mergeTriple.getLeft();
		assert (mergeTriple.getRight().exists()) 
			: "Right artifact does not exist: " + mergeTriple.getRight();
		assert (mergeTriple.getBase().isEmptyDummy() || mergeTriple.getBase()
				.exists()) : "Base artifact does not exist: "
				+ mergeTriple.getBase();

		if (LOG.isDebugEnabled()) {
			LOG.debug("Applying: " + this);
		}

		if (target != null && !target.exists()) {
			target.createArtifact(mergeTriple.getLeft().isLeaf());
		}

		mergeTriple.merge(this, context);

		if (context.hasStats()) {
			Stats stats = context.getStats();
			if (stats != null) {
				stats.incrementOperation(this);
				StatsElement element =
						stats.getElement(mergeTriple.getLeft().getStatsKey(
								context));
				element.incrementMerged();
			}
		}
	}

	/**
	 * Returns the merge triple.
	 * 
	 * @return merge triple
	 */
	public final MergeTriple<T> getMergeTriple() {
		return mergeTriple;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.operations.Operation#getName()
	 */
	@Override
	public final String getName() {
		return "MERGE";
	}

	/**
	 * Returns the target @code{Artifact}.
	 * 
	 * @return the target
	 */
	public final T getTarget() {
		return target;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString() {
		assert (mergeTriple != null);
		String dst = target == null ? "" : target.getId();
		return getId() + ": " + getName() + " " + mergeTriple.getMergeType()
				+ " " + mergeTriple.toString(true) + " INTO " + dst;
	}
}
