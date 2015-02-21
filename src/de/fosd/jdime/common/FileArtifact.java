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
 *     Georg Seibt <seibt@fim.uni-passau.de>
 */
package de.fosd.jdime.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.activation.MimetypesFileTypeMap;

import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.matcher.Color;
import de.fosd.jdime.matcher.Matching;
import de.fosd.jdime.strategy.DirectoryStrategy;
import de.fosd.jdime.strategy.MergeStrategy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.log4j.Logger;

/**
 * This class represents an artifact of a program.
 *
 * @author Olaf Lessenich
 */
public class FileArtifact extends Artifact<FileArtifact> {

	private static final Logger LOG = Logger.getLogger(ClassUtils.getShortClassName(FileArtifact.class));

	/**
	 * The expected MIME content type for java source files.
	 */
	private static final String MIME_JAVA_SOURCE = "text/x-java";

	/**
	 * Used for determining the content type of this <code>FileArtifact</code> if
	 * {@link Files#probeContentType(java.nio.file.Path)} fails.
	 */
	private static final MimetypesFileTypeMap mimeMap;

	static {
		mimeMap = new MimetypesFileTypeMap();
		mimeMap.addMimeTypes(MIME_JAVA_SOURCE + " java");
	}

	/**
	 * File in which the artifact is stored.
	 */
	private File file;

	/**
	 * Constructs a new <code>FileArtifact</code> contained in the given <code>File</code>.
	 * The newly constructed <code>FileArtifact</code> will not belong to a revision.
	 *
	 * @param file
	 * 		the <code>File</code> in which the artifact is stored
	 *
	 * @throws FileNotFoundException
	 * 		if <code>file</code> does not exist according to {@link java.io.File#exists()}
	 */
	public FileArtifact(File file) throws FileNotFoundException {
		this(null, file);
	}

	/**
	 * Constructs a new <code>FileArtifact</code> contained in the given <code>File</code>.
	 *
	 * @param revision
	 * 		the <code>Revision</code> the artifact belongs to
	 * @param file
	 * 		the <code>File</code> in which the artifact is stored
	 *
	 * @throws FileNotFoundException
	 * 		if <code>file</code> does not exist according to {@link java.io.File#exists()}
	 */
	public FileArtifact(Revision revision, File file) throws FileNotFoundException {
		this(revision, file, true);
	}

	/**
	 * Constructs a new <code>FileArtifact</code> contained in the given <code>File</code>.
	 *
	 * @param revision
	 * 		the <code>Revision</code> the artifact belongs to
	 * @param file
	 * 		the <code>File</code> in which the artifact is stored
	 * @param checkExistence
	 * 		whether to ensure that <code>file</code> exists
	 *
	 * @throws FileNotFoundException
	 * 		if <code>checkExistence</code> is <code>true</code> and <code>file</code> does not exist according to {@link
	 * 		java.io.File#exists()}
	 */
	public FileArtifact(Revision revision, File file, boolean checkExistence) throws FileNotFoundException {
		assert file != null;

		if (checkExistence && !file.exists()) {
			LOG.fatal("File not found: " + file.getAbsolutePath());
			throw new FileNotFoundException();
		}

		setRevision(revision);
		this.file = file;

		if (LOG.isTraceEnabled()) {
			LOG.trace("Artifact initialized: " + file.getPath());
			LOG.trace("Artifact exists: " + exists());
			LOG.trace("File exists: " + file.exists());

			if (exists()) {
				LOG.trace("Artifact isEmpty: " + isEmpty());
			}
		}
	}

	@Override
	public final FileArtifact addChild(final FileArtifact child)
			throws IOException {
		assert (child != null);

		assert (!isLeaf()) : "Child elements can not be added to leaf artifacts. "
				+ "isLeaf(" + this + ") = " + isLeaf();

		assert (getClass().equals(child.getClass())) : "Can only add children of same type";

		FileArtifact myChild = new FileArtifact(getRevision(), new File(file
				+ File.separator + child), false);

		return myChild;
	}

	@Override
	public final int compareTo(final FileArtifact o) {
		if (o == this) {
			return 0;
		}

		return this.toString().compareTo(o.toString());
	}

	@Override
	public final void copyArtifact(final FileArtifact destination)
			throws IOException {
		assert (destination != null);

		if (destination.isFile()) {
			if (isFile()) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Copying file " + this + " to file "
							+ destination);
					LOG.debug("Destination already exists overwriting: "
							+ destination.exists());
				}

				FileUtils.copyFile(file, destination.file);
			} else {
				throw new UnsupportedOperationException(
						"When copying to a file, "
								+ "the source must also be a file.");
			}
		} else if (destination.isDirectory()) {
			if (isFile()) {
				assert (destination.exists()) : "Destination directory does not exist: "
						+ destination;
				if (LOG.isDebugEnabled()) {
					LOG.debug("Copying file " + this + " to directory "
							+ destination);
				}
				FileUtils.copyFileToDirectory(file, destination.file);
			} else if (isDirectory()) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Copying directory " + this + " to directory "
							+ destination);
					LOG.debug("Destination already exists overwriting: "
							+ destination.exists());
				}
				FileUtils.copyDirectory(file, destination.file);
			}
		} else {
			LOG.fatal("Failed copying " + this + " to " + destination);
			LOG.fatal("isDirectory(" + this + ") = " + isDirectory());
			LOG.fatal("isDirectory(" + destination + ") = "
					+ destination.isDirectory());
			throw new NotYetImplementedException(
					"Only copying files and directories is supported.");
		}
	}

	@Override
	public final void createArtifact(final boolean isLeaf) throws IOException {

		// assert (!artifact.exists() || Main.isForceOverwriting())
		// : "File would be overwritten: " + artifact;
		//
		// if (artifact.exists()) {
		// Artifact.remove(artifact);
		// }
		assert (!exists()) : "File would be overwritten: " + this;

		if (file.getParentFile() != null) {
			boolean createdParents = file.getParentFile().mkdirs();

			if (LOG.isTraceEnabled()) {
				LOG.trace("Had to create parent directories: " + createdParents);
			}
		}

		if (isLeaf) {
			file.createNewFile();
			if (LOG.isTraceEnabled()) {
				LOG.trace("Created file" + file);
			}
		} else {
			file.mkdir();
			if (LOG.isTraceEnabled()) {
				LOG.trace("Created directory " + file);
			}

		}

		assert (exists());
	}

	@Override
	public final FileArtifact createEmptyDummy() throws FileNotFoundException {
		File dummyFile;

		try {
			dummyFile = Files.createTempFile(null, null).toFile();
			dummyFile.deleteOnExit();
		} catch (IOException e) {
			throw new FileNotFoundException(e.getMessage());
		}

		FileArtifact dummyArtifact = new FileArtifact(dummyFile);
		dummyArtifact.setEmptyDummy(true);

		LOG.trace("Artifact is a dummy artifact. Using temporary file " + dummyFile.getAbsolutePath());

		return dummyArtifact;
	}

	@Override
	protected final String dumpTree(final String indent) {
		StringBuilder sb = new StringBuilder();

		Matching<FileArtifact> m = null;
		if (hasMatches()) {
			Set<Revision> matchingRevisions = matches.keySet();

			// print color code
			String color = "";

			for (Revision rev : matchingRevisions) {
				m = getMatching(rev);
				color = m.getColor().toShell();
			}

			sb.append(color);
		}

		sb.append(indent).append("(").append(getId()).append(") ");
		sb.append(this);

		if (hasMatches()) {
			assert (m != null);
			sb.append(" <=> (").append(m.getMatchingArtifact(this)).append(")");
			sb.append(Color.DEFAULT.toShell());
		}
		sb.append(System.lineSeparator());

		if (!isLeaf()) {
			// children
			for (FileArtifact child : getChildren()) {
				sb.append(child.dumpTree(indent + "  "));
			}
		}

		return sb.toString();
	}

	@Override
	public final boolean equals(final Object obj) {
		assert (obj != null);
		assert (obj instanceof FileArtifact);
		if (this == obj) {
			return true;
		}
		return this.toString().equals(((FileArtifact) obj).toString());
	}

	@Override
	public final boolean exists() {
		assert (file != null);
		return file.exists();
	}

	/**
	 * Returns the MIME content type of the <code>File</code> in which this <code>FileArtifact</code> is stored. 
	 * If the content type can not be determined <code>null</code> will be returned.
	 *
	 * @return the MIME content type 
	 *
	 * @throws IOException
	 *         if an I/O exception occurs while trying to determine the content type
	 */
	public final String getContentType() throws IOException {
		assert (exists());

		String mimeType = Files.probeContentType(file.toPath());

		if (mimeType == null) {
			
			// returns application/octet-stream if the type can not be determined
			mimeType = mimeMap.getContentType(file); 
			
			if ("application/octet-stream".equals(mimeType)) { 
				mimeType = null;
			}
		}

		return mimeType;
	}

	/**
	 * Returns the list of artifacts contained in this directory.
	 *
	 * @return list of artifacts contained in this directory
	 */
	public final ArtifactList<FileArtifact> getDirContent() {
		assert (isDirectory());

		ArtifactList<FileArtifact> contentArtifacts = new ArtifactList<>();
		File[] content = file.listFiles();

		for (int i = 0; i < content.length; i++) {
			try {
				FileArtifact child = new FileArtifact(getRevision(), content[i]);
				child.setParent(this);
				contentArtifacts.add(child);
			} catch (FileNotFoundException e) {
				LOG.fatal(e);
			}
		}

		return contentArtifacts;
	}

	/**
	 * Returns the encapsulated file.
	 *
	 * @return file
	 */
	public final File getFile() {
		return file;
	}

	public ArtifactList<FileArtifact> getJavaFiles() throws IOException {
		ArtifactList<FileArtifact> javaFiles = new ArtifactList<>();

		if (isFile() && MIME_JAVA_SOURCE.equals(getContentType())) {
			javaFiles.add(this);
		} else if (isDirectory()) {
			
			for (FileArtifact child : getDirContent()) {
				javaFiles.addAll(child.getJavaFiles());
			}
		}

		return javaFiles;
	}

	/**
	 * Returns the absolute path of this artifact.
	 *
	 * @return absolute part of the artifact
	 */
	public final String getFullPath() {
		assert (file != null);
		return file.getAbsolutePath();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#getId()
	 */
	@Override
	public final String getId() {
		return getPath();
	}

	/**
	 * Returns the path of this artifact.
	 *
	 * @return path of the artifact
	 */
	public final String getPath() {
		assert (file != null);
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

	/**
	 * Returns the list of (relative) filenames contained in this directory.
	 *
	 * @return list of relative filenames
	 */
	public final List<String> getRelativeDirContent() {
		assert (isDirectory());
		return Arrays.asList(file.list());
	}

	@Override
	public final String getStatsKey(final MergeContext context) {
		assert (context != null);

		// MergeStrategy<FileArtifact> strategy
		// = (MergeStrategy<FileArtifact>) (isDirectory()
		// ? new DirectoryStrategy() : context.getMergeStrategy());
		// assert (strategy != null);
		//
		// return strategy.getStatsKey(this);
		return isDirectory() ? "directories" : "files";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#hashCode()
	 */
	@Override
	public final int hashCode() {
		return toString().hashCode();
	}

	@Override
	public final boolean hasUniqueLabels() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#initializeChildren()
	 */
	@Override
	public final void initializeChildren() {
		assert (exists());

		if (isDirectory()) {
			setChildren(getDirContent());
		} else {
			setChildren(null);
		}
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
	 * Returns true if the artifact is empty.
	 *
	 * @return true if the artifact is empty
	 */
	@Override
	public final boolean isEmpty() {
		assert (exists());
		if (isDirectory()) {
			return file.listFiles().length == 0;
		} else {
			return FileUtils.sizeOf(file) == 0;
		}
	}

	/**
	 * Returns true if artifact is a normal file.
	 *
	 * @return true if artifact is a normal file
	 */
	public final boolean isFile() {
		return file.isFile();
	}

	@Override
	public final boolean isLeaf() {
		return !file.isDirectory();
	}

	@Override
	public final boolean isOrdered() {
		return false;
	}

	@Override
	public final boolean matches(final FileArtifact other) {
		if (isDirectory() && isRoot() && other.isDirectory() && other.isRoot()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug(this + " and " + other + " are toplevel directories.");
				LOG.debug("We assume a match here and continue to merge the "
						+ "contained files and directories.");
			}

			return true;
		}
		return this.equals(other);
	}

	
	@Override
	public final void merge(MergeOperation<FileArtifact> operation, MergeContext context) throws IOException, InterruptedException {
		Objects.requireNonNull(operation, "operation must not be null!");
		Objects.requireNonNull(context, "context must not be null!");
		
		if (!exists()) {
			String className = getClass().getSimpleName();
			String filePath = file.getAbsolutePath();
			String message = String.format("Trying to merge %s whose file %s does not exist.", className, filePath);
			
			throw new RuntimeException(message);
		}
		
		@SuppressWarnings("unchecked")
		MergeStrategy<FileArtifact> strategy = (MergeStrategy<FileArtifact>) context.getMergeStrategy();
		
		if (isDirectory()) {
			strategy = new DirectoryStrategy();
		} else {
			String contentType = getContentType();

			if (LOG.isTraceEnabled()) {
				LOG.trace(getId() + " (" + this + ") has content type: " + contentType);
			}

			if (!MIME_JAVA_SOURCE.equals(contentType)) {
				LOG.debug("Skipping non-java file.");
				return;
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("Using strategy: " + strategy);
		}
		LOG.info(this);
		
		strategy.merge(operation, context);
		
		if (!context.isQuiet() && context.hasOutput()) {
			System.out.print(context.getStdIn());
		}
		context.resetStreams();
	}

	/**
	 * Removes the artifact's file.
	 *
	 * @throws IOException
	 *             If an input output exception occurs
	 */
	public final void remove() throws IOException {
		assert (exists() && !isEmptyDummy()) : "Tried to remove non-existing file: "
				+ getFullPath();

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

		assert (!exists());
	}

	@Override
	public final String toString() {
		assert (file != null);
		return file.getName();
	}

	/**
	 * Writes from a BufferedReader to the artifact.
	 *
	 * @param str
	 *            String to write
	 * @throws IOException
	 *             If an input output exception occurs.
	 */
	@Override
	public final void write(final String str) throws IOException {
		assert (file != null);
		assert (str != null);
		try (FileWriter writer = new FileWriter(file)) {
			writer.write(str);
		}
	}

	@Override
	public final FileArtifact createConflictDummy(final FileArtifact type,
			final FileArtifact left, final FileArtifact right)
			throws FileNotFoundException {
		throw new NotYetImplementedException();
	}

	public final String getContent() throws IOException {
		return file == null ? "" : FileUtils.readFileToString(file);
	}
}
