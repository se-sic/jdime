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
 */
package de.fosd.jdime.common.operations;

import java.io.IOException;
import java.util.logging.Logger;

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

	private static final Logger LOG = Logger.getLogger(AddOperation.class.getCanonicalName());

	/**
	 * The <code>Artifact</code> that is added by the operation.
	 */
	private T artifact;

	/**
	 * The output <code>Artifact</code>.
	 */
	private T target;

	private String condition;

	/**
	 * Class constructor.
	 * @param artifact that is added by the operation
	 * @param target output artifact
	 * @param condition presence condition
	 */
	public AddOperation(final T artifact, final T target, String condition) {
		super();
		this.artifact = artifact;
		this.target = target;

		if (condition != null) {
			this.condition = condition;
		}
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

		LOG.fine(() -> "Applying: " + this);

		if (artifact.isChoice()) {
			target.addChild(artifact);
			return;
		}

		if (target != null) {
			assert (target.exists());

			if (context.isConditionalMerge(artifact) && condition != null) {
				T choice = target.createChoiceDummy(condition, artifact);
				assert (choice.isChoice());
				target.addChild(choice);
			} else {
				LOG.fine("no conditions");
				target.addChild((T) artifact.clone());
			}
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

					LOG.fine(childStats::toString);

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
		return getId() + ": " + getName() + " " + artifact + " (" + condition + ")";
	}
}
