/**
 * 
 */
package de.fosd.jdime.common.operations;

import java.io.IOException;

import org.apache.log4j.Logger;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.ArtifactList;
import de.fosd.jdime.common.MergeContext;

/**
 * @author Olaf Lessenich
 * 
 * @param <T>
 *            type of artifact
 */
public class ConflictOperation<T extends Artifact<T>> extends Operation<T> {

	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(ConflictOperation.class);

	/**
	 * 
	 */
	private T type;

	/**
	 * 
	 */
	private T left;

	/**
	 * 
	 */
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
