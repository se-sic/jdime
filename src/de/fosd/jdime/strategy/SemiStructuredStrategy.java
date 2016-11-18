package de.fosd.jdime.strategy;

import de.fosd.jdime.artifact.file.FileArtifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.operations.MergeOperation;

public class SemiStructuredStrategy extends StructuredStrategy {

    @Override
    public void merge(MergeOperation<FileArtifact> operation, MergeContext context) {
        boolean oldSemiStructured = context.isSemiStructured();

        context.setSemiStructured(true);
        super.merge(operation, context);
        context.setSemiStructured(oldSemiStructured);
    }
}
