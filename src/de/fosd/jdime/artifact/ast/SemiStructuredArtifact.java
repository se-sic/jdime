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
package de.fosd.jdime.artifact.ast;

import java.nio.CharBuffer;
import java.util.regex.Pattern;

import beaver.Symbol;
import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.artifact.file.FileArtifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.config.merge.MergeScenario;
import de.fosd.jdime.config.merge.Revision;
import de.fosd.jdime.operations.MergeOperation;
import de.fosd.jdime.strategy.LinebasedStrategy;
import org.extendj.ast.ASTNode;
import org.extendj.ast.Block;
import org.extendj.ast.ConstructorDecl;
import org.extendj.ast.Opt;
import org.extendj.ast.Stmt;

import static de.fosd.jdime.artifact.file.FileArtifact.FileType.FILE;
import static de.fosd.jdime.config.merge.MergeType.THREEWAY;
import static de.fosd.jdime.stats.KeyEnums.Type.METHOD;
import static java.util.regex.Pattern.MULTILINE;

/**
 * An {@link Artifact} that is part of an {@link ASTNodeArtifact} tree. It replaces a subtree of {@link ASTNodeArtifact}
 * by storing their {@link Artifact#prettyPrint()} and merging it line based.
 */
public class SemiStructuredArtifact extends ASTNodeArtifact {

    private static final Pattern BRACES = Pattern.compile("\\A\\s*\\{\\R?|(?:\\R^\\h*)?\\}\\s*\\z", MULTILINE);

    private static final LinebasedStrategy linebased = new LinebasedStrategy();

    /**
     * Thrown if an {@link ASTNodeArtifact} can not be replaced by a {@link SemiStructuredArtifact}.
     */
    public static class NotReplaceableException extends Exception {

        private static final long serialVersionUID = 0;

        /**
         * @see Exception#Exception(String)
         */
        public NotReplaceableException(String message) {
            super(message);
        }
    }

    private FileArtifact content;

    /**
     * <em>This constructor may modify the tree {@code toEncapsulate} is a part of.</em>
     * <p>
     * Constructs an new {@link SemiStructuredArtifact} that that encapsulates the {@link Artifact#prettyPrint()} of
     * the given {@link Artifact} {@code toEncapsulate}. If {@code toEncapsulate} has a parent {@link Artifact}, it is
     * replaced in the children of that parent by this {@link SemiStructuredArtifact}. The encapsulated {@link ASTNode}
     * tree is changed in the same way.
     * <p>
     * Currently this {@link Artifact} can only replace {@link ASTNodeArtifact} containing {@link Block} AST nodes.
     *
     * @param toEncapsulate
     *         the {@link ASTNodeArtifact} to encapsulate
     * @param originalLines
     *         the lines (including line separators) of the original source code file of which the {@code toEncapsulate}
     *         {@link Artifact} represents a part of
     * @throws NotReplaceableException
     *         if the {@link ASTNode} contained in {@code toEncapsulate} is not assignable to
     *         a {@link Block} or its original source code can not be determined; the tree containing
     *         {@code toEncapsulate} is not changed if this {@link Exception} is thrown
     */
    public SemiStructuredArtifact(ASTNodeArtifact toEncapsulate, String[] originalLines) throws NotReplaceableException {
        super(toEncapsulate);

        if (!Block.class.isAssignableFrom(toEncapsulate.astnode.getClass())) {
            throw new NotReplaceableException("Can only replace ASTNodeArtifacts containing 'Block' AST nodes.");
        }

        this.content = new FileArtifact(getRevision(), FILE);
        this.content.setContent(extractOriginalContent(originalLines));

        this.astnode = new SemiStructuredASTNode(this);

        ASTNodeArtifact parent = toEncapsulate.getParent();

        if (parent != null) {
            parent.setChild(this, parent.indexOf(toEncapsulate));
            parent.astnode.setChild(this.astnode, parent.astnode.getIndexOfChild(toEncapsulate.astnode));
        }

        toEncapsulate.enclosingClassOrMethod().ifPresent(enc -> {

            if (enc.getType() != METHOD || !(enc.astnode instanceof ConstructorDecl)) {
                return;
            }

            ConstructorDecl astnode = (ConstructorDecl) enc.astnode;

            if (astnode.hasParsedConstructorInvocation()) {
                Opt<Stmt> optNode = astnode.getParsedConstructorInvocationOptNoTransform();

                optNode.removeChildren();
                enc.findWrappingASTNodeArtifact(optNode).ifPresent(ASTNodeArtifact::clearChildren);
            }
        });
    }

    /**
     * Constructs an empty {@link SemiStructuredArtifact} with the given {@link Revision}.
     *
     * @param revision
     *         the {@link Revision} for this {@link SemiStructuredArtifact}
     * @see ASTNodeArtifact#ASTNodeArtifact(Revision)
     */
    private SemiStructuredArtifact(Revision revision) {
        super(revision);
        this.content = new FileArtifact(revision, FILE);
    }

    /**
     * Copies the given {@link SemiStructuredArtifact}.
     *
     * @param toCopy
     *         the {@link SemiStructuredArtifact} to copy
     * @see ASTNodeArtifact#ASTNodeArtifact(ASTNodeArtifact)
     */
    private SemiStructuredArtifact(SemiStructuredArtifact toCopy) {
        super(toCopy);

        this.content = toCopy.content.copy();
        ((SemiStructuredASTNode) this.astnode).setArtifact(this);
    }

    /**
     * Extracts the original code that produced the sub-AST under the node stored in this {@link SemiStructuredArtifact}
     *
     * @param lines
     *         the lines (including line separators) of the original source code file
     * @return the original code
     * @throws NotReplaceableException
     *         if the original source file region (as determined by {@link ASTNode#getStart()}
     *         and {@link ASTNode#getEnd()}) is invalid (e.g. empty)
     */
    private String extractOriginalContent(String[] lines) throws NotReplaceableException {
        final int startLine;
        final int startCol;

        final int endLine;
        final int endCol;

        {
            int start = getASTNode().getStart();
            int end = getASTNode().getEnd();

            // OF COURSE lines and columns are 1 indexed in ExtendJ
            startLine = Symbol.getLine(start) - 1;
            startCol = Symbol.getColumn(start) - 1;
            endLine = Symbol.getLine(end) - 1;
            endCol = Symbol.getColumn(end) - 1;
        }

        if (endLine < startLine || (endLine == startLine && endCol <= startCol)) {
            String msg = String.format("Invalid source file region. (Line, Column) Start: (%d,%d) End: (%d,%d)",
                                        startLine, startCol, endLine, endCol);
            throw new NotReplaceableException(msg);
        }

        String content;

        if (startLine == endLine) {
            content = lines[startLine].substring(startCol, endCol + 1);
        } else {
            content = multiLineContent(lines, startLine, startCol, endLine, endCol);
        }

        content = BRACES.matcher(content).replaceAll("");

        return content;
    }

    /**
     * Returns the region of the original source code specified by {@code startLine, startCol, endLine, endCol}.
     *
     * @param lines
     *         the lines (including line separators) of the original source code file
     * @param startLine
     *         the first line of the region
     * @param startCol
     *         the first column in the first line of the region
     * @param endLine
     *         the last line of the region
     * @param endCol
     *         the last column of the last line of the region
     * @return the original source code region
     */
    private String multiLineContent(String[] lines, int startLine, int startCol, int endLine, int endCol) {
        StringBuilder builder = new StringBuilder();

        for (int line = startLine; line <= endLine; line++) {
            String theLine = lines[line];

            if (line == startLine) {
                if (startCol > 0) {
                    builder.append(CharBuffer.allocate(startCol).toString().replace('\0', ' '));
                    builder.append(theLine.substring(startCol));
                } else {
                    builder.append(theLine);
                }
            } else if (line == endLine) {
                builder.append(lines[line].substring(0, endCol + 1));
            } else {
                builder.append(lines[line]);
            }
        }

        return builder.toString();
    }

    /**
     * Returns the {@link FileArtifact} whose {@link FileArtifact#getContent()} represents the pretty printed code
     * of the subtree this {@link SemiStructuredArtifact} replaced.
     *
     * @return the {@link FileArtifact} representing the pretty printed subtree this {@link SemiStructuredArtifact}
     *         replaced
     */
    FileArtifact getContent() {
        return content;
    }

    @Override
    public ASTNodeArtifact copy() {
        return new SemiStructuredArtifact(this);
    }

    @Override
    public ASTNodeArtifact createEmptyArtifact(Revision revision) {
        return new SemiStructuredArtifact(revision);
    }

    @Override
    protected String hashId() {
        return content.getContent();
    }

    @Override
    public void merge(MergeOperation<ASTNodeArtifact> operation, MergeContext context) {
        SemiStructuredArtifact left, base, right, target;

        {
            MergeScenario<ASTNodeArtifact> scenario = operation.getMergeScenario();
            ASTNodeArtifact l = scenario.getLeft();
            ASTNodeArtifact b = scenario.getBase();
            ASTNodeArtifact r = scenario.getRight();
            ASTNodeArtifact t = operation.getTarget();

            boolean validTypes;

            validTypes = l instanceof SemiStructuredArtifact;
            validTypes &= b instanceof SemiStructuredArtifact;
            validTypes &= r instanceof SemiStructuredArtifact;
            validTypes &= t instanceof SemiStructuredArtifact;

            if (validTypes) {
                left = (SemiStructuredArtifact) l;
                base = (SemiStructuredArtifact) b;
                right = (SemiStructuredArtifact) r;
                target = (SemiStructuredArtifact) t;
            } else {
                throw new RuntimeException("All Artifacts involved in a semistructured merge must be SemiStructuredArtifacts.");
            }
        }

        MergeScenario<FileArtifact> fileMergeScenario = new MergeScenario<>(THREEWAY, left.content, base.content, right.content);
        MergeOperation<FileArtifact> fileMerge = new MergeOperation<>(fileMergeScenario, target.content);
        linebased.merge(fileMerge, context);
    }
}
