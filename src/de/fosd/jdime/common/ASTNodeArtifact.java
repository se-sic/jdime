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

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.fosd.jdime.common.operations.ConflictOperation;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.common.operations.Operation;
import de.fosd.jdime.merge.Merge;
import de.fosd.jdime.stats.KeyEnums;
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
import static de.fosd.jdime.strdump.DumpMode.PRETTY_PRINT_DUMP;

/**
 * @author Olaf Lessenich
 *
 */
public class ASTNodeArtifact extends Artifact<ASTNodeArtifact> {

    private static final Logger LOG = Logger.getLogger(ASTNodeArtifact.class.getCanonicalName());

    private boolean initialized = false;

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
    public static ASTNodeArtifact createProgram(final ASTNodeArtifact artifact) {
        assert (artifact.astnode != null);
        assert (artifact.astnode instanceof Program);

        Program old = (Program) artifact.astnode;
        Program program;
        try {
            program = old.clone();
        } catch (CloneNotSupportedException e) {
            program = new Program();
        }

        ASTNodeArtifact p = new ASTNodeArtifact(program, null);
        p.deleteChildren();

        return p;
    }

    /**
     * Encapsulated ASTNode.
     */
    private ASTNode<?> astnode = null;

    /**
     * Constructor class.
     */
    private ASTNodeArtifact() {
        this.astnode = new ASTNode<>();
        this.initializeChildren();
    }

    /**
     * @param astnode
     *            astnode
     */
    private ASTNodeArtifact(final ASTNode<?> astnode, Revision revision) {
        assert (astnode != null);
        this.astnode = astnode;
        setRevision(revision);
        initializeChildren();
    }

    private void initializeChildren() {
        ArtifactList<ASTNodeArtifact> children = new ArtifactList<>();
        for (int i = 0; i < astnode.getNumChild(); i++) {
            if (astnode != null) {
                ASTNodeArtifact child = new ASTNodeArtifact(astnode.getChild(i), getRevision());
                child.setParent(this);
                child.setRevision(getRevision());
                children.add(child);
                if (!child.initialized) {
                    child.initializeChildren();
                }
            }
        }
        setChildren(children);
        initialized = true;
    }

    /**
     * Constructs an ASTNodeArtifact from a FileArtifact.
     *
     * @param artifact
     *            file artifact
     */
    public ASTNodeArtifact(FileArtifact artifact) {
        assert (artifact != null);

        setRevision(artifact.getRevision());

        ASTNode<?> astnode;
        if (artifact.isEmpty()) {
            astnode = new ASTNode<>();
        } else {
            Program p = initProgram();

            try {
                p.addSourceFile(artifact.getPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            astnode = p;
        }

        this.astnode = astnode;
        initializeChildren();
        renumberTree();

        LOG.finest(() -> String.format("created new ASTNodeArtifact for revision %s", getRevision()));
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
    public ASTNodeArtifact clone() {
        assert (exists());

        ASTNodeArtifact clone = null;

        try {
            clone = new ASTNodeArtifact(astnode.clone(), getRevision());
            clone.setRevision(getRevision());
            clone.setNumber(getNumber());
            clone.cloneMatches(this);

            ArtifactList<ASTNodeArtifact> cloneChildren = new ArtifactList<>();
            for (ASTNodeArtifact child : children) {
                ASTNodeArtifact cloneChild = child.clone();
                cloneChild.astnode.setParent(clone.astnode);
                cloneChild.setParent(clone);
                cloneChildren.add(cloneChild);
            }
            clone.setChildren(cloneChildren);
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        assert (clone.exists());

        return clone;
    }

    @Override
    public ASTNodeArtifact addChild(ASTNodeArtifact child) {
        LOG.finest(() -> String.format("%s.addChild(%s)", getId(), child.getId()));

        assert (this.exists());
        assert (child.exists());

        child.setParent(this);
        children.add(child);

        return child;
    }

    @Override
    public ASTNodeArtifact createEmptyArtifact() {
        ASTNodeArtifact emptyArtifact= new ASTNodeArtifact();
        emptyArtifact.setRevision(getRevision());
        return emptyArtifact;
    }

    public void deleteChildren() {
        while (hasChildren()) {
            ASTNodeArtifact child = getChild(0);
            child.astnode = null;
            children.remove(0);
        }
    }

    @Override
    public String prettyPrint() {
        assert (astnode != null);

        rebuildAST();
        astnode.flushCaches();
        astnode.flushTreeCache();

        if (LOG.isLoggable(Level.FINEST)) {
            System.out.println(findRoot().dump(PLAINTEXT_TREE));
        }

        String indent = isRoot() ? "" : astnode.extractIndent();
        return indent + astnode.prettyPrint();
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

    @Override
    public final boolean isLeaf() {
        // TODO Auto-generated method stub
        return false;
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

        if (!context.isQuiet() && context.hasOutput()) {
            System.out.print(context.getStdIn());
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

        Iterator<ASTNodeArtifact> it = children.iterator();
        ASTNodeArtifact elem;
        while (it.hasNext()) {
            elem = it.next();
            if (elem == child) {
                it.remove();
            }
        }

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

        ASTNode<?>[] newchildren = new ASTNode[getNumChildren()];

        for (int i = 0; i < getNumChildren(); i++) {
            ASTNodeArtifact child = getChild(i);
            newchildren[i] = child.astnode;
            newchildren[i].setParent(astnode);
            child.rebuildAST();
        }

        astnode.jdimeChanges = hasChanges();
        astnode.jdimeId = getId();
        astnode.setChildren(newchildren);

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
            throw new RuntimeException("Mismatch of getNumChildren() and astnode.getNumChildren()---" +
                    "This is either a bug in ExtendJ or in JDime!");
        }
    }

    @Override
    public final String toString() {
        return astnode.dumpString();
    }

    @Override
    public final ASTNodeArtifact createConflictArtifact(final ASTNodeArtifact left, final ASTNodeArtifact right) {
        ASTNodeArtifact conflict = left != null
                ? new ASTNodeArtifact(left.astnode.treeCopyNoTransform(), null)
                : new ASTNodeArtifact(right.astnode.treeCopyNoTransform(), null);

        conflict.setRevision(MergeScenario.CONFLICT);
        conflict.setNumber(virtualcount++);
        conflict.setConflict(left, right);

        return conflict;
    }

    @Override
    public final ASTNodeArtifact createChoiceArtifact(final String condition, final ASTNodeArtifact artifact) {
        LOG.fine("Creating choice node");
        ASTNodeArtifact choice;

        choice = new ASTNodeArtifact(artifact.astnode.treeCopyNoTransform(), null);
        choice.setRevision(MergeScenario.CHOICE);
        choice.setNumber(virtualcount++);
        choice.setChoice(condition, artifact);
        return choice;
    }

    /**
     * Inspects an artifact. This can be used to retrieve information about a single artifact.
     */
    public String inspect() {
        return dump(PRETTY_PRINT_DUMP);
    }
}
