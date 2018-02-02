/**
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2017 University of Passau, Germany
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

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.artifact.ArtifactList;
import de.fosd.jdime.artifact.ast.ASTNodeArtifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.config.merge.MergeScenario;
import de.fosd.jdime.config.merge.Revision;
import de.fosd.jdime.config.merge.Revision.SuccessiveNameSupplier;
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
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.CompositeFileComparator;

import static de.fosd.jdime.stats.MergeScenarioStatus.FAILED;
import static java.nio.charset.StandardCharsets.UTF_8;
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
     * The type of virtual {@link File} to be represented by a {@link FileArtifact}.
     */
    public enum FileType {
        FILE,
        DIR;
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
     * The original existing {@link File} this {@link FileArtifact} represents or {@code null} if this
     * {@link FileArtifact} is virtual.
     */
    private final File original;

    /**
     * The current {@link File} this {@link FileArtifact} represents.
     */
    private File file;

    /**
     * The content of this {@link FileArtifact}. The content will be retrieved from the {@link #original} {@link File}
     * and written back to the {@link #file} after the merge.
     */
    private String content;

    /**
     * Constructs a new <code>FileArtifact</code> representing the given <code>File</code>. If <code>file</code> is a
     * directory then <code>FileArtifact</code>s representing its contents will be added as children to this
     * <code>FileArtifact</code>.
     *
     * @param revision
     *         the <code>Revision</code> the artifact belongs to
     * @param file
     *         the <code>File</code> in which the artifact is stored
     * @throws IllegalArgumentException
     *         if {@code file} does not exist
     */
    public FileArtifact(Revision revision, File file) {
        this(revision, new AtomicInteger(0)::getAndIncrement, file, true);
    }

    /**
     * Constructs a new <code>FileArtifact</code> representing the given <code>File</code>.
     *
     * @param revision
     *         the <code>Revision</code> the artifact belongs to
     * @param file
     *         the <code>File</code> in which the artifact is stored
     * @param recursive
     *         If <code>file</code> is a directory then <code>FileArtifact</code>s representing its contents will be
     *         added as children to this <code>FileArtifact</code>.
     * @throws IllegalArgumentException
     *         if {@code file} does not exist
     */
    public FileArtifact(Revision revision, File file, boolean recursive) {
        this(revision, new AtomicInteger(0)::getAndIncrement, file, recursive);
    }

    /**
     * Constructs a new <code>FileArtifact</code> representing the given <code>File</code>. If <code>file</code> is a
     * directory then <code>FileArtifact</code>s representing its contents will be added as children to this
     * <code>FileArtifact</code>.
     *
     * @param revision
     *         the <code>Revision</code> the artifact belongs to
     * @param number
     *         supplies first the number for this artifact and then in DFS order the number for its children
     * @param file
     *         the <code>File</code> in which the artifact is stored
     * @param recursive
     *         If <code>file</code> is a directory then <code>FileArtifact</code>s representing its contents will be
     *         added as children to this <code>FileArtifact</code>.
     * @throws IllegalArgumentException
     *         if {@code file} does not exist
     */
    private FileArtifact(Revision revision, Supplier<Integer> number, File file, boolean recursive) {
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

        if (recursive && isDirectory()) {
            modifyChildren(children -> {
                children.addAll(getDirContent(number));
                children.sort(comp);
            });
        }
    }

    /**
     * Constructs a new virtual {@link FileArtifact} representing a non-existent {@link File} with a generated name. The
     * new {@link FileArtifact} will always have the number 0.
     *
     * @param revision
     *         the {@link Revision} the artifact belongs to
     * @param type
     *         the virtual type for the {@link FileArtifact}, must be one of {@link FileType#FILE} or
     *         {@link FileType#DIR}
     */
    public FileArtifact(Revision revision, FileType type) {
        super(revision, 0);

        this.type = type;
        this.original = null;

        File tempDir = FileUtils.getTempDirectory();
        IntFunction<File> toFile;

        if (type == FileType.DIR) {
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
                .orElseThrow(() -> new AbortException("Could not find an available file name for the virtual file or directory."));
    }

    /**
     * Copies the given {@link FileArtifact} detached from its tree.
     *
     * @param toCopy
     *         the {@link FileArtifact} to copy
     * @see #copy()
     */
    private FileArtifact(FileArtifact toCopy) {
        super(toCopy);

        this.type = toCopy.type;
        this.original = toCopy.original;
        this.file = toCopy.file;
        this.content = toCopy.content;
    }

    @Override
    protected FileArtifact self() {
        return this;
    }

    @Override
    public void addChild(FileArtifact child) {
        super.addChild(child);

        child.file = new File(file, child.file.getName());
        modifyChildren(ch -> ch.sort(comp));
    }

    @Override
    protected boolean canAddChild(FileArtifact toAdd) {

        if (!isDirectory()) {
            String msg = String.format("FileArtifact '%s' does not represent a directory. Can not add '%s' as a child.", this, toAdd);
            throw new IllegalStateException(msg);
        }

        return true;
    }

    @Override
    public FileArtifact copy() {
        return new FileArtifact(this);
    }

    @Override
    public FileArtifact createEmptyArtifact(Revision revision) {
        return new FileArtifact(revision, FileType.FILE);
    }

    @Override
    public String prettyPrint() {
        return getContent();
    }

    @Override
    public boolean exists() {
        return getFile().exists();
    }

    /**
     * Removes all <code>FileArtifact</code>s under this one representing files that are not Java source code files
     * (according to {@link #isJavaFile()}) or directories that do not contain (possibly indirectly) any java source
     * code files.
     */
    public void filterNonJavaFiles() {
        if (isDirectory()) {
            getChildren().stream().filter(FileArtifact::isDirectory).forEach(FileArtifact::filterNonJavaFiles);

            LOG.fine(() -> "Filtering out the children not representing java source code files from " + this);
            modifyChildren(cs -> cs.removeIf(c -> c.isFile() && !c.isJavaFile() || c.isDirectory() && !c.hasChildren()));
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
        String mimeType = null;
        File file = getFile();

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
        File[] files = getFile().listFiles();

        if (files == null) {
            LOG.warning(() -> String.format("Tried to get the directory contents of %s which is not a directory.", this));
            return Collections.emptyList();
        } else if (files.length == 0) {
            return Collections.emptyList();
        }

        List<FileArtifact> artifacts = new ArrayList<>(files.length);

        for (File f : files) {
            FileArtifact child = new FileArtifact(getRevision(), number, f, true);

            child.setParent(this);
            artifacts.add(child);
        }

        return artifacts;
    }

    /**
     * Returns the encapsulated file. The original file will be returned for non-virtual
     * {@link FileArtifact FileArtifacts}. If the {@link FileArtifact} is virtual, the returned {@link File} may not
     * exist.
     *
     * @return the encapsulated {@link File}
     */
    public File getFile() {
        return original != null ? original : file;
    }

    /**
     * Returns all {@link FileArtifact FileArtifacts} representing Java sourcecode files that are part of the
     * {@link FileArtifact} tree rooted in this {@link Artifact}.
     *
     * @return the Java sourcecode files as determined by {@link #isJavaFile()}
     */
    private List<FileArtifact> getJavaFiles() {
        return getJavaFiles(new ArtifactList<>());
    }

    /**
     * Returns all {@link FileArtifact FileArtifacts} representing Java sourcecode files that are part of the
     * {@link FileArtifact} tree rooted in this {@link Artifact}.
     *
     * @param list the {@link List} to append the Java sourcecode files to
     * @return the given {@code list} containing the Java sourcecode files as determined by {@link #isJavaFile()}
     */
    private List<FileArtifact> getJavaFiles(List<FileArtifact> list) {

        if (isJavaFile()) {
            list.add(this);
        } else if (isDirectory()) {
            getChildren().forEach(c -> c.getJavaFiles(list));
        }

        return list;
    }

    @Override
    public final String getId() {
        return getRevision() + ":" + getFile().getPath();
    }

    @Override
    protected String hashId() {
        return file.getName();
    }

    /**
     * Returns the SHA256 hash of the content of this {@link FileArtifact} encoded in a hexadecimal {@link String}.
     *
     * @return the hexadecimal content hash
     */
    public String getContentHash() {
        return DigestUtils.sha256Hex(getContent());
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
            forAllJavaFiles(astNodeArtifact ->
                    mScenarioStatistics.add(StatisticsInterface.getASTStatistics(astNodeArtifact, null))
            );
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
        return Optional.of(() -> getFile().getName());
    }

    /**
     * Returns true if artifact is a directory.
     *
     * @return true if artifact is a directory
     */
    public boolean isDirectory() {
        return type == FileType.DIR;
    }

    /**
     * Returns true if the artifact is empty.
     *
     * @return true if the artifact is empty
     */
    @Override
    public boolean isEmpty() {
        if (isDirectory()) {
            return !hasChildren();
        } else {
            return "".equals(getContent());
        }
    }

    /**
     * Returns true if artifact is a normal file.
     *
     * @return true if artifact is a normal file
     */
    public boolean isFile() {
        return type == FileType.FILE;
    }

    @Override
    public boolean isOrdered() {
        return false;
    }

    @Override
    public boolean matches(final FileArtifact other) {

        if (isDirectory() && isRoot() && other.isDirectory() && other.isRoot()) {
            LOG.fine(() -> String.format("%s and %s are toplevel directories.", this, other));
            LOG.fine("We assume a match here and continue to merge the contained files and directories.");
            return true;
        }

        return this.toString().equals(other.toString());
    }

    @Override
    public boolean categoryMatches(FileArtifact other) {
        return isDirectory() && other.isDirectory() || isFile() && other.isFile();
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
                try {
                    strategy.merge(operation, context);
                } catch (Throwable e) {

                    if (context.hasStatistics()) {
                        context.getStatistics().getScenarioStatistics(scenario).setStatus(FAILED);
                    }

                    throw e;
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

                        context.setMergeStrategy(MergeStrategy.parse(MergeStrategy.LINEBASED).get());
                        merge(operation, context);
                    } else {
                        LOG.severe(() -> "Skipping " + scenario);
                    }
                }
            }
        }
    }

    @Override
    public final String toString() {
        return getFile().getName();
    }

    @Override
    public FileArtifact createConflictArtifact(FileArtifact left, FileArtifact right) {
        throw new NotYetImplementedException();
    }

    @Override
    public FileArtifact createChoiceArtifact(String condition, FileArtifact artifact) {
        throw new NotYetImplementedException();
    }

    /**
     * Outputs the contents represented by this {@link FileArtifact} and its children using the given
     * {@link PrintStream}.
     *
     * @param to
     *         the {@link PrintStream} to write to
     */
    public void outputContent(PrintStream to) {

        if (isDirectory()) {
            getChildren().forEach(c -> {
                c.outputContent(to);
                to.println();
            });
        } else {
            to.print(getContent());
        }
    }

    /**
     * Recursively (over)writes the contents of this {@link FileArtifact} and its children to the files they represent.
     *
     * @throws IOException
     *         if there is an exception accessing the filesystem
     */
    public void writeContent() throws IOException {

        if (isFile()) {

            if (content != null) {
                writeToFile();
            } else if (original != null) {
                copyFile();
            } else {
                touchFile();
            }
        } else if (isDirectory()) {

            for (FileArtifact child : getChildren()) {
                child.writeContent();
            }
        }
    }

    /**
     * Writes the {@link #content} of this {@link FileArtifact} to its {@link #file}.
     *
     * @throws IOException
     *         see {@link FileUtils#openOutputStream(File)}
     */
    private void writeToFile() throws IOException {
        try (OutputStreamWriter out = new OutputStreamWriter(FileUtils.openOutputStream(file), UTF_8)) {
            out.write(content);
        }
    }

    /**
     * Copies the {@link #original} to the {@link #file} of this {@link FileArtifact}.
     *
     * @throws IOException
     *         see {@link FileUtils#copyFile(File, File)}
     */
    private void copyFile() throws IOException {
        if (!Files.isSameFile(original.toPath(), file.toPath())) {
            FileUtils.copyFile(original, file);
        }
    }

    /**
     * Creates an empty version of the {@link #file} on disk.
     *
     * @throws IOException
     *         see {@link FileUtils#touch(File)}
     */
    private void touchFile() throws IOException {
        FileUtils.touch(file);
    }

    /**
     * Returns the content of the {@link File} this {@link FileArtifact} represents. Will return an empty {@link String}
     * if there is an exception reading the content of non-virtual {@link FileArtifact FileArtifacts} or if the
     * {@link FileArtifact} is virtual and the content was not set to something other than an empty {@link String}.
     * Also returns an empty {@link String} for directories.
     *
     * @return the content this {@link FileArtifact} represents
     */
    public String getContent() {

        if (isDirectory()) {
            LOG.warning("Returning an empty string as the contents of the directory " + file);
            return "";
        }

        if (content == null) {
            String content;

            if (original == null) {
                content = "";
            } else {
                try {
                    content = FileUtils.readFileToString(original, UTF_8);
                } catch (IOException e) {
                    LOG.log(Level.WARNING, e, () -> "Could not read the contents of " + this);
                    return "";
                }
            }

            this.content = content;
        }

        return content;
    }

    /**
     * Sets the content this {@link FileArtifact} represents to the new value. If this {@link FileArtifact} represents
     * a directory, the call is ignored.
     *
     * @param content
     *         the new content
     */
    public void setContent(String content) {

        if (isFile()) {
            this.content = content;
        } else {
            LOG.warning("Ignoring a call to setContent(String) on a FileArtifact representing a directory.");
        }
    }
}
