package de.fosd.jdime.artifact.ast;

import de.fosd.jdime.artifact.file.FileArtifact;
import org.jastadd.extendj.ast.ASTNode;

public class SemiStructuredASTNode extends ASTNode<SemiStructuredASTNode> {

    private FileArtifact content;

    public SemiStructuredASTNode(FileArtifact content) {
        this.content = content;
    }

    @Override
    public void prettyPrint(StringBuffer sb) {
        sb.append(content.getContent());
    }
}
