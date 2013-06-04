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

import java.io.IOException;

import org.apache.log4j.Logger;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;

/**
 * The operation adds <code>Artifact</code>s.
 * 
 * @author Olaf Lessenich
 * 
 * @param <T> type of artifact
 * 
 */
public class AddOperation<T extends Artifact<T>> extends Operation<T> {
	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(AddOperation.class);

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
		this.artifact = artifact;
		this.target = target;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.operations.Operation#apply()
	 */
	@Override
	public final void apply(final MergeContext<T> context) throws IOException {
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

	}

	@Override
	public final String getName() {
		return "ADD";
	}

	/**
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
		return getName() + " " + artifact;
	}
				
}
