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
package de.fosd.jdime.common.operations;

import java.io.IOException;

import org.apache.commons.lang3.ClassUtils;
import org.apache.log4j.Logger;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;

/**
 * @author Olaf Lessenich
 *
 * @param <T>
 *            type of artifact
 */
public class ConflictOperation<T extends Artifact<T>> extends Operation<T> {

	private static final Logger LOG = Logger.getLogger(ClassUtils
			.getShortClassName(ConflictOperation.class));
	private T type;
	private T left;
	private T right;

	/**
	 * Output Artifact.
	 */
	private T target;

	/**
	 * Class constructor.
	 *
	 * @param type
	 *            type
	 * @param left
	 *            left alternatives
	 * @param right
	 *            right alternatives
	 * @param target
	 *            target node
	 */
	public ConflictOperation(final T type, final T left, final T right,
			final T target) {
		super();
		this.type = type;
		this.left = left;
		this.right = right;
		this.target = target;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.fosd.jdime.common.operations.Operation#apply(de.fosd.jdime.common.
	 * MergeContext)
	 */
	@Override
	public final void apply(final MergeContext context) throws IOException,
			InterruptedException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Applying: " + this);
		}

		if (target != null) {
			if (!target.exists()) {
				target.createArtifact(false);
			}

			assert (target.exists());
			T conflict = target.createConflictDummy(type, left, right);
			assert (conflict.isConflict());
			conflict.copyArtifact(target);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.operations.Operation#getName()
	 */
	@Override
	public final String getName() {
		return "CONFLICT";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.operations.Operation#toString()
	 */
	@Override
	public final String toString() {
		return getId() + ": " + getName() + " {" + left + "} <~~> {" + right
				+ "}";
	}
}
