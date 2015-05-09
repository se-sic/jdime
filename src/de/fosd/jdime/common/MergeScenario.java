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
package de.fosd.jdime.common;

import de.fosd.jdime.common.operations.MergeOperation;

import java.io.IOException;
import java.util.LinkedHashMap;

/**
 * This class represents a merge scenario.
 *
 * @param <T> type of artifact
 * @author Olaf Lessenich
 */
public class MergeScenario<T extends Artifact<T>> {

	/**
	 * Type of merge.
	 */
	private MergeType mergeType;

	private LinkedHashMap<Revision, T> artifacts;

	private Revision leftRev = new Revision("left");
	private Revision baseRev = new Revision("base");
	private Revision rightRev = new Revision("right");

	/**
	 * Creates a new merge scenario.
	 *
	 * @param mergeType type of merge
	 * @param left      artifact
	 * @param base      artifact
	 * @param right     artifact
	 */
	public MergeScenario(final MergeType mergeType, final T left, final T base, final T right) {
		this.artifacts = new LinkedHashMap<>();
		this.mergeType = mergeType;
		this.artifacts.put(this.leftRev, left);
		this.artifacts.put(this.baseRev, base);
		this.artifacts.put(this.rightRev, right);
	}

	/**
	 * Creates a new merge scenario.
	 *
	 * @param mergeType      type of merge
	 * @param inputArtifacts artifacts to merge
	 */
	public MergeScenario(final MergeType mergeType, ArtifactList<T> inputArtifacts) {
		this.artifacts = new LinkedHashMap<>();
		this.mergeType = mergeType;

		for (T artifact : inputArtifacts) {
			artifacts.put(artifact.getRevision(), artifact);
		}
	}

	/**
	 * Returns the baseRev artifact.
	 *
	 * @return the baseRev
	 */
	public final T getBase() {
		return artifacts.get(baseRev);
	}

	/**
	 * Returns the leftRev artifact.
	 *
	 * @return the leftRev
	 */
	public final T getLeft() { return artifacts.get(leftRev); }

	/**
	 * Returns the type of merge.
	 *
	 * @return type of merge
	 */
	public final MergeType getMergeType() {
		return mergeType;
	}

	/**
	 * Returns the rightRev artifact.
	 *
	 * @return the rightRev
	 */
	public final T getRight() {
		return artifacts.get(rightRev);
	}

	/**
	 * Returns whether this is a valid merge scenario.
	 *
	 * @return true if the merge scenario is valid.
	 */
	public final boolean isValid() {
		T left = artifacts.get(leftRev);
		T base = artifacts.get(baseRev);
		T right = artifacts.get(rightRev);
		return left != null && base != null && right != null
				&& left.getClass().equals(right.getClass())
				&& (base.isEmptyDummy() || base.getClass().equals(
				left.getClass()));
	}

	/**
	 * Run the merge scenario.
	 *
	 * @param mergeOperation merge operation
	 * @param context merge context
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void run(final MergeOperation mergeOperation, final MergeContext context) throws IOException, InterruptedException {
		// FIXME: I think this could be done easier. It's just too fucking ugly.
		//        We need the first element that was inserted and run the merge on it.
		artifacts.get(artifacts.keySet().iterator().next()).merge(mergeOperation, context);
	}

	/**
	 * Sets the baseRev artifact.
	 *
	 * @param base the baseRev to set
	 */
	public final void setBase(final T base) {
		artifacts.put(baseRev, base);
	}

	/**
	 * Sets the leftRev artifact.
	 *
	 * @param left the leftRev to set
	 */
	public final void setLeft(final T left) {
		artifacts.put(leftRev, left);
	}

	/**
	 * Sets the rightRev artifact.
	 *
	 * @param right the rightRev to set
	 */
	public final void setRight(final T right) {
		artifacts.put(rightRev, right);
	}

	/**
	 * Returns a String representing the MergeScenario separated by whitespace.
	 *
	 * @return String representation
	 */
	@Override
	public final String toString() {
		return toString(" ", false);
	}

	/**
	 * Returns a String representing the MergeScenario separated by whitespace,
	 * omitting empty dummy files.
	 *
	 * @param humanReadable do not print dummy files if true
	 * @return String representation
	 */
	public final String toString(final boolean humanReadable) {
		return toString(" ", humanReadable);
	}

	/**
	 * Returns a String representing the MergeScenario.
	 *
	 * @param sep           separator
	 * @param humanReadable do not print dummy files if true
	 * @return String representation
	 */
	public final String toString(final String sep, final boolean humanReadable) {
		T left = artifacts.get(leftRev);
		T base = artifacts.get(baseRev);
		T right = artifacts.get(rightRev);
		StringBuilder sb = new StringBuilder("");
		sb.append(left.getId()).append(sep);

		if (!humanReadable || !base.isEmptyDummy()) {
			sb.append(base.getId()).append(sep);
		}

		sb.append(right.getId());
		return sb.toString();
	}

	/**
	 * Returns a list containing all three revisions. Empty dummies for baseRev are
	 * included.
	 *
	 * @return list of artifacts
	 */
	public final ArtifactList<T> getList() {
		ArtifactList<T> list = new ArtifactList<>();
		list.add(artifacts.get(leftRev));
		list.add(artifacts.get(baseRev));
		list.add(artifacts.get(rightRev));
		return list;
	}
}
