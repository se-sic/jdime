/**
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
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.MimetypesFileTypeMap;

import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.merge.Merge;
import de.fosd.jdime.stats.ElementStatistics;
import de.fosd.jdime.stats.KeyEnums;
import de.fosd.jdime.stats.MergeScenarioStatistics;
import de.fosd.jdime.stats.StatisticsInterface;
import de.fosd.jdime.strategy.LinebasedStrategy;
import de.fosd.jdime.strategy.MergeStrategy;
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
        file = Files.createTempFile(null, null).toFile();
        file.deleteOnExit();
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
    public FileArtifact addChild(FileArtifact child) {
        assert (child != null);
        assert (!isLeaf()) : String.format("Child elements can not be added to leaf artifacts. isLeaf(%s) = %s", this, isLeaf());
        assert (getClass().equals(child.getClass())) : "Can only add children of same type";

        if (exists() && isDirectory()) {

            try {
                if (child.isFile()) {
                    LOG.fine(() -> "Copying file " + child + " to directory " + this);
                    FileUtils.copyFileToDirectory(child.file, this.file);
                } else if (child.isDirectory()) {
                    LOG.fine(() -> "Copying directory " + child + " to directory " + this);
                    LOG.fine(() -> "Destination already exists overwriting: " + exists());
                    FileUtils.copyDirectory(child.file, this.file);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // re-initialize children
            initializeChildren();

            // find added child
            for (FileArtifact myChild : children) {
                if (FilenameUtils.getBaseName(myChild.getFullPath()).equals(FilenameUtils.getBaseName(child.getFullPath()))) {

                    try {
                        return new FileArtifact(child.getRevision(), myChild.file);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            LOG.finest(() -> this + ".children: " + children);

            return null;
        } else {

            try {
                return new FileArtifact(getRevision(), new File(file + File.separator + child), false, null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
    public final FileArtifact createEmptyArtifact() {
        FileArtifact emptyFile;

        try {
            emptyFile = new FileArtifact();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LOG.finest(() -> "Artifact is a dummy artifact. Using temporary file: " + emptyFile.getFullPath());
        return emptyFile;
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
    public void deleteChildren() {
        LOG.finest(() -> this + ".deleteChildren()");

        if (exists()) {
            if (isDirectory()) {
                for (FileArtifact child : children) {
                    child.remove();
                }
            } else {
                remove();

                try {
                    if (!file.createNewFile()) {
                        throw new IOException("File#createNewFile returned false.");
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Returns the MIME content type of the <code>File</code> in which this <code>FileArtifact</code> is stored. 
     * If the content type can not be determined <code>null</code> will be returned.
     *
     * @return the MIME content type
     */
    public final String getContentType() {
        assert (exists());

        String mimeType = null;

        try {
            mimeType = Files.probeContentType(file.toPath());
        } catch (IOException e) {
            LOG.log(Level.WARNING, e, () -> "Could not probe content type of " + file);
        }

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

    public ArtifactList<FileArtifact> getJavaFiles() {
        ArtifactList<FileArtifact> javaFiles = new ArtifactList<>();

        if (isFile() && MIME_JAVA_SOURCE.equals(getContentType())) {
            javaFiles.add(this);
        } else if (isDirectory()) {

            try {
                for (FileArtifact child : getDirContent()) {
                    javaFiles.addAll(child.getJavaFiles());
                }
            } catch (IOException e) {
                LOG.log(Level.WARNING, e, () -> "Could not get directory content of " + this);
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

        for (FileArtifact child : getJavaFiles()) {
            ASTNodeArtifact childAST;

            try {
                childAST = new ASTNodeArtifact(child);
            } catch (RuntimeException e) {
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
    public void merge(MergeOperation<FileArtifact> operation, MergeContext context) {
        Objects.requireNonNull(operation, "operation must not be null!");
        Objects.requireNonNull(context, "context must not be null!");
        
        if (!exists()) {
            String className = getClass().getSimpleName();
            String filePath = file.getAbsolutePath();
            String message = String.format("Trying to merge %s whose file %s does not exist.", className, filePath);
            
            throw new RuntimeException(message);
        }

        if (isDirectory()) {
            Merge<FileArtifact> merge = new Merge<>();

            LOG.finest(() -> "Merging directories " + operation.getMergeScenario());
            merge.merge(operation, context);
        } else {
            MergeStrategy<FileArtifact> strategy = context.getMergeStrategy();
            MergeScenario<FileArtifact> scenario = operation.getMergeScenario();

            String contentType = getContentType();
            LOG.finest(() -> String.format("%s (%s) has content type: %s", getId(), this, contentType));

            if (!MIME_JAVA_SOURCE.equals(contentType)) {
                LOG.fine(() -> "Skipping non-java file " + this);
                return;
            }

            if (context.hasStatistics()) {
                context.getStatistics().setCurrentFileMergeScenario(scenario);
            }

            try {
                strategy.merge(operation, context);

                if (!context.isQuiet() && context.hasOutput()) {
                    System.out.print(context.getStdIn());
                }
            } catch (RuntimeException e) {
                context.addCrash(scenario, e);

                LOG.log(Level.SEVERE, e, () -> {
                    String ls = System.lineSeparator();
                    String scStr = operation.getMergeScenario().toString(ls, true);
                    return String.format("Exception while merging%n%s", scStr);
                });

                if (context.isExitOnError()) {
                    throw new AbortException(e);
                } else {

                    if (!context.isKeepGoing() && !(strategy instanceof LinebasedStrategy)) {
                        context.setMergeStrategy(MergeStrategy.parse(MergeStrategy.LINEBASED));

                        context.resetStreams();
                        merge(operation, context);
                    }
                }
            }

            context.resetStreams();
        }
    }

    /**
     * Removes the artifact's file.
     */
    public void remove() {
        if (!exists()) {
            return;
        }

        try {
            if (isDirectory()) {
                LOG.fine(() -> "Deleting directory recursively: " + file);
                FileUtils.forceDelete(file);
            } else if (isFile()) {
                LOG.fine(() -> "Deleting file: " + file);
                FileUtils.forceDelete(file);
            } else {
                throw new UnsupportedOperationException("Only files and directories can be removed at the moment");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public final String toString() {
        assert (file != null);
        return file.getName();
    }

    /**
     * Writes the given <code>String</code> to this <code>FileArtifact</code>.
     *
     * @param str the <code>String</code> to write
     */
    public void write(String str) {
        assert (file != null);
        assert (str != null);

        if (file.getParentFile() != null && !file.getParentFile().exists()) {

            try {
                FileUtils.forceMkdir(file.getParentFile());
            } catch (IOException e) {
                LOG.log(Level.WARNING, e, () -> "Could not create the parent folder of " + file);
            }
        }

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(str);
        } catch (IOException e) {
            LOG.log(Level.WARNING, e, () -> "Could not write to " + this);
        }
    }

    @Override
    public FileArtifact createConflictArtifact(FileArtifact left, FileArtifact right) {
        throw new NotYetImplementedException();
    }

    @Override
    public FileArtifact createChoiceArtifact(String condition, FileArtifact artifact) {
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
