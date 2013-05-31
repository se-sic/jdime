/**
 * 
 */
package de.fosd.jdime.common;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

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
	 * 
	 */
	private boolean emptyDummy = false;

	/**
	 * @return the leaf
	 */
	public abstract boolean isLeaf();

	/**
	 * 
	 */
	private ArtifactList children = null;

	/**
	 * @return the children
	 */
	public final ArtifactList getChildren() {
		if (children == null) {
			initializeChildren();
		}

		return children;
	}

	/**
	 * @param children
	 *            the children to set
	 */
	public final void setChildren(final ArtifactList children) {
		this.children = children;
	}

	/**
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
	 * @param otherChild artifact
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
	 * @param emptyDummy
	 *            the emptyDummy to set
	 */
	public final void setEmptyDummy(final boolean emptyDummy) {
		this.emptyDummy = emptyDummy;
	}

	/**
	 * @return the emptyDummy
	 */
	public final boolean isEmptyDummy() {
		return emptyDummy;
	}

	/**
	 * @return the revision
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
	 * @return the parent
	 */
	public final Artifact getParent() {
		return parent;
	}

	/**
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
		StringBuilder sb = new StringBuilder();
		for (Artifact artifact : list) {
			sb.append(artifact.getName());
			sb.append(sep);
		}
		return sb.toString();
	}
	
	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
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
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
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
	 * @param artifact artifact that should be created.
	 * @param isLeaf if true, a leaf type artifact will be created
	 * @return artifact
	 * @throws IOException If an input output exception occurs
	 */
	public abstract Artifact createArtifact(Artifact artifact, boolean isLeaf)
			throws IOException;

	/**
	 * Returns identifier.
	 * 
	 * @return identifier
	 */
	public abstract String getId();
}
