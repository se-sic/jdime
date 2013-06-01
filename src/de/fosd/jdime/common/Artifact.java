/**
 * 
 */
package de.fosd.jdime.common;

import java.io.BufferedReader;
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
	 * Returns true, if the artifact is a leaf.
	 * 
	 * @return true, if the artifact is a leaf
	 */
	public abstract boolean isLeaf();

	/**
	 * Children of the artifact.
	 */
	private ArtifactList children = null;

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
	 * Sets the children of the artifact.
	 * 
	 * @param children
	 *            the new children to set
	 */
	public final void setChildren(final ArtifactList children) {
		this.children = children;
	}

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
	 * Returns true if the artifact has children.
	 * 
	 * @return true if the artifact has children
	 */
	public final boolean hasChildren() {
		return getNumChildren() > 0;
	}

	/**
	 * @param otherChild
	 *            artifact
	 * 
	 * @return child if child is contained, else otherwise
	 */
	public abstract Artifact getChild(final Artifact otherChild);

	/**
	 * Initializes the children of the artifact.
	 */
	public abstract void initializeChildren();

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
	 * Set whether the artifact is an empty dummy.
	 * 
	 * @param emptyDummy
	 *            true, if the artifact is an emptyDummy
	 */
	public final void setEmptyDummy(final boolean emptyDummy) {
		this.emptyDummy = emptyDummy;
	}

	/**
	 * Returns whether the artifact is an empty dummy.
	 * 
	 * @return true, if the artifact is an empty dummy.
	 */
	public final boolean isEmptyDummy() {
		return emptyDummy;
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
	 * @param revision
	 *            the revision to set
	 */
	public final void setRevision(final Revision revision) {
		this.revision = revision;
	}

	/**
	 * Parent artifact.
	 */
	private Artifact parent;

	/**
	 * Returns the parent artifact.
	 * 
	 * @return the parent artifact
	 */
	public final Artifact getParent() {
		return parent;
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
	 * Returns the name of the artifact.
	 * 
	 * @return name of the artifact
	 */
	public abstract String getName();

	/**
	 * Returns a String representing a list of artifacts.
	 * 
	 * @param list
	 *            of artifacts
	 * @param sep
	 *            separator
	 * @return String representation
	 */
	public static String getNames(final ArtifactList list, final String sep) {
		assert (sep != null);
		assert (list != null);

		StringBuilder sb = new StringBuilder("");
		for (Artifact artifact : list) {
			sb.append(artifact.getName());
			sb.append(sep);
		}

		return sb.toString();
	}

	/**
	 * Returns a comma-separated String representing a list of artifacts.
	 * 
	 * @param list
	 *            of artifacts
	 * @return comma-separated String
	 */
	public static String getNames(final ArtifactList list) {
		return Artifact.getNames(list, " ");
	}

	/**
	 * Returns true if this artifact physically exists.
	 * 
	 * @return true if the artifact exists.
	 */
	public abstract boolean exists();

	/**
	 * Copies an <code>Artifact</code>.
	 * 
	 * @param destination
	 *            destination artifact
	 * @throws IOException
	 *             If an input or output exception occurs.
	 */
	public abstract void copyArtifact(final Artifact destination)
			throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public abstract int hashCode();

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
	 * @param reader
	 *            Reader to read from
	 * @throws IOException
	 *             If an input output exception occurs
	 */
	public abstract void write(BufferedReader reader) throws IOException;

	/**
	 * Returns a dummy Artifact.
	 * 
	 * @return dummy artifact
	 * @throws FileNotFoundException
	 *             If a file is not found
	 */
	public abstract Artifact createEmptyDummy() throws FileNotFoundException;

	/**
	 * Creates an Artifact.
	 * 
	 * @param isLeaf
	 *            if true, a leaf type artifact will be created
	 * @throws IOException
	 *             If an input output exception occurs
	 */
	public abstract void createArtifact(boolean isLeaf)
			throws IOException;

	/**
	 * Returns identifier.
	 * 
	 * @return identifier
	 */
	public abstract String getId();

	/**
	 * Performs a merge on the provided merge triple.
	 * 
	 * @param operation
	 *            merge operation
	 * @param context
	 *            merge context
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public abstract void merge(MergeOperation operation, MergeContext context) 
			throws IOException, InterruptedException;
}
