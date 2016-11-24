package de.fosd.jdime.artifact.ast;

import java.io.IOException;

import de.fosd.jdime.artifact.file.FileArtifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.config.merge.MergeScenario;
import de.fosd.jdime.config.merge.Revision;
import de.fosd.jdime.operations.MergeOperation;
import de.fosd.jdime.strategy.LinebasedStrategy;

import static de.fosd.jdime.artifact.file.FileArtifact.FileType.VFILE;
import static de.fosd.jdime.config.merge.MergeType.THREEWAY;

public class SemiStructuredArtifact extends ASTNodeArtifact {

    private FileArtifact content;

    public SemiStructuredArtifact(ASTNodeArtifact toEncapsulate) {
        super(toEncapsulate);

        this.content = new FileArtifact(getRevision(), VFILE);
        this.content.setContent(toEncapsulate.prettyPrint());

        ASTNodeArtifact parent = toEncapsulate.getParent();

        if (parent != null) {
            parent.setChild(this, parent.indexOf(toEncapsulate));
            parent.getASTNode().setChild(getASTNode(), parent.getASTNode().getIndexOfChild(toEncapsulate.getASTNode()));
        }
    }

    private SemiStructuredArtifact(Revision revision) {
        super(revision);
        this.content = new FileArtifact(revision, VFILE);
    }

    private SemiStructuredArtifact(SemiStructuredArtifact toCopy) {
        super(toCopy);
        this.content = toCopy.content.copy();
    }

    @Override
    public String prettyPrint() {
        return content.prettyPrint();
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

        try { // TODO only write once (caching?)
            left.content.writeContent();
            base.content.writeContent();
            right.content.writeContent();
        } catch (IOException e) {
            throw new RuntimeException("Could not write the SemiStructuredArtifacts to disk.", e);
        }

        MergeScenario<FileArtifact> fileMergeScenario = new MergeScenario<>(THREEWAY, left.content, base.content, right.content);
        MergeOperation<FileArtifact> fileMerge = new MergeOperation<>(fileMergeScenario, target.content);
        new LinebasedStrategy().merge(fileMerge, context);

        target.setASTNode(new SemiStructuredASTNode(target.content));
    }
}
