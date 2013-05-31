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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;

/**
 * This class represents an artifact of a program.
 * 
 * @author Olaf Lessenich
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
	 * Needed for missing base revisions.
	 */
	private boolean emptyDummy = false;

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
	 * Returns true if this artifacy is an empty dummy.
	 * 
	 * @return true if this artifact is a empty dummy
	 */
	public final boolean isEmptyDummy() {
		return emptyDummy;
	}

	/**
	 * Creates a new instance of an artifact.
	 * 
	 * @param revision
	 *            the artifact belongs to
	 * @param file
	 *            where the artifact is stored
	 * @param checkPresence
	 *            If true, an exception is thrown when the file does not exist
	 * @throws FileNotFoundException
	 *             FileNotFoundException
	 */
	public Artifact(final Revision revision, final File file,
			final boolean checkPresence) throws FileNotFoundException {
		assert file != null;

		if (checkPresence && !file.exists()) {
			System.err.println("File not found: " + file.getAbsolutePath());
			throw new FileNotFoundException();
		}

		this.revision = revision;
		this.file = file;
	}

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
		this(revision, file, true);
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
	 * Creates an empty artifact. This is needed for empty base revisions.
	 * 
	 * @return empty artifact.
	 * @throws FileNotFoundException
	 *             FileNotFoundException
	 */
	public static Artifact createEmptyArtifact() throws FileNotFoundException {
		// FIXME: The following works only for Unix-like systems. Do something
		// about it!
		Artifact newEmptyDummy = new Artifact(new File("/dev/null"));
		newEmptyDummy.emptyDummy = true;
		return newEmptyDummy;
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
	public static String toString(final ArtifactList list, final String sep) {
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
	public static String toString(final ArtifactList list) {
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

	/**
	 * Returns the list of artifacts contained in this directory.
	 * 
	 * @return list of artifacts contained in this directory
	 */
	public final ArtifactList getContent() {
		if (!this.isDirectory()) {
			throw new UnsupportedOperationException();
		}

		ArtifactList contentArtifacts = new ArtifactList();
		File[] content = file.listFiles();

		for (int i = 0; i < content.length; i++) {
			try {
				Artifact child = new Artifact(this.revision, content[i]);
				child.setParent(this);
				contentArtifacts.add(child);
			} catch (FileNotFoundException e) {
				// this should not happen
				e.printStackTrace();
			}
		}

		return contentArtifacts;

	}

	/**
	 * Returns the list of (relative) filenames contained in this directory.
	 * 
	 * @return list of relative filenames
	 */
	public final List<String> getRelativeContent() {
		if (!this.isDirectory()) {
			throw new UnsupportedOperationException();
		}

		return Arrays.asList(file.list());
	}

	/**
	 * Computes the relative path of an artifact using a second artifact as
	 * base.
	 * 
	 * @param artifact
	 *            artifact whose relative path should be computed
	 * @param base
	 *            base artifact
	 * @return relative path of artifact
	 */
	public static String computeRelativePath(final Artifact artifact,
			final Artifact base) {
		String relativePath = artifact.toString().substring(
				base.toString().length());
		while (relativePath.startsWith("/")) {
			relativePath = relativePath.substring(1);
		}
		return relativePath;
	}

	/**
	 * Returns the path of this artifact.
	 * 
	 * @return path of the artifact
	 */
	public final String getPath() {
		return file.getPath();
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

	/**
	 * Returns a reader that can be used to retrieve the content of the
	 * artifact.
	 * 
	 * @return Reader
	 * @throws FileNotFoundException
	 *             If the artifact is a file which is not found
	 * @throws NotYetImplementedException
	 *             If a functionality is reached that is not implemented yet
	 */
	public final BufferedReader getReader() throws FileNotFoundException,
			NotYetImplementedException {
		if (isFile()) {
			return new BufferedReader(new FileReader(file));
		} else {
			throw new NotYetImplementedException();
		}
	}

	/**
	 * Copies an <code>Artifact</code>.
	 * 
	 * @param source
	 *            source artifact
	 * @param destination
	 *            destination artifact
	 * @throws IOException
	 *             If an input or output exception occurs.
	 * @throws NotYetImplementedException
	 *             If some functionality is reached that is not implemented yet.
	 */
	public static void copyArtifact(final Artifact source,
			final Artifact destination) throws IOException,
			NotYetImplementedException {
		if (destination.isFile()) {
			if (source.isFile()) {
				// assert (!destination.exists()) :
				// "File would be overwritten: "
				// + destination;
				FileUtils.copyFile(source.file, destination.file);
			} else {
				throw new UnsupportedOperationException(
						"When copying to a file, "
								+ "the source must also be a file.");
			}
		} else if (destination.isDirectory()) {
			if (source.isFile()) {
				assert (destination.exists()) 
				: "Destination directory does not exist: "
						+ destination;
				FileUtils.copyFileToDirectory(source.file, destination.file);
			} else if (source.isDirectory()) {
				// assert (!destination.exists()) :
				// "File would be overwritten: "
				// + destination;
				FileUtils.copyDirectory(source.file, destination.file);
			}
		} else {
			throw new NotYetImplementedException(
					"Only copying files and directories is supported by now.");
		}
	}

	/**
	 * Create a new file or directory.
	 * 
	 * @param artifact
	 *            artifact that should be created
	 * @param isDirectory
	 *            if true, a directory is created
	 * @throws IOException
	 *             if an input output exception occurs
	 */
	public static void createFile(final Artifact artifact,
			final boolean isDirectory) throws IOException {
		if (artifact == null) {
			return;
		}
		
		assert (!artifact.exists()) : "File would be overwritten: " + artifact;

		artifact.file.getParentFile().mkdirs();

		if (isDirectory) {
			artifact.file.mkdir();
		} else {
			artifact.file.createNewFile();
		}
	}

	/**
	 * Writes from a BufferedReader to the artifact.
	 * 
	 * @param reader
	 *            reader
	 * @throws IOException
	 *             If an input output exception occurs.
	 */
	public final void write(final BufferedReader reader) throws IOException {
		FileWriter writer = new FileWriter(file);

		String line = "";
		while ((line = reader.readLine()) != null) {
			writer.append(line);
			writer.append(System.getProperty("line.separator"));
		}

		writer.close();
	}

}
