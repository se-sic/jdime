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
package de.fosd.jdime.operations;

import java.util.Objects;
import java.util.logging.Logger;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.stats.MergeScenarioStatistics;
import de.fosd.jdime.stats.Statistics;

/**
 * An {@link Operation} that deletes an {@link Artifact} from the children of another {@link Artifact}.
 *
 * @param <T>
 *         the type of the {@link Artifact Artifacts}
 */
public class DeleteOperation<T extends Artifact<T>> extends Operation<T> {

    private static final Logger LOG = Logger.getLogger(DeleteOperation.class.getCanonicalName());

    /**
     * The {@link Artifact} that is deleted by this {@link Operation}.
     */
    private T artifact;

    /**
     * The {@link Artifact} from whose children to delete {@link #artifact} from.
     */
    private T target;

    /**
     * The condition under which the {@link #artifact} is <b>NOT</b> deleted from {@link #target}.
     */
    private String condition;

    /**
     * Constructs a new <code>DeleteOperation</code> deleting the given <code>artifact</code> from the children of
     * <code>target</code>.
     *
     * @param artifact
     *         the <code>Artifact</code> to be deleted
     * @param target
     *         the <code>Artifact</code> to delete from
     * @param condition
     *         the condition under which the node is NOT deleted
     */
    public DeleteOperation(T artifact, T target, String condition) {
        Objects.requireNonNull(artifact, "The artifact to be deleted must not be null.");
        Objects.requireNonNull(target, "The target to delete from must not be null.");

        this.artifact = artifact;
        this.target = target;

        if (condition != null) {
            this.condition = condition;
        }
    }

    @Override
    public void apply(MergeContext context) {
        LOG.fine(() -> "Applying: " + this);

        if (context.isConditionalMerge(artifact) && condition != null) {
            LOG.fine("Creating a choice node.");
            target.addChild(target.createChoiceArtifact(condition, artifact));
        } else {
            // TODO delete anyway.

            // Nothing to do :-)
            //
            // Why?
            // While merging, the target node is created with no children.
            // Therefore if a deletion of an element is applied during the merge,
            // nothing has to be done.
            //
            // For ASTNodeArtifacts, the important method we rely on here is
            // StructuredStrategy.merge(), which calls
            // ASTNodeArtifact.createProgram(ASTNodeArtifact artifact),
            // which then calls clearChildren() on the created Program.
        }

        if (context.hasStatistics()) {
            Statistics statistics = context.getStatistics();
            MergeScenarioStatistics mScenarioStatistics = statistics.getCurrentFileMergeScenarioStatistics();

            artifact.deleteOpStatistics(mScenarioStatistics, context);
        }
    }

    @Override
    public String toString() {
        if (condition == null) {
            return String.format("%s: %s FROM %s", getId(), artifact.getId(), target.getId());
        } else {
            return String.format("%s: %s FROM %s CONDITION %s", getId(), artifact.getId(), target.getId(), condition);
        }
    }
}
