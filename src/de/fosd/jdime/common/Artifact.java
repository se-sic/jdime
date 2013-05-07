/**
 * 
 */
package de.fosd.jdime.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

/**
 * This class represents an artifact of a program.
 * 
 * @author lessenic
 * 
 */
public class Artifact {
	/**
	 * File in which the artifact is stored.
	 */
	private File file;

	/**
	 * Revision the artifact belongs to.
	 */
	private Revision revision;

	/**
	 * Creates a new instance of an artifact.
	 * 
	 * @param revision
	 *            the artifact belongs to
	 * @param file
	 *            where the artifact is stored
	 * @throws FileNotFoundException
	 *             FileNotFoundException
	 */
	public Artifact(final Revision revision, final File file)
			throws FileNotFoundException {
		assert file != null;

		if (!file.exists()) {
			throw new FileNotFoundException();
		}

		this.revision = revision;
		this.file = file;
	}

	/**
	 * Creates a new instance of an artifact.
	 * 
	 * @param file
	 *            where the artifact is stored
	 * @throws FileNotFoundException
	 *             FileNotFoundException
	 */
	public Artifact(final File file) throws FileNotFoundException {
		this(null, file);
	}

	/**
	 * Creates an empty artifact.
	 * 
	 * @return empty artifact.
	 * @throws FileNotFoundException
	 *             FileNotFoundException
	 */
	public static Artifact createEmptyArtifact() throws FileNotFoundException {
		// FIXME: The following works only for Unix-like systems. Do something
		// about it!
		return new Artifact(new File("/dev/null"));
	}

	/**
	 * Returns a String representing a list of artifacts.
	 * 
	 * @param list
	 *            of artifacts
	 * @param sep
	 *            separator
	 * @return String representation
	 */
	public static String toString(final List<Artifact> list, final String sep) {
		StringBuilder sb = new StringBuilder();
		for (Artifact artifact : list) {
			sb.append(artifact);
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
	public static String toString(final List<Artifact> list) {
		return Artifact.toString(list, " ");
	}

	/**
	 * Returns true if artifact exists.
	 * 
	 * @return true if artifact exists
	 */
	public final boolean exists() {
		return file.exists();
	}

	/**
	 * Returns true if artifact is a normal file.
	 * 
	 * @return true if artifact is a normal file
	 */
	public final boolean isFile() {
		return file.isFile();
	}

	/**
	 * Returns true if artifact is a directory.
	 * 
	 * @return true if artifact is a directory
	 */
	public final boolean isDirectory() {
		return file.isDirectory();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString() {
		return file.getPath();
	}
}
