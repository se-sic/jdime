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
package de.fosd.jdime.common.operations;

import org.apache.log4j.Logger;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.stats.Stats;
import de.fosd.jdime.stats.StatsElement;

/**
 * The operation deletes <code>Artifact</code>s.
 * 
 * @author Olaf Lessenich
 * 
 * @param <T>
 *            type of artifact
 * 
 */
public class DeleteOperation<T extends Artifact<T>> extends Operation<T> {

	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(DeleteOperation.class);

	/**
	 * The <code>Artifact</code> that is deleted by the operation.
	 */
	private T artifact;

	/**
	 * Class constructor.
	 * 
	 * @param artifact
	 *            that is deleted by the operation
	 */
	public DeleteOperation(final T artifact) {
		super();
		this.artifact = artifact;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.operations.Operation#apply()
	 */
	@Override
	public final void apply(final MergeContext context) {
		assert (artifact != null);
		assert (artifact.exists()) : "Artifact does not exist: " + artifact;

		if (LOG.isDebugEnabled()) {
			LOG.debug("Applying: " + this);
		}

		if (context.hasStats()) {
			Stats stats = context.getStats();
			stats.incrementOperation(this);
			StatsElement element = stats.getElement(
					artifact.getStatsKey(context));
			element.incrementDeleted();
		}
	}

	@Override
	public final String getName() {
		return "DELETE";
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
