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
package de.fosd.jdime.artifact.ast;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.artifact.ArtifactList;
import de.fosd.jdime.artifact.Artifacts;
import de.fosd.jdime.artifact.file.FileArtifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.config.merge.MergeScenario;
import de.fosd.jdime.config.merge.Revision;
import de.fosd.jdime.merge.Merge;
import de.fosd.jdime.operations.ConflictOperation;
import de.fosd.jdime.operations.MergeOperation;
import de.fosd.jdime.operations.Operation;
import de.fosd.jdime.stats.KeyEnums;
import de.fosd.jdime.stats.MergeScenarioStatistics;
import org.jastadd.extendj.ast.ASTNode;
import org.jastadd.extendj.ast.BytecodeParser;
import org.jastadd.extendj.ast.BytecodeReader;
import org.jastadd.extendj.ast.ClassDecl;
import org.jastadd.extendj.ast.ConstructorDecl;
import org.jastadd.extendj.ast.ImportDecl;
import org.jastadd.extendj.ast.InterfaceDecl;
import org.jastadd.extendj.ast.JavaParser;
import org.jastadd.extendj.ast.Literal;
import org.jastadd.extendj.ast.MethodDecl;
import org.jastadd.extendj.ast.Program;
import org.jastadd.extendj.ast.TryStmt;

import static de.fosd.jdime.strdump.DumpMode.PLAINTEXT_TREE;

/**
 * @author Olaf Lessenich
 *
 */
public class ASTNodeArtifact extends Artifact<ASTNodeArtifact> {

    private static final Logger LOG = Logger.getLogger(ASTNodeArtifact.class.getCanonicalName());

    /**
     * Initializes parser.
     *
     * @param p
     *            program
     */
    private static void initParser(Program p) {
        JavaParser parser = (is, fileName) -> new org.jastadd.extendj.parser.JavaParser().parse(is, fileName);
        BytecodeReader bytecodeParser = (is, fullName, program) -> new BytecodeParser(is, fullName).parse(null, null, program);

        p.initJavaParser(parser);
        p.initBytecodeReader(bytecodeParser);
    }

    /**
     * Parses the content of the given <code>FileArtifact</code> to an AST. If the <code>artifact</code> is empty,
     * an empty <code>ASTNode</code> obtained via {@link ASTNode#ASTNode()} will be returned.
     *
     * @param artifact
     *         the <code>FileArtifact</code> to parse
     * @return the root of the resulting AST
     */
    private static ASTNode<?> parse(FileArtifact artifact) {
        ASTNode<?> astNode;

        if (artifact.isEmpty()) {
            astNode = new ASTNode<>();
        } else {
            Program p = initProgram();

            try {
                p.addSourceFile(artifact.getPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            astNode = p;
        }

        return astNode;
    }

    /**
     * Initializes a program.
     *
     * @return program
     */
    private static Program initProgram() {
        Program program = new Program();
        program.state().reset();
        initParser(program);
        return program;
    }

    /**
     * @param artifact
     *            artifact to create program from
     * @return ASTNodeArtifact
     */
    public static ASTNodeArtifact createProgram(ASTNodeArtifact artifact) {
        assert (artifact.astnode != null);
        assert (artifact.astnode instanceof Program);

        Program old = (Program) artifact.astnode;
        Program program;

        try {
            program = old.clone();
        } catch (CloneNotSupportedException e) {
            program = new Program();
        }

        ASTNodeArtifact p = new ASTNodeArtifact(artifact.getRevision(), program);
        p.deleteChildren();

        return p;
    }

    /**
     * Encapsulated ASTNode.
     */
    private ASTNode<?> astnode = null;

    /**
     * Constructs a new <code>ASTNodeArtifact</code> (tree) representing the AST of the code in <code>artifact</code>.
     * All members of the tree will be in the same <code>Revision</code> as <code>artifact</code>.
     *
     * @param artifact
     *         the <code>FileArtifact</code> containing the code to be parsed
     */
    public ASTNodeArtifact(FileArtifact artifact) {
        this(artifact.getRevision(), new AtomicInteger()::getAndIncrement, parse(artifact));
    }

    /**
     * Constructs a new <code>ASTNodeArtifact</code> encapsulating an empty <code>ASTNode</code> obtained via
     * {@link ASTNode#ASTNode()}.
     *
     * @param revision
     *         the <code>Revision</code> for this <code>ASTNodeArtifact</code>
     */
    private ASTNodeArtifact(Revision revision) {
        this(revision, new AtomicInteger()::getAndIncrement, new ASTNode<>());
    }

    /**
     * Constructs a new <code>ASTNodeArtifact</code> encapsulating the given <code>ASTNode</code>. Children
     * <code>ASTNodeArtifact</code>s for all the children of <code>astNode</code> will be added.
     *
     * @param revision
     *         the <code>Revision</code> for this <code>ASTNodeArtifact</code>
     * @param astNode
     *         the <code>ASTNode</code> to encapsulate
     */
    private ASTNodeArtifact(Revision revision, ASTNode<?> astNode) {
        this(revision, new AtomicInteger()::getAndIncrement, astNode);
    }

    /**
     * Constructs a new <code>ASTNodeArtifact</code> encapsulating the given <code>ASTNode</code>. Children
     * <code>ASTNodeArtifact</code>s for all the children of <code>astNode</code> will be added.
     *
     * @param revision
     *         the <code>Revision</code> for this <code>ASTNodeArtifact</code>
     * @param number
     *         supplies first the number for this artifact and then in DFS order the number for its children
     * @param astNode
     *         the <code>ASTNode</code> to encapsulate
     */
    private ASTNodeArtifact(Revision revision, Supplier<Integer> number, ASTNode<?> astNode) {
        super(revision, number.get());

        this.astnode = astNode;
        initializeChildren(number);
    }

    /**
     * Copies the given {@link Artifact}.
     *
     * @param toCopy
     *         to {@link Artifact} to copy
     * @see #copy()
     */
    private ASTNodeArtifact(ASTNodeArtifact toCopy) {
        super(toCopy);

        try {
            this.astnode = toCopy.astnode.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds {@code ASTNodeArtifact} children to this artifact encapsulating the children of the {@link #astnode}.
     *
     * @param number
     *         supplies the numbers for the added children
     */
    private void initializeChildren(Supplier<Integer> number) {
        List<ASTNodeArtifact> children = new ArtifactList<>();

        for (int i = 0; i < astnode.getNumChild(); i++) {
            ASTNodeArtifact child = new ASTNodeArtifact(getRevision(), number, astnode.getChild(i));

            child.setParent(this);
            children.add(child);
        }

        setChildren(children);
    }

    /**
     * Returns the encapsulated JastAddJ ASTNode
     *
     * @return encapsulated ASTNode object from JastAddJ
     */
    public final ASTNode<?> getASTNode() {
        return astnode;
    }

    @Override
    public ASTNodeArtifact copy() {
        return new ASTNodeArtifact(this);
    }

    @Override
    public ASTNodeArtifact addChild(ASTNodeArtifact child) {
        LOG.finest(() -> String.format("%s.addChild(%s)", getId(), child.getId()));

        assert (this.exists());
        assert (child.exists());

        modifyChildren(children -> children.add(child));
        child.setParent(this);

        return child;
    }

    @Override
    public ASTNodeArtifact createEmptyArtifact(Revision revision) {
        return new ASTNodeArtifact(revision);
    }

    @Override
    public void deleteChildren() {
        modifyChildren(List::clear);
    }

    @Override
    public String prettyPrint() {
        assert (astnode != null);

        try {
            rebuildAST();
            astnode.flushCaches();
            astnode.flushTreeCache();
        } catch (Exception e) {
            LOG.severe("Exception caught during prettyPrint(): " + e);
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }

        LOG.finest(() -> Artifacts.root(this).dump(PLAINTEXT_TREE));

        String indent = isRoot() ? "" : astnode.extractIndent();
        String prettyprint = indent + astnode.prettyPrint();

        if (prettyprint.trim().length() == 0) {
            throw new RuntimeException("Error: Could not pretty-print file!");
        }

        return prettyprint;
    }

    @Override
    public final boolean exists() {
        return astnode != null;
    }

    @Override
    public final String getId() {
        return getRevision() + ":" + getNumber();
    }

    @Override
    protected String hashId() {
        return astnode.getMatchingRepresentation();
    }

    @Override
    public KeyEnums.Type getType() {
        if (isMethod()) {
            return KeyEnums.Type.METHOD;
        } else if (isClass()) {
            return KeyEnums.Type.CLASS;
        } else if (astnode instanceof TryStmt){
            return KeyEnums.Type.TRY;
        } else {
            return KeyEnums.Type.NODE;
        }
    }

    @Override
    public KeyEnums.Level getLevel() {
        KeyEnums.Type type = getType();

        if (type == KeyEnums.Type.METHOD) {
            return KeyEnums.Level.METHOD;
        } else if (type == KeyEnums.Type.CLASS) {
            return KeyEnums.Level.CLASS;
        } else {

            if (getParent() == null) {
                return KeyEnums.Level.TOP;
            } else {
                return getParent().getLevel();
            }
        }
    }

    @Override
    public void mergeOpStatistics(MergeScenarioStatistics mScenarioStatistics, MergeContext mergeContext) {
        mScenarioStatistics.getTypeStatistics(getRevision(), getType()).incrementNumMerged();
        mScenarioStatistics.getLevelStatistics(getRevision(), getLevel()).incrementNumMerged();
    }

    /**
     * Returns whether this <code>ASTNodeArtifact</code> represents a method declaration.
     *
     * @return true iff this is a method declaration
     */
    private boolean isMethod() {
        return astnode instanceof MethodDecl || astnode instanceof ConstructorDecl;
    }

    /**
     * Returns whether the <code>ASTNodeArtifact</code> is within a method.
     *
     * @return true iff the <code>ASTNodeArtifact</code> is within a method
     */
    public boolean isWithinMethod() {
        ASTNodeArtifact parent = getParent();
        return parent != null && (parent.isMethod() || parent.isWithinMethod());
    }

    /**
     * Returns whether this <code>ASTNodeArtifact</code> represents a class or interface declaration.
     *
     * @return true iff this is a class or method declaration
     */
    private boolean isClass() {
        return astnode instanceof ClassDecl || astnode instanceof InterfaceDecl;
    }

    @Override
    public Optional<Supplier<String>> getUniqueLabel() {
        boolean hasLabel = ImportDecl.class.isAssignableFrom(astnode.getClass())
                            || Literal.class.isAssignableFrom(astnode.getClass());

        return hasLabel ? Optional.of(() -> astnode.dumpString()) : Optional.empty();
    }

    @Override
    public final boolean isEmpty() {
        return !hasChildren();
    }

    /**
     * Returns whether declaration order is significant for this node.
     *
     * @return whether declaration order is significant for this node
     */
    @Override
    public final boolean isOrdered() {
        return astnode.isOrdered();
    }

    /**
     * Returns whether a node matches another node.
     *
     * @param other
     *            node to compare with.
     * @return true if the node matches another node.
     */
    @Override
    public final boolean matches(final ASTNodeArtifact other) {
        assert (astnode != null);
        assert (other != null);
        assert (other.astnode != null);

        LOG.finest(() -> "match(" + getId() + ", " + other.getId() + ")");

        LOG.finest(() -> {
            return String.format("Try Matching: {%s} and {%s}",
                    astnode.getMatchingRepresentation(),
                    other.astnode.getMatchingRepresentation());
        });

        return astnode.matches(other.astnode);
    }

    @Override
    public void merge(MergeOperation<ASTNodeArtifact> operation, MergeContext context) {
        Objects.requireNonNull(operation, "operation must not be null!");
        Objects.requireNonNull(context, "context must not be null!");

        MergeScenario<ASTNodeArtifact> triple = operation.getMergeScenario();
        ASTNodeArtifact left = triple.getLeft();
        ASTNodeArtifact right = triple.getRight();
        ASTNodeArtifact target = operation.getTarget();

        boolean safeMerge = true;

        int numChildNoTransform;
        try {
            numChildNoTransform = target.astnode.getClass().newInstance().getNumChildNoTransform();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        if (!isRoot() && numChildNoTransform > 0) {

            // this language element has a fixed number of children, we need to be careful with this one
            // as it might cause lots of issues while being pretty-printed
            boolean leftChanges = left.isChange();
            boolean rightChanges = right.isChange();

            for (int i = 0; !leftChanges && i < left.getNumChildren(); i++) {
                leftChanges = left.getChild(i).isChange();
            }

            for (int i = 0; !rightChanges && i < right.getNumChildren(); i++) {
                rightChanges = right.getChild(i).isChange();
            }

            if (leftChanges && rightChanges) {
                // this one might be trouble

                if (left.getNumChildren() == right.getNumChildren()) {
                    // so far so good

                    for (int i = 0; i < left.getNumChildren(); i++) {
                        if (!left.getChild(i).astnode.getClass().getName().equals(right.getChild(i).astnode.getClass().getName())) {
                            // no good, this might get us some ClassCastExceptions
                            safeMerge = false;
                        }
                    }
                } else {
                    // no way ;)
                    safeMerge = false;
                }

            }
        }

        if (safeMerge) {
            Merge<ASTNodeArtifact> merge = new Merge<>();

            LOG.finest(() -> "Merging ASTs " + operation.getMergeScenario());
            merge.merge(operation, context);
        } else {
            LOG.finest(() -> String.format("Target %s expects a fixed amount of children.", target.getId()));
            LOG.finest(() -> String.format("Both %s and %s contain changes.", left.getId(), right.getId()));
            LOG.finest(() -> "We are scared of this node and report a conflict instead of performing the merge.");

            // to be safe, we will report a conflict instead of merging
            ASTNodeArtifact targetParent = target.getParent();
            targetParent.removeChild(target);

            Operation<ASTNodeArtifact> conflictOp = new ConflictOperation<>(left, right, targetParent,
                    left.getRevision().getName(), right.getRevision().getName());
            conflictOp.apply(context);
        }
    }

    /**
     * Removes a child.
     *
     * @param child
     *            child that should be removed
     */
    private void removeChild(final ASTNodeArtifact child) {
        LOG.finest(() -> String.format("[%s] Removing child %s", getId(), child.getId()));
        LOG.finest(() -> String.format("Children before removal: %s", getChildren()));

        modifyChildren(children -> children.removeIf(it -> it == child));

        LOG.finest(() -> String.format("Children after removal: %s", getChildren()));
    }

    /**
     * Rebuild the encapsulated ASTNode tree top down. This should be only
     * called at the root node
     */
    private void rebuildAST() {
        LOG.finest(() -> String.format("%s.rebuildAST()", getId()));
        int oldNumChildren = astnode.getNumChildNoTransform();

        if (isConflict()) {
            astnode.isConflict = true;
            astnode.jdimeId = getId();

            if (left != null) {
                left.rebuildAST();
                astnode.left = left.astnode;
            } else {
                /* FIXME: this is actually a bug.
                 * JDime should use an empty ASTNode with the correct revision information.
                 */
            }

            if (right != null) {
                right.rebuildAST();
                astnode.right = right.astnode;
            } else {
                /* FIXME: this is actually a bug.
                 * JDime should use an empty ASTNode with the correct revision information.
                 */
            }
        }

        if (isChoice()) {
            astnode.isChoice = true;
            astnode.jdimeId = getId();
            astnode.variants = new LinkedHashMap<String, ASTNode<?>>();

            for (String condition : variants.keySet()) {
                ASTNodeArtifact variant = variants.get(condition);
                variant.rebuildAST();
                astnode.variants.put(condition, variant.astnode);
            }
        }

        ASTNode<?>[] newChildren = new ASTNode<?>[getNumChildren()];

        for (int i = 0; i < getNumChildren(); i++) {
            ASTNodeArtifact child = getChild(i);
            newChildren[i] = child.astnode;
            newChildren[i].setParent(astnode);
            child.rebuildAST();
        }

        astnode.jdimeChanges = hasChanges();
        astnode.jdimeId = getId();
        astnode.setChildren(newChildren);

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest(() -> String.format("jdime: %d, astnode.before: %d, astnode.after: %d children", getNumChildren(), oldNumChildren,
                    astnode.getNumChildNoTransform()));
            if (getNumChildren() != astnode.getNumChildNoTransform()) {
                LOG.finest("mismatch between jdime and astnode for " + getId() + "(" + astnode.dumpString() + ")");
            }
            if (oldNumChildren != astnode.getNumChildNoTransform()) {
                LOG.finest("Number of children has changed");
            }
        }

        if (!isConflict() && getNumChildren() != astnode.getNumChildNoTransform()) {
            StringBuilder elements = new StringBuilder();
            for (Revision r : getMatches().keySet()) {
                if (elements.length() > 0) {
                    elements.append(", ");
                }
                elements.append(getMatching(r).getMatchingArtifact(this).getId());
            }

            LOG.severe("Mismatch of getNumChildren() and astnode.getNumChildren()---" +
                    "This is either a bug in ExtendJ or in JDime! Inspect AST element " +
                    getId() + " (" + elements.toString() + ") to look into this issue.");
        }
    }

    @Override
    public final String toString() {
        return astnode.dumpString();
    }

    @Override
    public ASTNodeArtifact createConflictArtifact(ASTNodeArtifact left, ASTNodeArtifact right) {
        ASTNodeArtifact conflict;

        if (left != null) {
            conflict = new ASTNodeArtifact(MergeScenario.CONFLICT, left.astnode.treeCopyNoTransform());
        } else {
            conflict = new ASTNodeArtifact(MergeScenario.CONFLICT, right.astnode.treeCopyNoTransform());
        }

        conflict.setConflict(left, right);

        return conflict;
    }

    @Override
    public ASTNodeArtifact createChoiceArtifact(String condition, ASTNodeArtifact artifact) {
        LOG.fine("Creating choice node");

        ASTNodeArtifact choice = new ASTNodeArtifact(MergeScenario.CHOICE, artifact.astnode.treeCopyNoTransform());
        choice.setChoice(condition, artifact);

        return choice;
    }
}
