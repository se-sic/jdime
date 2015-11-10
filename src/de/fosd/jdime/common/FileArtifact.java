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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.MimetypesFileTypeMap;

import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.matcher.Color;
import de.fosd.jdime.matcher.Matching;
import de.fosd.jdime.stats.ElementStatistics;
import de.fosd.jdime.stats.KeyEnums;
import de.fosd.jdime.stats.MergeScenarioStatistics;
import de.fosd.jdime.strategy.DirectoryStrategy;
import de.fosd.jdime.strategy.MergeStrategy;
import de.fosd.jdime.strategy.StatisticsInterface;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

/**
 * This class represents an artifact of a program.
 *
 * @author Olaf Lessenich
 */
public class FileArtifact extends Artifact<FileArtifact> {

    private static final Logger LOG = Logger.getLogger(FileArtifact.class.getCanonicalName());

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

    private FileArtifact() throws IOException {
        try {
            file = Files.createTempFile(null, null).toFile();
            file.deleteOnExit();
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    /**
     * Constructs a new <code>FileArtifact</code> contained in the given <code>File</code>.
     * The newly constructed <code>FileArtifact</code> will not belong to a revision.
     *
     * @param file
     *         the <code>File</code> in which the artifact is stored
     *
     *
     * @throws IOException
     *         if does not exist according to {@link java.io.File#exists()} or cannot be created.
     */
    public FileArtifact(File file) throws IOException {
        this(null, file);
    }

    /**
     * Constructs a new <code>FileArtifact</code> contained in the given <code>File</code>.
     *
     * @param revision
     *         the <code>Revision</code> the artifact belongs to
     * @param file
     *         the <code>File</code> in which the artifact is stored
     *
     * @throws IOException
     *         if does not exist according to {@link java.io.File#exists()} or cannot be created.
     */
    public FileArtifact(Revision revision, File file) throws IOException {
        this(revision, file, false, null);
    }

    /**
     * Constructs a new <code>FileArtifact</code> contained in the given <code>File</code>.
     *
     * @param revision
     *         the <code>Revision</code> the artifact belongs to
     * @param file
     *         the <code>File</code> in which the artifact is stored
     * @param createIfNonexistent
     *         whether to create that <code>file</code> if it does not exist
     * @param isLeaf
     *      if true, a leaf type artifact will be created
     *
     * @throws IOException
     *         if <code>createNonExistent</code> is <code>false</code> and <code>file</code> does not exist according to {@link
     *         java.io.File#exists()}, or if <code>createNonExistent</code> is <code>true</code> but <code>file</code>
     *         cannot be created.
     */
    public FileArtifact(Revision revision, File file, boolean createIfNonexistent, Boolean isLeaf) throws IOException {
        assert file != null;

        if (!file.exists()) {
            if (createIfNonexistent) {
                if (file.getParentFile() != null && !file.getParentFile().exists()) {
                    boolean createdParents = file.getParentFile().mkdirs();
                    LOG.finest(() -> "Had to create parent directories: " + createdParents);
                }

                if (isLeaf) {
                    file.createNewFile();
                    LOG.finest(() -> "Created file" + file);
                } else {
                    file.mkdir();
                    LOG.finest(() -> "Created directory " + file);
                }

                assert (file.exists());

            } else {
                LOG.severe(() -> "File not found: " + file.getAbsolutePath());
                throw new FileNotFoundException();
            }
        }

        this.file = file;
        setRevision(revision);
        initializeChildren();

        LOG.finest(() -> "Artifact initialized: " + file.getPath());
        LOG.finest(() -> "Artifact exists: " + exists());
        LOG.finest(() -> "File exists: " + file.exists());

        if (exists()) {
            LOG.finest(() -> "Artifact isEmpty: " + isEmpty());
        }
    }

    private void initializeChildren() {
        if (!exists()) {
            return;
        }

        if (isDirectory()) {
            try {
                setChildren(getDirContent());
                for (FileArtifact child : children) {
                    child.setRevision(getRevision());
                    child.initializeChildren();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            setChildren(null);
        }
    }

    @Override
    public final FileArtifact addChild(final FileArtifact child)
            throws IOException {
        assert (child != null);

        assert (!isLeaf()) : "Child elements can not be added to leaf artifacts. "
                + "isLeaf(" + this + ") = " + isLeaf();

        assert (getClass().equals(child.getClass())) : "Can only add children of same type";

        if (exists() && isDirectory()) {

            if (child.isFile()) {
                LOG.fine(() -> "Copying file " + child + " to directory " + this);
                FileUtils.copyFileToDirectory(child.file, this.file);
            } else if (child.isDirectory()) {
                LOG.fine(() -> "Copying directory " + child + " to directory " + this);
                LOG.fine(() -> "Destination already exists overwriting: " + exists());
                FileUtils.copyDirectory(child.file, this.file);
            }

            // re-initialize children
            initializeChildren();

            // find added child
            for (FileArtifact myChild : children) {
                if (FilenameUtils.getBaseName(myChild.getFullPath()).equals(FilenameUtils.getBaseName(child.getFullPath()))) {
                    return new FileArtifact(child.getRevision(), myChild.file);
                }
            }

            LOG.finest(() -> this + ".children: " + children);

            return null;
        } else {
            return new FileArtifact(getRevision(), new File(file + File.separator + child), false, null);
        }
    }

    @Override
    public FileArtifact clone() {
        LOG.finest(() -> "CLONE: " + this);

        try {
            return new FileArtifact(getRevision(), file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public final int compareTo(final FileArtifact o) {
        if (o == this) {
            return 0;
        }

        return this.toString().compareTo(o.toString());
    }

    @Override
    public final FileArtifact createEmptyArtifact() throws IOException {
        FileArtifact emptyFile = new FileArtifact();
        LOG.finest(() -> "Artifact is a dummy artifact. Using temporary file: " + emptyFile.getFullPath());
        return emptyFile;
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
                color = m.getHighlightColor().toShell();
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
    public String prettyPrint() {
        return getContent();
    }

    @Override
    public final boolean exists() {
        assert (file != null);
        return file.exists();
    }

    @Override
    public void deleteChildren() throws IOException {
        LOG.finest(() -> this + ".deleteChildren()");

        if (exists()) {
            if (isDirectory()) {
                for (FileArtifact child : children) {
                    child.remove();
                }
            } else {
                remove();
                file.createNewFile();
            }
        }
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
    public final ArtifactList<FileArtifact> getDirContent() throws IOException {
        assert (isDirectory());

        ArtifactList<FileArtifact> contentArtifacts = new ArtifactList<>();
        File[] content = file.listFiles();

        for (int i = 0; i < content.length; i++) {
            FileArtifact child;
            File file = content[i];

            try {
                child = new FileArtifact(getRevision(), file);
                child.setParent(this);
                contentArtifacts.add(child);
            } catch (FileNotFoundException e) {
                LOG.log(Level.SEVERE, e, () -> "Could not create the FileArtifact of " + file);
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

    @Override
    public final String getId() {
        return getRevision() + ":" + getPath();
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
    public KeyEnums.Type getType() {
        return isDirectory() ? KeyEnums.Type.DIRECTORY : KeyEnums.Type.FILE;
    }

    @Override
    public KeyEnums.Level getLevel() {
        return KeyEnums.Level.NONE;
    }

    @Override
    public void addOpStatistics(MergeScenarioStatistics mScenarioStatistics, MergeContext mergeContext) {
        forAllJavaFiles(astNodeArtifact -> {
            mScenarioStatistics.add(StatisticsInterface.getASTStatistics(astNodeArtifact, null));

            // TODO do we need this with the way the new MergeScenarioStatistics work?
//            if (mergeContext.isConsecutive()) {
//                mergeContext.getStatistics().addRightStats(childStats);
//            } else {
//                mergeContext.getStatistics().addASTStats(childStats);
//            }
        });
    }

    @Override
    public void deleteOpStatistics(MergeScenarioStatistics mScenarioStatistics, MergeContext mergeContext) {
        forAllJavaFiles(astNodeArtifact -> {
            MergeScenarioStatistics delStats = StatisticsInterface.getASTStatistics(astNodeArtifact, null);
            Map<Revision, Map<KeyEnums.Level, ElementStatistics>> lStats = delStats.getLevelStatistics();
            Map<Revision, Map<KeyEnums.Type, ElementStatistics>> tStats = delStats.getTypeStatistics();

            for (Map.Entry<Revision, Map<KeyEnums.Level, ElementStatistics>> entry : lStats.entrySet()) {
                for (Map.Entry<KeyEnums.Level, ElementStatistics> sEntry : entry.getValue().entrySet()) {
                    ElementStatistics eStats = sEntry.getValue();

                    eStats.setNumDeleted(eStats.getNumAdded());
                    eStats.setNumAdded(0);
                }
            }

            for (Map.Entry<Revision, Map<KeyEnums.Type, ElementStatistics>> entry : tStats.entrySet()) {
                for (Map.Entry<KeyEnums.Type, ElementStatistics> sEntry : entry.getValue().entrySet()) {
                    ElementStatistics eStats = sEntry.getValue();

                    eStats.setNumDeleted(eStats.getNumAdded());
                    eStats.setNumAdded(0);
                }
            }

            mScenarioStatistics.add(delStats);

            // TODO do we need this with the way the new MergeScenarioStatistics work?
//            if (mergeContext.isConsecutive()) {
//                mergeContext.getStatistics().addRightStats(childStats);
//            } else {
//                mergeContext.getStatistics().addASTStats(childStats);
//            }
        });
    }

    /**
     * Uses {@link #getJavaFiles()} and applies the given <code>Consumer</code> to every resulting
     * <code>FileArtifact</code> after it being parsed to an <code>ASTNodeArtifact</code>. If an
     * <code>IOException</code> occurs getting the files the method will immediately return. If an
     * <code>IOException</code> occurs parsing a file to an <code>ASTNodeArtifact</code> it will be skipped.
     *
     * @param cons
     *         the <code>Consumer</code> to apply
     */
    private void forAllJavaFiles(Consumer<ASTNodeArtifact> cons) {
        ArtifactList<FileArtifact> javaFiles;

        try {
            javaFiles = getJavaFiles();
        } catch (IOException e) {
            LOG.log(Level.WARNING, e, () -> {
                String format = "Could not get the Java files from %s. No statistics will be collected for them.";
                return String.format(format, file.getAbsolutePath());
            });

            return;
        }

        for (FileArtifact child : javaFiles) {
            ASTNodeArtifact childAST;

            try {
                childAST = new ASTNodeArtifact(child);
            } catch (IOException e) {
                LOG.log(Level.WARNING, e, () -> {
                    String format = "Could not construct an ASTNodeArtifact from %s. No statistics will be collected for it.";
                    return String.format(format, child);
                });

                continue;
            }

            cons.accept(childAST);
        }
    }

    @Override
    public final boolean hasUniqueLabels() {
        return true;
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
            LOG.fine(() -> String.format("%s and %s are toplevel directories.", this, other));
            LOG.fine("We assume a match here and continue to merge the contained files and directories.");
            return true;
        }

        return this.toString().equals(other.toString());
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
            LOG.finest(() -> String.format("%s (%s) has content type: %s", getId(), this, contentType));

            if (!MIME_JAVA_SOURCE.equals(contentType)) {
                LOG.fine(() -> "Skipping non-java file " + this);
                return;
            }
        }

        LOG.config("Using strategy: " + strategy);
        LOG.config(() -> "merge: " + this);
        
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
        if (!exists()) {
            return;
        }

        if (isDirectory()) {
            LOG.fine(() -> "Deleting directory recursively: " + file);
            FileUtils.deleteDirectory(file);
        } else if (isFile()) {
            LOG.fine(() -> "Deleting file: " + file);
            FileUtils.deleteQuietly(file);
        } else {
            throw new UnsupportedOperationException("Only files and directories can be removed at the moment");
        }
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
    public final void write(final String str) throws IOException {
        assert (file != null);
        assert (str != null);

        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(str);
        }
    }

    @Override
    public final FileArtifact createConflictArtifact(final FileArtifact left, final FileArtifact right) {
        throw new NotYetImplementedException();
    }

    @Override
    public final FileArtifact createChoiceArtifact(final String condition, final FileArtifact artifact) {
        throw new NotYetImplementedException();
    }

    public final String getContent() {

        try {
            return file == null ? "" : FileUtils.readFileToString(file);
        } catch (IOException e) {
            LOG.log(Level.WARNING, e, () -> "Could not read the contents of " + this);
            return "";
        }
    }
}
