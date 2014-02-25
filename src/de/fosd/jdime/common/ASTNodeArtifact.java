/* 
 * Copyright (C) 2013 Olaf Lessenich.
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
 */
package de.fosd.jdime.common;

import AST.ASTNode;
import AST.BytecodeParser;
import AST.ClassDecl;
import AST.CompilationUnit;
import AST.ConstructorDecl;
import AST.FieldDecl;
import AST.FieldDeclaration;
import AST.ImportDecl;
import AST.InterfaceDecl;
import AST.JavaParser;
import AST.Literal;
import AST.MethodDecl;
import AST.Program;
import de.fosd.jdime.common.operations.ConflictOperation;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.matcher.Color;
import de.fosd.jdime.matcher.Matching;
import de.fosd.jdime.strategy.ASTNodeStrategy;
import de.fosd.jdime.strategy.MergeStrategy;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import org.apache.log4j.Logger;

/**
 * @author Olaf Lessenich
 *
 */
public class ASTNodeArtifact extends Artifact<ASTNodeArtifact> {

    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(ASTNodeArtifact.class);

    /**
     * Initializes parser.
     *
     * @param p program
     */
    private static void initParser(final Program p) {
        p.initJavaParser(new JavaParser() {
            @Override
            public CompilationUnit parse(final java.io.InputStream is,
                    final String fileName) throws java.io.IOException,
                    beaver.Parser.Exception {
                return new parser.JavaParser().parse(is, fileName);
            }
        });
    }

    /**
     * Initializes a program.
     *
     * @return program
     */
    private static Program initProgram() {
        Program program = new Program();
        program.state().reset();
        program.initBytecodeReader(new BytecodeParser());
        initParser(program);
        return program;
    }

    /**
     * @param artifact artifact to create program from
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

        ASTNodeArtifact p = new ASTNodeArtifact(program);
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
    public ASTNodeArtifact() {
    }

    /**
     * @param astnode astnode
     */
    public ASTNodeArtifact(final ASTNode<?> astnode) {
        assert (astnode != null);
        this.astnode = astnode;
    }

    /**
     * Constructs an ASTNodeArtifact from a FileArtifact.
     *
     * @param artifact file artifact
     */
    public ASTNodeArtifact(final FileArtifact artifact) {
        assert (artifact != null);

        setRevision(artifact.getRevision());

        ASTNode<?> astnode;
        if (artifact.isEmptyDummy()) {
            astnode = new ASTNode<>();
            setEmptyDummy(true);
        } else {
            Program p = initProgram();
            p.addSourceFile(artifact.getPath());
            astnode = p;
        }

        this.astnode = astnode;
        renumber(1, this);
    }

    /**
     * Returns the encapsulated ASTNode. Debugging method only. TODO: Remove
     * this in a later.
     *
     * @return encapsulated ASTnode
     */
    public final ASTNode<?> getASTNode() {
        return astnode;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * de.fosd.jdime.common.Artifact#addChild(de.fosd.jdime.common.Artifact)
     */
    @Override
    public final ASTNodeArtifact addChild(final ASTNodeArtifact child) throws
            IOException {
        if (child.isConflict()) {
            child.setParent(this);
            children.add(child);
            return child;
        }

        ASTNodeArtifact myChild;
        try {
            myChild = new ASTNodeArtifact((ASTNode<?>) child.astnode.clone());
            myChild.deleteChildren();
            myChild.setRevision(child.getRevision());
            myChild.setParent(this);
            myChild.astnode.setParent(astnode);
            myChild.setRevision(child.getRevision());
            myChild.setNumber(child.getNumber());
            myChild.cloneMatches(child);

            if (children == null) {
                initializeChildren();
            }
            children.add(myChild);
            // astnode.flushCaches();
            if (LOG.isTraceEnabled()) {
                LOG.trace("Added child " + myChild.getId() + " to parent node "
                        + getId());
            }
            return myChild;
        } catch (CloneNotSupportedException e) {
            throw new NotYetImplementedException();
        }

    }

    @Override
    public final int compareTo(final ASTNodeArtifact o) {
        if (hasUniqueLabels()) {
            return astnode.dumpString().compareTo(o.astnode.dumpString());
        } else {
            throw new RuntimeException("This artifact is not comparable.");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * de.fosd.jdime.common.Artifact#copyArtifact(de.fosd.jdime.common.Artifact)
     */
    @Override
    public final void copyArtifact(final ASTNodeArtifact destination) throws
            IOException {
        ASTNodeArtifact copy = destination.addChild(this);
        if (!isConflict() && hasChildren()) {
            for (ASTNodeArtifact child : getChildren()) {
                child.copyArtifact(copy);
            }
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see de.fosd.jdime.common.Artifact#createArtifact(boolean)
     */
    @Override
    public final void createArtifact(final boolean isLeaf)
            throws IOException {
        // TODO Auto-generated method stub
    }

    /*
     * (non-Javadoc)
     *
     * @see de.fosd.jdime.common.Artifact#createEmptyDummy()
     */
    @Override
    public final ASTNodeArtifact createEmptyDummy() throws FileNotFoundException {
        ASTNodeArtifact dummy = new ASTNodeArtifact();
        dummy.astnode = new ASTNode<>();
        dummy.setEmptyDummy(true);
        dummy.setRevision(getRevision());
        return dummy;
    }

    /**
     *
     */
    public final void deleteChildren() {
        while (hasChildren()) {
            ASTNodeArtifact child = getChild(0);
            child.astnode = null;
            children.remove(0);
        }
    }

    /**
     * Returns the AST in dot-format.
     *
     * @param includeNumbers include node number in label if true
     * @return AST in dot-format.
     */
    public final String dumpGraphvizTree(final boolean includeNumbers) {
        assert (astnode != null);
        StringBuilder sb = new StringBuilder();
        sb.append(getNumber()).append("[label=\"");

        // node label
        if (includeNumbers) {
            sb.append("(").append(getNumber()).append(") ");
        }

        sb.append(astnode.dumpString());

        sb.append("\"");

        if (hasMatches()) {
            sb.append(", fillcolor = green, style = filled");
        }

        sb.append("];");
        sb.append(System.lineSeparator());

        // children
        for (ASTNodeArtifact child : getChildren()) {
            sb.append(child.dumpGraphvizTree(includeNumbers));

            // edge
            sb.append(getNumber()).append("->").append(child.getNumber())
                    .append(";").append(System.lineSeparator());
        }

        return sb.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see de.fosd.jdime.common.Artifact#dumpTree(java.lang.String)
     */
    @Override
    protected final String dumpTree(final String indent) {
        assert (astnode != null);
        StringBuilder sb = new StringBuilder();

        // node itself

        Matching<ASTNodeArtifact> m = null;

        // color
        if (!isConflict() && hasMatches()) {

            Set<Revision> matchingRevisions = matches.keySet();

            // print color code
            String color = "";

            for (Revision rev : matchingRevisions) {
                m = getMatching(rev);
                color = m.getColor().toShell();
            }

            sb.append(color);
        }

        if (isConflict()) {
            sb.append(Color.RED.toShell());
            sb.append(indent).append("(").append(getId()).append(") ");
            sb.append(this);
            sb.append(System.lineSeparator());
            sb.append(Color.RED.toShell());
            sb.append("<<<<<<< ");
            sb.append(System.lineSeparator());
            // children
            if (left != null) {
                sb.append(left.dumpTree(indent));
            }
            sb.append(Color.RED.toShell());
            sb.append("======= ");
            sb.append(System.lineSeparator());
            // children
            if (right != null) {
                sb.append(right.dumpTree(indent));
            }

            sb.append(Color.RED.toShell());
            sb.append(">>>>>>> ");
            sb.append(System.lineSeparator());
        } else {
            sb.append(indent).append("(").append(getId()).append(") ");
            sb.append(this);

            if (hasMatches()) {
                assert (m != null);
                sb.append(" <=> (").append(m.getMatchingArtifact(this).getId())
                        .append(")");
                sb.append(Color.DEFAULT.toShell());
            }
            sb.append(System.lineSeparator());

            // children
            for (ASTNodeArtifact child : getChildren()) {
                sb.append(child.dumpTree(indent + "  "));
            }
        }

        return sb.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    //	@Override
    @Override
    public final boolean exists() {
        return astnode != null;
    }

    /*
     * (non-Javadoc)
     *
     * @see de.fosd.jdime.common.Artifact#getId()
     */
    @Override
    public final String getId() {
        return getRevision() + ":" + getNumber();
    }

    @Override
    public final String getStatsKey(final MergeContext context) {
        return "nodes";
    }

    /*
     * (non-Javadoc)
     *
     * @see de.fosd.jdime.common.Artifact#hashCode()
     */
    @Override
    public final int hashCode() {
        return astnode.dumpString().hashCode();
    }

    @Override
    public final boolean hasUniqueLabels() {
        return ImportDecl.class.isAssignableFrom(astnode.getClass())
                || Literal.class.isAssignableFrom(astnode.getClass());
    }

    /*
     * (non-Javadoc)
     *
     * @see de.fosd.jdime.common.Artifact#initializeChildren()
     */
    @Override
    public final void initializeChildren() {
        assert (astnode != null);
        ArtifactList<ASTNodeArtifact> children = new ArtifactList<>();
        for (int i = 0; i < astnode.getNumChildNoTransform(); i++) {
            ASTNodeArtifact child = new ASTNodeArtifact(
                    astnode.getChild(i));
            child.setParent(this);
            child.setRevision(getRevision());
            children.add(child);
        }
        setChildren(children);
    }

    /*
     * (non-Javadoc)
     *
     * @see de.fosd.jdime.common.Artifact#isEmpty()
     */
    @Override
    public final boolean isEmpty() {
        return !hasChildren();
    }

    /*
     * (non-Javadoc)
     *
     * @see de.fosd.jdime.common.Artifact#isLeaf()
     */
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
        return !ConstructorDecl.class.isAssignableFrom(astnode.getClass())
                && !MethodDecl.class.isAssignableFrom(astnode.getClass())
                && !InterfaceDecl.class.isAssignableFrom(astnode.getClass())
                && !FieldDecl.class.isAssignableFrom(astnode.getClass())
                && !FieldDeclaration.class.isAssignableFrom(astnode.getClass())
                && !ImportDecl.class.isAssignableFrom(astnode.getClass());
    }

    /**
     * Returns whether a node matches another node.
     *
     * @param other node to compare with.
     * @return true if the node matches another node.
     */
    @Override
    public final boolean matches(final ASTNodeArtifact other) {
        assert (astnode != null);
        assert (other != null);
        assert (other.astnode != null);

        if ((ImportDecl.class.isAssignableFrom(astnode.getClass())
                || Literal.class.isAssignableFrom(astnode.getClass()))
                && other.astnode.getClass().equals(astnode.getClass())) {
            return astnode.toString().equals(other.astnode.toString());
        }

        return astnode.dumpString().equals(other.astnode.dumpString());
    }

    /*
     * (non-Javadoc)
     *
     * @see de.fosd.jdime.common.Artifact#merge(de.fosd.jdime.common.operations.
     * MergeOperation, de.fosd.jdime.common.MergeContext)
     */
    @Override
    public final void merge(final MergeOperation<ASTNodeArtifact> operation,
            final MergeContext context) throws IOException, InterruptedException {
        assert (operation != null);
        assert (context != null);

        MergeStrategy<ASTNodeArtifact> strategy = new ASTNodeStrategy();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Using strategy: " + strategy.toString());
        }

        MergeTriple<ASTNodeArtifact> triple = operation.getMergeTriple();
        assert (triple != null);
        ASTNodeArtifact left, right, target;
        left = triple.getLeft();
        right = triple.getRight();
        target = operation.getTarget();

        boolean safeMerge = true;

        try {
            if (!isRoot()
                    && target.astnode.getClass().newInstance()
                    .getNumChildNoTransform() > 0) {
                // this language element has a fixed number of children
                // we need to be careful with this one
                boolean leftChanges = left.hasChanges(false);
                boolean rightChanges = right.hasChanges(false);

                if (leftChanges && rightChanges) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("target " + target.getId()
                                + " expects a fixed amount of children.");
                        LOG.trace("changes in " + left.getId() + ": "
                                + leftChanges);
                        LOG.trace("changes in " + right.getId() + ": "
                                + rightChanges);
                        LOG.trace("We will report a conflict "
                                + "instead of performing the merge");
                    }
                    safeMerge = false;
                    // to be safe, we will report a conflict instead of merging
                    ASTNodeArtifact targetParent = target.getParent();
                    targetParent.removeChild(target);
                    ConflictOperation<ASTNodeArtifact> conflictOp =
                            new ConflictOperation<>(left, left, right,
                            targetParent);
                    conflictOp.apply(context);
                }
            }
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException();
        }

        if (safeMerge) {
            strategy.merge(operation, context);
        }

        if (!context.isQuiet() && context.hasOutput()) {
            System.out.println(context.getStdIn());
        }
    }

    /**
     * Removes a child.
     *
     * @param child child that should be removed
     */
    public final void removeChild(final ASTNodeArtifact child) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("[" + getId() + "] removing child " + child.getId());
            LOG.trace("children before removal: " + getChildren());
        }

        Iterator<ASTNodeArtifact> it = children.iterator();
        ASTNodeArtifact elem;
        while (it.hasNext()) {
            elem = it.next();
            if (elem == child) {
                it.remove();
            }
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("children after removal: " + getChildren());
        }
    }

    /**
     * Pretty-prints the AST to source code.
     *
     * @return Pretty-printed AST (source code)
     */
    public final String prettyPrint() {
        assert (astnode != null);
        rebuildAST();
        astnode.flushCaches();
        if (LOG.isDebugEnabled()) {
            System.out.println(dumpTree());
        }
        return astnode.prettyPrint();
    }

    /**
     * Rebuild the encapsulated ASTNode tree top down. This should be only
     * called at the root node
     */
    public final void rebuildAST() {

        if (isConflict()) {
            astnode.isConflict = true;
            astnode.jdimeId = getId();

            if (left != null) {
                left.rebuildAST();
                astnode.left = left.astnode;
            }

            if (right != null) {
                right.rebuildAST();
                astnode.right = right.astnode;
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

        assert (isConflict()
                || getNumChildren() == astnode.getNumChildNoTransform());

    }

    /*
     * (non-Javadoc)
     *
     * @see de.fosd.jdime.common.Artifact#toString()
     */
    @Override
    public final String toString() {
        assert (astnode != null);
        return astnode.dumpString();
    }

    /*
     * (non-Javadoc)
     *
     * @see de.fosd.jdime.common.Artifact#write(java.lang.String)
     */
    @Override
    public final void write(
            final String str) throws IOException {
        // TODO Auto-generated method stub
    }

    @Override
    public final ASTNodeArtifact createConflictDummy(
            final ASTNodeArtifact type,
            final ASTNodeArtifact left,
            final ASTNodeArtifact right)
            throws FileNotFoundException {
        ASTNodeArtifact conflict;

        conflict = new ASTNodeArtifact(type.astnode.fullCopy());
        conflict.setConflict(left, right);

        return conflict;
    }

    /**
     * Returns statistical data of the tree. stats[0]: number of nodes stats[1]:
     * tree depth stats[2]: maximum number of children
     *
     * @return statistics
     */
    public final int[] getStats() {
        // 0: number of nodes, 1: tree depth, 2: max children
        int[] mystats = new int[3];
        mystats[0] = 1;
        mystats[1] = 0;
        mystats[2] = getNumChildren();
        
        for (int i = 0; i < getNumChildren(); i++) {
            int[] childstats = getChild(i).getStats();
            mystats[0] += childstats[0];
            if (childstats[1] + 1 > mystats[1]) {
                mystats[1] = childstats[1] + 1;
            }
            if (childstats[2] > mystats[2]) {
                mystats[2] = childstats[2];
            }
        }

        return mystats;
    }
    
    public final int[] getStats(Revision revision, Level level) {
        // 0: number of nodes, 1: tree depth, 2: max children,
        // 3: number of matching nodes, 4: number of changed nodes, 
        // 5: number of deleted nodes
        // 6: number of top level nodes
        // 7: matching top level nodes
        // 8: changed top level nodes
        // 9: removed top level nodes
        // 10: number of class level nodes
        // 11: matching class level nodes
        // 12: changed class level nodes
        // 13: removed class level nodes
        // 14: number of method level nodes
        // 15: matching method level nodes
        // 16: changed method level nodes
        // 17: removed method level nodes
        int[] mystats = new int[18];
        mystats[0] = 1;
        mystats[1] = 0;
        mystats[2] = getNumChildren();
        mystats[3] = 0; // matching
        mystats[4] = 0; // changed
        mystats[5] = 0; // removed
        mystats[6] = 0; // top nodes
        mystats[7] = 0; // top matches
        mystats[8] = 0; // top changed
        mystats[9] = 0; // top removed
        mystats[10] = 0; //class nodes
        mystats[11] = 0; //class matches
        mystats[12] = 0; //class changes
        mystats[13] = 0; //class removed
        mystats[14] = 0; //method nodes
        mystats[15] = 0; //method matches
        mystats[16] = 0; //method changes
        mystats[17] = 0; //method removed
        
        if (hasMatching(revision)) {
            mystats[3] = 1;
        } else {
            // changed or deleted?
            if (hasMatches()) {
                // was deleted
                mystats[5] = 1;
            } else {
                mystats[4] = 1;
            }    
        }
        
        switch(level) {
            case TOP:
                mystats[6] = mystats[0];
                mystats[7] = mystats[3];
                mystats[8] = mystats[4];
                mystats[9] = mystats[5];
                break;
            case CLASS:
                mystats[10] = mystats[0];
                mystats[11] = mystats[3];
                mystats[12] = mystats[4];
                mystats[13] = mystats[5];
                break;
            case METHOD:
                mystats[14] = mystats[0];
                mystats[15] = mystats[3];
                mystats[16] = mystats[4];
                mystats[17] = mystats[5];
                break;
        }
        
        // find out current level
        if (astnode instanceof ClassDecl) {
            level = Level.CLASS;
        } else if (astnode instanceof MethodDecl || astnode instanceof ConstructorDecl) {
            level = Level.METHOD;
        }
        
        for (int i = 0; i < getNumChildren(); i++) {
            int[] childstats = getChild(i).getStats(revision, level);
            mystats[0] += childstats[0];
            mystats[3] += childstats[3];
            mystats[4] += childstats[4];
            mystats[5] += childstats[5];
            mystats[6] += childstats[6];
            mystats[7] += childstats[7];
            mystats[8] += childstats[8];
            mystats[9] += childstats[9];
            mystats[10] += childstats[10];
            mystats[11] += childstats[11];
            mystats[12] += childstats[12];
            mystats[13] += childstats[13];
            mystats[14] += childstats[14];
            mystats[15] += childstats[15];
            mystats[16] += childstats[16];
            mystats[17] += childstats[17];
            if (childstats[1] + 1 > mystats[1]) {
                mystats[1] = childstats[1] + 1;
            }
            if (childstats[2] > mystats[2]) {
                mystats[2] = childstats[2];
            }
        }
        
        

        return mystats;
    }
}
