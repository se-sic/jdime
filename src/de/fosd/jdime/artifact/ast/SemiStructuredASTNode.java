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

import java.util.regex.Pattern;

import org.extendj.ast.Block;
import org.extendj.ast.List;
import org.extendj.ast.Stmt;
import org.jastadd.util.PrettyPrinter;

import static java.util.regex.Pattern.MULTILINE;

/**
 * A {@link Block} that refers to the {@link SemiStructuredArtifact#getContent() content} of a
 * {@link SemiStructuredArtifact} for its pretty printing.
 */
public class SemiStructuredASTNode extends Block {

    private static final Pattern LAST_NEWLINE = Pattern.compile("\\R\\z", MULTILINE);

    private SemiStructuredArtifact artifact;

    /**
     * Constructs a new {@link SemiStructuredASTNode} referring to the given {@code artifact} for its pretty printing
     * via {@link SemiStructuredArtifact#getContent()}.
     *
     * @param artifact
     *         the {@link SemiStructuredArtifact} to refer to
     */
    public SemiStructuredASTNode(SemiStructuredArtifact artifact) {
        this.artifact = artifact;
    }

    /**
     * Sets the {@link SemiStructuredArtifact} to refer to the given {@code artifact}.
     *
     * @param artifact
     *         the new {@link SemiStructuredArtifact} to refer to for pretty printing
     */
    void setArtifact(SemiStructuredArtifact artifact) {
        this.artifact = artifact;
    }

    @Override
    public void init$Children() {
        // Block adds an empty List as the first child. A SemiStructuredASTNode does not have children.
    }

    @Override
    protected int numChildren() {
        return 0; // Block returns 1...
    }

    @Override
    public void prettyPrint(PrettyPrinter out) {
        String content = artifact.getContent().getContent();

        out.print("{\n");
        out.print(LAST_NEWLINE.matcher(content).replaceAll(""));
        out.println();
        out.print("}");
    }

    @Override
    public int getNumStmt() {
        return getNumStmtNoTransform();
    }

    @Override
    public int getNumStmtNoTransform() {
        return 1;
    }

    @Override
    public Stmt getStmt(int i) {
        /*
         * This Block can not be stored as a field because the SemiStructuredArtifact containing this
         * SemiStructuredASTNode (the 'artifact' field) is copied. This invalidates the reference used by the anonymous
         * class leading to incorrect content being printed.
         */
        return new Block() {

            @Override
            public void prettyPrint(PrettyPrinter out) {
                String content = artifact.getContent().getContent();
                out.print(LAST_NEWLINE.matcher(content).replaceAll(""));
            }
        };
    }

    @Override
    public List<Stmt> getStmtList() {
        return getStmtsNoTransform();
    }

    @Override
    public List<Stmt> getStmtListNoTransform() {
        return getStmtsNoTransform();
    }

    @Override
    public List<Stmt> getStmts() {
        return getStmtsNoTransform();
    }

    @Override
    public List<Stmt> getStmtsNoTransform() {
        return new List<>(getStmt(42));
    }
}
