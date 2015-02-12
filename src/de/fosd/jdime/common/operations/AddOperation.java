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

import de.fosd.jdime.common.ASTNodeArtifact;
import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.LangElem;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.stats.ASTStats;
import de.fosd.jdime.stats.Stats;
import de.fosd.jdime.stats.StatsElement;

/**
 * The operation adds <code>Artifact</code>s.
 *
 * @author Olaf Lessenich
 *
 * @param <T>
 *            type of artifact
 *
 */
public class AddOperation<T extends Artifact<T>> extends Operation<T> {

	private static final Logger LOG = Logger.getLogger(ClassUtils
			.getShortClassName(AddOperation.class));

	/**
	 * The <code>Artifact</code> that is added by the operation.
	 */
	private T artifact;

	/**
	 * The output <code>Artifact</code>.
	 */
	private T target;

	/**
	 * Class constructor.
	 *
	 * @param artifact
	 *            that is added by the operation.
	 * @param target
	 *            output artifact
	 */
	public AddOperation(final T artifact, final T target) {
		super();
		this.artifact = artifact;
		this.target = target;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.operations.Operation#apply()
	 */
	@Override
	public final void apply(final MergeContext context) throws IOException {
		assert (artifact != null);
		assert (artifact.exists()) : "Artifact does not exist: " + artifact;

		if (LOG.isDebugEnabled()) {
			LOG.debug("Applying: " + this);
		}

		if (target != null) {
			if (!target.exists()) {
				target.createArtifact(false);
			}

			assert (target.exists());

			artifact.copyArtifact(target);
		}

		if (context.hasStats()) {
			Stats stats = context.getStats();
			stats.incrementOperation(this);
			StatsElement element = stats.getElement(artifact
					.getStatsKey(context));
			element.incrementAdded();

			if (artifact instanceof FileArtifact) {

				// analyze java files to get statistics
				for (FileArtifact child : ((FileArtifact) artifact)
						.getJavaFiles()) {
					ASTNodeArtifact childAST = new ASTNodeArtifact(child);
					ASTStats childStats = childAST.getStats(null,
							LangElem.TOPLEVELNODE, false);
					if (LOG.isDebugEnabled()) {
						LOG.debug(childStats.toString());
					}

					if (context.isConsecutive()) {
						context.getStats().addRightStats(childStats);
					} else {
						context.getStats().addASTStats(childStats);
					}
				}
			}
		}
	}

	@Override
	public final String getName() {
		return "ADD";
	}

	/**
	 * Returns the target <code>Artifact</code>
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
		return getId() + ": " + getName() + " " + artifact;
	}
}
