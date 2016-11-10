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
package de.fosd.jdime.artifact.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.activation.MimetypesFileTypeMap;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.artifact.ArtifactList;
import de.fosd.jdime.artifact.ast.ASTNodeArtifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.config.merge.MergeScenario;
import de.fosd.jdime.config.merge.Revision;
import de.fosd.jdime.config.merge.Revision.SuccessiveNameSupplier;
import de.fosd.jdime.config.merge.Revision.SuccessiveRevSupplier;
import de.fosd.jdime.execption.AbortException;
import de.fosd.jdime.execption.NotYetImplementedException;
import de.fosd.jdime.merge.Merge;
import de.fosd.jdime.operations.MergeOperation;
import de.fosd.jdime.stats.ElementStatistics;
import de.fosd.jdime.stats.KeyEnums;
import de.fosd.jdime.stats.MergeScenarioStatistics;
import de.fosd.jdime.stats.StatisticsInterface;
import de.fosd.jdime.strategy.LinebasedStrategy;
import de.fosd.jdime.strategy.MergeStrategy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.CompositeFileComparator;

import static java.util.logging.Level.SEVERE;
import static org.apache.commons.io.comparator.DirectoryFileComparator.DIRECTORY_COMPARATOR;
import static org.apache.commons.io.comparator.NameFileComparator.NAME_COMPARATOR;

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
     * A <code>Comparator</code> to compare <code>FileArtifact</code>s by their <code>File</code>s. It considers
     * all directories smaller than files and otherwise compares by the file name.
     */
    private static final Comparator<FileArtifact> comp = new Comparator<FileArtifact>() {

        @SuppressWarnings("unchecked")
        private Comparator<File> c = new CompositeFileComparator(DIRECTORY_COMPARATOR, NAME_COMPARATOR);

        @Override
        public int compare(FileArtifact o1, FileArtifact o2) {
            return c.compare(o1.getFile(), o2.getFile());
        }
    };

    /**
     * The type of {@link File} a {@link FileArtifact} represents.
     */
    public enum FileType {

        FILE,
        DIR,
        VFILE,
        VDIR;

        /**
         * Returns whether this enum constant is a virtual one.
         *
         * @return true iff this enum constant is one of {@link #VFILE} or {@link #VDIR}
         */
        boolean isVirtual() {
            return this == VFILE || this == VDIR;
        }

        /**
         * Returns whether this enum constant is either {@link #FILE} or {@link #VFILE}.
         *
         * @return true iff this enum constant is one of {@link #FILE} or {@link #VFILE}
         */
        boolean isFile() {
            return this == FILE || this == VFILE;
        }

        /**
         * Returns whether this enum constant is either {@link #DIR} or {@link #VDIR}.
         *
         * @return true iff this enum constant is one of {@link #DIR} or {@link #VDIR}
         */
        boolean isDirectory() {
            return this == DIR || this == VDIR;
        }
    }

    /**
     * A {@link Supplier} used for generating names for virtual {@link FileArtifact FileArtifacts}.
     */
    private static final SuccessiveNameSupplier virtualNameSupplier = new SuccessiveNameSupplier();

    /**
     * The type of file this {@link FileArtifact} represents.
     */
    private final FileType type;

    /**
     * The original {@link File} this {@link FileArtifact} represents or {@code null} if {@link #type} is
     * {@link FileType#VFILE} or {@link FileType#VDIR}.
     */
    private final File original;

    /**
     * The current {@link File} this {@link FileArtifact} represents.
     */
    private File file;

    /**
     * Constructs a new <code>FileArtifact</code> representing the given <code>File</code>.
     * If <code>file</code> is a directory then <code>FileArtifact</code>s representing its contents will be added
     * as children to this <code>FileArtifact</code>.
     *
     * @param revision
     *         the <code>Revision</code> the artifact belongs to
     * @param file
     *         the <code>File</code> in which the artifact is stored
     * @throws IllegalArgumentException if {@code file} does not exist
     */
    public FileArtifact(Revision revision, File file) {
        this(revision, new AtomicInteger(0)::getAndIncrement, file);
    }

    /**
     * Constructs a new <code>FileArtifact</code> representing the given <code>File</code>.
     * If <code>file</code> is a directory then <code>FileArtifact</code>s representing its contents will be added
     * as children to this <code>FileArtifact</code>.
     *
     * @param revision
     *         the <code>Revision</code> the artifact belongs to
     * @param number
     *         supplies first the number for this artifact and then in DFS order the number for its children
     * @param file
     *         the <code>File</code> in which the artifact is stored
     * @throws IllegalArgumentException if {@code file} does not exist
     */
    private FileArtifact(Revision revision, Supplier<Integer> number, File file) {
        super(revision, number.get());

        if (!file.exists()) {
            throw new IllegalArgumentException("File '" + file + "' does not exist.");
        }

        if (file.isFile()) {
            this.type = FileType.FILE;
        } else if (file.isDirectory()) {
            this.type = FileType.DIR;
        } else {
            throw new IllegalArgumentException("File '" + file + "' is not a normal file or directory.");
        }

        this.original = file;
        this.file = file;

        if (isDirectory()) {
            children = new ArtifactList<>();
            children.addAll(getDirContent(number));
            Collections.sort(children, comp);
        } else {
            children = null;
        }
    }

    /**
     * Constructs a new virtual {@link FileArtifact} representing a non-existent {@link File} with a generated name. The
     * new {@link FileArtifact} will always have the number 0.
     *
     * @param revision
     *         the {@link Revision} the artifact belongs to
     * @param type
     *         the virtual type for the {@link FileArtifact}, must be one of {@link FileType#VFILE} or
     *         {@link FileType#VDIR}
     * @throws IllegalArgumentException
     *         if {@code type} is not virtual
     */
    public FileArtifact(Revision revision, FileType type) {
        super(revision, 0);

        if (!type.isVirtual()) {
            throw new IllegalArgumentException("Type " + type + " is not virtual.");
        }

        this.type = type;
        this.original = null;

        File tempDir = FileUtils.getTempDirectory();
        IntFunction<File> toFile;

        if (type == FileType.VDIR) {
            toFile = n -> {
                synchronized (virtualNameSupplier) {
                    return new File(tempDir, "VirtualDirectory_" + virtualNameSupplier.get());
                }
            };
        } else {
            toFile = n -> {
                synchronized (virtualNameSupplier) {
                    return new File(tempDir, "VirtualFile_" + virtualNameSupplier.get());
                }
            };
        }

        this.file = IntStream.range(0, Integer.MAX_VALUE).mapToObj(toFile).filter(f -> !f.exists()).findFirst()
                .orElseThrow(() -> new AbortException("Could not find an available file name for the pretend file or directory."));
    }

    /**
     * Constructs a new virtual {@link FileArtifact} representing the given non-existent {@link File}. The new
     * {@link FileArtifact} will always have the number 0.
     *
     * @param revision
     *         the {@link Revision} the artifact belongs to
     * @param file
     *         the non-existent file the new {@link FileArtifact} represents
     * @param type
     *         the virtual type for the {@link FileArtifact}, must be one of {@link FileType#VFILE} or
     *         {@link FileType#VDIR}
     * @throws IllegalArgumentException
     *         if {@code file} exists or {@code type} is not virtual
     */
    public FileArtifact(Revision revision, File file, FileType type) {
        super(revision, 0);

        if (file.exists()) {
            throw new IllegalArgumentException("File '" + file + "' exists.");
        } else if (!type.isVirtual()) {
            throw new IllegalArgumentException("Type " + type + " is not virtual.");
        }

        this.type = type;
        this.original = null;
        this.file = file;
    }

    @Override
    public FileArtifact addChild(FileArtifact child) {

        if (!exists()) {
            String msg = String.format("FileArtifact '%s' does not exist. Can not add '%s' as a child.", this, child);
            throw new IllegalStateException(msg);
        }

        if (!isDirectory()) {
            String msg = String.format("FileArtifact '%s' does not represent a directory. Can not add '%s' as a child.", this, child);
            throw new IllegalStateException(msg);
        }

        File copy = new File(file, child.getFile().getName());

        try {
            if (child.isFile()) {
                LOG.fine(() -> String.format("Copying file %s to directory %s", child, this));
                FileUtils.copyFile(child.file, copy);
            } else if (child.isDirectory()) {
                LOG.fine(() -> String.format("Copying directory %s to directory %s", child, this));
                FileUtils.copyDirectory(child.file, copy);
            }
        } catch (IOException e) {
            LOG.log(SEVERE, e, () -> String.format("Failed to add a FileArtifact representing '%s' as a child to '%s'.", child, this));
            throw new RuntimeException(e);
        }

        FileArtifact added = new FileArtifact(getRevision(), copy);

        children.add(added);
        Collections.sort(children, comp);

        return added;
    }

    @Override
    public FileArtifact clone() {
        LOG.finest(() -> "CLONE: " + this);
        return new FileArtifact(getRevision(), file);
    }

    @Override
    public final FileArtifact createEmptyArtifact(Revision revision) {

        try {
            File temp = Files.createTempFile(null, null).toFile();
            temp.deleteOnExit();

            FileArtifact emptyFile = new FileArtifact(revision, temp);

            LOG.finest(() -> "Artifact is a dummy artifact. Using temporary file: " + emptyFile.getFullPath());
            return emptyFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String prettyPrint() {
        return getContent();
    }

    @Override
    public final boolean exists() {
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
     * Removes all <code>FileArtifact</code>s under this one representing files that are not Java source code files
     * (according to {@link #isJavaFile()}) or directories that do not contain (possibly indirectly) any java source
     * code files.
     */
    public void filterNonJavaFiles() {
        if (isDirectory()) {
            children.stream().filter(FileArtifact::isDirectory).forEach(FileArtifact::filterNonJavaFiles);

            LOG.fine(() -> "Filtering out the children not representing java source code files from " + this);
            children.removeIf(c -> (c.isFile() && !c.isJavaFile()) || (c.isDirectory() && c.getChildren().isEmpty()));
        }
    }

    /**
     * Returns whether this <code>FileArtifact</code> (probably) represents a Java source code file.
     *
     * @return true iff this <code>FileArtifact</code> likely represents a Java source code file
     */
    public boolean isJavaFile() {
        return isFile() && MIME_JAVA_SOURCE.equals(getContentType());
    }

    /**
     * Returns the MIME content type of the <code>File</code> in which this <code>FileArtifact</code> is stored. 
     * If the content type can not be determined <code>null</code> will be returned.
     *
     * @return the MIME content type
     */
    private String getContentType() {
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
     * Returns newly allocated <code>FileArtifacts</code> representing the files contained in the directory represented
     * by this <code>FileArtifact</code>. If this <code>FileArtifact</code> does not represent a directory, an empty
     * list is returned.
     *
     * @param number
     *         the number <code>Supplier</code> to be passed to the new <code>FileArtifact</code>s
     * @return <code>FileArtifacts</code> representing the children of this directory
     */
    private List<FileArtifact> getDirContent(Supplier<Integer> number) {
        File[] files = file.listFiles();

        if (files == null) {
            LOG.warning(() -> String.format("Tried to get the directory contents of %s which is not a directory.", this));
            return Collections.emptyList();
        } else if (files.length == 0) {
            return Collections.emptyList();
        }

        List<FileArtifact> artifacts = new ArrayList<>(files.length);

        for (File f : files) {
            FileArtifact child = new FileArtifact(getRevision(), number, f);

            child.setParent(this);
            artifacts.add(child);
        }

        return artifacts;
    }

    /**
     * Returns the encapsulated file.
     *
     * @return file
     */
    public final File getFile() {
        return file;
    }

    private List<FileArtifact> getJavaFiles() {
        return getJavaFiles(new ArtifactList<>());
    }

    private List<FileArtifact> getJavaFiles(List<FileArtifact> list) {

        if (isJavaFile()) {
            list.add(this);
        } else if (isDirectory()) {
            children.forEach(c -> c.getJavaFiles(list));
        }

        return list;
    }

    /**
     * Returns the absolute path of this artifact.
     *
     * @return absolute part of the artifact
     */
    public final String getFullPath() {
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
        mScenarioStatistics.getTypeStatistics(null, getType()).incrementNumAdded();

        if (!(mergeContext.getMergeStrategy() instanceof LinebasedStrategy)) {
            forAllJavaFiles(astNodeArtifact -> {
                mScenarioStatistics.add(StatisticsInterface.getASTStatistics(astNodeArtifact, null));
            });
        }
    }

    @Override
    public void deleteOpStatistics(MergeScenarioStatistics mScenarioStatistics, MergeContext mergeContext) {
        mScenarioStatistics.getTypeStatistics(null, getType()).incrementNumDeleted();

        if (!(mergeContext.getMergeStrategy() instanceof LinebasedStrategy)) {
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
            });
        }
    }

    @Override
    public void mergeOpStatistics(MergeScenarioStatistics mScenarioStatistics, MergeContext mergeContext) {
        mScenarioStatistics.getTypeStatistics(null, getType()).incrementNumMerged();
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
    public Optional<Supplier<String>> getUniqueLabel() {
        return Optional.of(() -> file.getName());
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

            if (context.hasStatistics()) {
                context.getStatistics().setCurrentFileMergeScenario(operation.getMergeScenario());
            }

            LOG.finest(() -> "Merging directories " + operation.getMergeScenario());
            merge.merge(operation, context);
        } else {
            MergeStrategy<FileArtifact> strategy = context.getMergeStrategy();
            MergeScenario<FileArtifact> scenario = operation.getMergeScenario();

            if (!isJavaFile()) {
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
            } catch (AbortException e) {
                throw e; // AbortExceptions must always cause the merge to be aborted
            } catch (RuntimeException e) {
                context.addCrash(scenario, e);

                LOG.log(SEVERE, e, () -> {
                    String ls = System.lineSeparator();
                    String scStr = operation.getMergeScenario().toString(ls, true);
                    return String.format("Exception while merging%n%s", scStr);
                });

                if (context.isExitOnError()) {
                    throw new AbortException(e);
                } else {

                    if (!context.isKeepGoing() && !(strategy instanceof LinebasedStrategy)) {
                        LOG.severe(() -> "Falling back to line based strategy.");
                        context.setMergeStrategy(MergeStrategy.parse(MergeStrategy.LINEBASED));

                        context.resetStreams();
                        merge(operation, context);
                    } else {
                        LOG.severe(() -> "Skipping " + scenario);
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
        return file.getName();
    }

    /**
     * Writes the given <code>String</code> to this <code>FileArtifact</code>.
     *
     * @param str the <code>String</code> to write
     */
    public void write(String str) {
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
            return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.log(Level.WARNING, e, () -> "Could not read the contents of " + this);
            return "";
        }
    }
}
