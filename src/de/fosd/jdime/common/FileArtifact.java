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
import org.apache.log4j.Logger;

/**
 * This class represents an artifact of a program.
 * 
 * @author Olaf Lessenich *
 */
public class FileArtifact extends Artifact {
	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(FileArtifact.class);

	/**
	 * File in which the artifact is stored.
	 */
	private File file;

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
	public FileArtifact(final Revision revision, final File file,
			final boolean checkPresence) 
					throws FileNotFoundException {
		assert file != null;

		if (checkPresence && !file.exists()) {
			System.err.println("File not found: " + file.getAbsolutePath());
			throw new FileNotFoundException();
		}

		setRevision(revision);
		this.file = file;

		if (LOG.isTraceEnabled()) {
			LOG.trace("Artifact initialized: " + file.getPath());
			LOG.trace("Artifact exists: " + file.exists());
		}
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
	public FileArtifact(final Revision revision, final File file)
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
	public FileArtifact(final File file) throws FileNotFoundException {
		this(null, file);
	}

	@Override
	public final Artifact createEmptyDummy() throws FileNotFoundException {
		// FIXME: The following works only for Unix-like systems. Do something
		// about it!
		Artifact myEmptyDummy = new FileArtifact(new File("/dev/null"));
		myEmptyDummy.setEmptyDummy(true);
		LOG.trace("Artifact is a dummy artifact.");
		return myEmptyDummy;
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
	public final ArtifactList getDirContent() {
		if (!this.isDirectory()) {
			throw new UnsupportedOperationException();
		}

		ArtifactList contentArtifacts = new ArtifactList();
		File[] content = file.listFiles();

		for (int i = 0; i < content.length; i++) {
			try {
				Artifact child = new FileArtifact(getRevision(), content[i]);
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
	public final List<String> getRelativeDirContent() {
		if (!this.isDirectory()) {
			throw new UnsupportedOperationException();
		}

		return Arrays.asList(file.list());
	}

	/**
	 * Returns the path of this artifact.
	 * 
	 * @return path of the artifact
	 */
	public final String getPath() {
		return file.getPath();
	}

	/**
	 * Returns the absolute path of this artifact.
	 * 
	 * @return absolute part of the artifact
	 */
	public final String getFullPath() {
		return file.getAbsolutePath();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#getName()
	 */
	@Override
	public final String getName() {
		return file.getPath();
	}

	/**
	 * Returns a reader that can be used to retrieve the content of the
	 * artifact.
	 * 
	 * @return Reader
	 * @throws FileNotFoundException
	 *             If the artifact is a file which is not found
	 */
	public final BufferedReader getReader() throws FileNotFoundException {
		if (isFile()) {
			return new BufferedReader(new FileReader(file));
		} else {
			throw new NotYetImplementedException();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#isLeaf()
	 */
	@Override
	public final boolean isLeaf() {
		return file.isFile();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.fosd.jdime.common.Artifact#copyArtifact(de.fosd.jdime.common.Artifact)
	 */
	@Override
	public final void copyArtifact(final Artifact destination)
			throws IOException {
		if (((FileArtifact) destination).isFile()) {
			if (isFile()) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Copying file " + this + " to file "
							+ destination);
					LOG.debug("Destination already exists overwriting: "
							+ destination.exists());
				}

				FileUtils.copyFile(file, ((FileArtifact) destination).file);
			} else {
				throw new UnsupportedOperationException(
						"When copying to a file, "
								+ "the source must also be a file.");
			}
		} else if (((FileArtifact) destination).isDirectory()) {
			if (isFile()) {
				assert (destination.exists()) 
					: "Destination directory does not exist: " + destination;
				if (LOG.isDebugEnabled()) {
					LOG.debug("Copying file " + this + " to directory "
							+ destination);
				}
				FileUtils.copyFileToDirectory(file,
						((FileArtifact) destination).file);
			} else if (isDirectory()) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Copying directory " + this + " to directory "
							+ destination);
					LOG.debug("Destination already exists overwriting: "
							+ destination.exists());
				}
				FileUtils
						.copyDirectory(file, ((FileArtifact) destination).file);
			}
		} else {
			throw new NotYetImplementedException(
					"Only copying files and directories is supported by now.");
		}
	}

	@Override
	public final Artifact createArtifact(final Artifact artifact,
			final boolean isLeaf) throws IOException {
		if (artifact == null) {
			return artifact;
		}

		// assert (!artifact.exists() || Main.isForceOverwriting())
		// : "File would be overwritten: " + artifact;
		//
		// if (artifact.exists()) {
		// Artifact.remove(artifact);
		// }

		FileArtifact fileartifact = (FileArtifact) artifact;

		assert (!artifact.exists()) : "File would be overwritten: "
				+ fileartifact;

		boolean createdParents = fileartifact.file.getParentFile().mkdirs();

		if (LOG.isTraceEnabled()) {
			LOG.trace("Had to create parent directories: " + createdParents);
		}

		if (isLeaf) {
			fileartifact.file.createNewFile();
			if (LOG.isTraceEnabled()) {
				LOG.trace("Created file" + fileartifact.file);
			}
		} else {
			fileartifact.file.mkdir();
			if (LOG.isTraceEnabled()) {
				LOG.trace("Created directory " + fileartifact.file);
			}
			
		}

		return fileartifact;
	}

	/**
	 * Removes the artifact's file.
	 * 
	 * @throws IOException
	 *             If an input output exception occurs
	 */
	public final void remove() throws IOException {
		assert (exists() && !isEmptyDummy()) 
					: "Tried to remove non-existing file: " + getFullPath();

		if (isDirectory()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Deleting directory recursively: " + file);
			}
			FileUtils.deleteDirectory(file);
		} else if (isFile()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Deleting file: " + file);
			}
			file.delete();
		} else {
			throw new UnsupportedOperationException(
					"Only files and directories can be removed at the moment");
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

	/**
	 * Returns true if the artifact is empty.
	 * 
	 * @return true if the artifact is empty
	 */
	public final boolean isEmpty() {
		return FileUtils.sizeOf(file) == 0;
	}

	@Override
	public final void initializeChildren() {
		if (isDirectory()) {
			setChildren(getDirContent());
		} else {
			setChildren(null);
		}
	}

	@Override
	public final Artifact addChild(final Artifact child) throws IOException {
		assert (!isLeaf()) 
					: "Child elements can not be added to leaf artifacts. "
						+ "isLeaf(" + this + ") = " + isLeaf();

		assert (getClass().equals(child.getClass())) 
					: "Can only add children of same type";

		FileArtifact myChild = new FileArtifact(getRevision(), new File(file
				+ File.separator + child.getId()), false);
		
		return myChild;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.fosd.jdime.common.Artifact#hasChild(de.fosd.jdime.common.Artifact)
	 */
	@Override
	public final Artifact getChild(final Artifact otherChild) {
		for (Artifact myChild : getChildren()) {
			if (myChild.equals(otherChild)) {
				return myChild;
			}
		}

		return null;
	}

	@Override
	public final int hashCode() {
		return getId().hashCode();
	}

	@Override
	public final boolean equals(final Object obj) {
		return getId().equals(((FileArtifact) obj).getId());
	}

	@Override
	public final String getId() {
		return file.getName();
	}
}
