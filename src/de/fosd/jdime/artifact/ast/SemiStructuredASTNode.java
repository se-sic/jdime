package de.fosd.jdime.artifact.ast;

import java.util.regex.Pattern;

import org.jastadd.extendj.ast.Block;
import org.jastadd.extendj.ast.List;
import org.jastadd.extendj.ast.Stmt;

import static java.util.regex.Pattern.MULTILINE;

public class SemiStructuredASTNode extends Block {

    private static final Pattern BRACES = Pattern.compile("\\A\\s*\\{\\s*$|\\R^\\s*\\}\\s*\\z", MULTILINE);

    private SemiStructuredArtifact artifact;

    public SemiStructuredASTNode(SemiStructuredArtifact artifact) {
        this.artifact = artifact;
    }

    void setArtifact(SemiStructuredArtifact artifact) {
        this.artifact = artifact;
    }

    @Override
    public void refined_PrettyPrint_Block_prettyPrint(StringBuffer sb) {
        sb.append(artifact.getContent().getContent().trim());
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
            public void refined_PrettyPrint_Block_prettyPrint(StringBuffer sb) {
                sb.append(BRACES.matcher(artifact.getContent().getContent()).replaceAll(""));
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
