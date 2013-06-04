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
package de.fosd.jdime.common;

import java.io.FileNotFoundException;
import java.io.IOException;

import de.fosd.jdime.common.operations.MergeOperation;

/**
 * @author Olaf Lessenich
 * 
 */
public abstract class Artifact {

	/**
	 * Revision the artifact belongs to.
	 */
	private Revision revision;

	/**
	 * If true, this artifact is an empty dummy.
	 */
	private boolean emptyDummy = false;

	/**
	 * Children of the artifact.
	 */
	private ArtifactList children = null;

	/**
	 * Parent artifact.
	 */
	private Artifact parent;

	/**
	 * Adds a child.
	 * 
	 * @param child
	 *            child to add
	 * @return added child
	 * @throws IOException
	 *             If an input output exception occurs
	 */
	public abstract Artifact addChild(final Artifact child) throws IOException;

	/**
	 * Copies an @code{Artifact}.
	 * 
	 * @param destination
	 *            destination artifact
	 * @throws IOException
	 *             If an input or output exception occurs.
	 */
	public abstract void copyArtifact(final Artifact destination)
			throws IOException;

	/**
	 * Creates an @code{Artifact}.
	 * 
	 * @param isLeaf
	 *            if true, a leaf type artifact will be created
	 * @throws IOException
	 *             If an input output exception occurs
	 */
	public abstract void createArtifact(boolean isLeaf) throws IOException;

	/**
	 * Returns a dummy @code{Artifact}.
	 * 
	 * @return dummy artifact
	 * @throws FileNotFoundException
	 *             If a file is not found
	 */
	public abstract Artifact createEmptyDummy() throws FileNotFoundException;

	/**
	 * Returns true if this artifact physically exists.
	 * 
	 * @return true if the artifact exists.
	 */
	public abstract boolean exists();

	/**
	 * Returns a child @code{Artifact}.
	 * 
	 * @param otherChild
	 *            artifact
	 * 
	 * @return child if child exists, null otherwise
	 */
	public abstract Artifact getChild(final Artifact otherChild);

	/**
	 * Return child at position i.
	 * 
	 * @param i
	 *            position of child
	 * @return child at position i
	 */
	public final Artifact getChild(final int i) {
		assert (children != null);
		return children.get(i);
	}

	/**
	 * Returns the children of the artifact.
	 * 
	 * @return the children of the artifact
	 */
	public final ArtifactList getChildren() {
		if (children == null) {
			initializeChildren();
		}

		return children;
	}

	/**
	 * Returns identifier.
	 * 
	 * @return identifier
	 */
	public abstract String getId();

	/**
	 * Returns the name of the artifact.
	 * 
	 * @return name of the artifact
	 */
	public abstract String getName();

	/**
	 * Returns the number of children the artifact has.
	 * 
	 * @return number of children
	 */
	public final int getNumChildren() {
		if (children == null) {
			initializeChildren();
		}

		return children == null ? 0 : children.size();
	}

	/**
	 * Returns the parent artifact.
	 * 
	 * @return the parent artifact
	 */
	public final Artifact getParent() {
		return parent;
	}

	/**
	 * Returns the revision the artifact belongs to.
	 * 
	 * @return the revision the artifact belongs to.
	 */
	public final Revision getRevision() {
		return revision;
	}

	/**
	 * Returns true if the artifact has children.
	 * 
	 * @return true if the artifact has children
	 */
	public final boolean hasChildren() {
		return getNumChildren() > 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public abstract int hashCode();

	/**
	 * Initializes the children of the artifact.
	 */
	public abstract void initializeChildren();

	/**
	 * Returns whether the artifact is an empty dummy.
	 * 
	 * @return true, if the artifact is an empty dummy.
	 */
	public final boolean isEmptyDummy() {
		return emptyDummy;
	}

	/**
	 * Returns true, if the artifact is a leaf.
	 * 
	 * @return true, if the artifact is a leaf
	 */
	public abstract boolean isLeaf();

	/**
	 * Performs a merge on the provided merge triple.
	 * 
	 * @param operation
	 *            merge operation
	 * @param context
	 *            merge context
	 * @throws InterruptedException If a thread is interrupted
	 * @throws IOException If an input output exception occurs
	 */
	public abstract void merge(MergeOperation operation, MergeContext context)
			throws IOException, InterruptedException;

	/**
	 * Sets the children of the artifact.
	 * 
	 * @param children
	 *            the new children to set
	 */
	public final void setChildren(final ArtifactList children) {
		this.children = children;
	}

	/**
	 * Set whether the artifact is an empty dummy.
	 * 
	 * @param emptyDummy
	 *            true, if the artifact is an emptyDummy
	 */
	public final void setEmptyDummy(final boolean emptyDummy) {
		this.emptyDummy = emptyDummy;
	}

	/**
	 * Sets the parent artifact.
	 * 
	 * @param parent
	 *            the parent to set
	 */
	public final void setParent(final Artifact parent) {
		this.parent = parent;
	}

	/**
	 * Sets the revision.
	 * 
	 * @param revision
	 *            the revision to set
	 */
	public final void setRevision(final Revision revision) {
		this.revision = revision;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString() {
		return this.getName();
	}

	/**
	 * Read from a BufferedReader and writes to the artifact.
	 * 
	 * @param str
	 *            String to write
	 * x@throws IOException
	 *             If an input output exception occurs
	 */
	public abstract void write(String str) throws IOException;
}
