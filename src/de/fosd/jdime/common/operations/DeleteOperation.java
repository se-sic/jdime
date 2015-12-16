/*
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
 */
package de.fosd.jdime.common.operations;

import java.io.IOException;
import java.util.logging.Logger;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeScenario;
import de.fosd.jdime.stats.ElementStatistics;
import de.fosd.jdime.stats.MergeScenarioStatistics;
import de.fosd.jdime.stats.Statistics;

/**
 * The operation deletes <code>Artifact</code>s.
 *
 * @author Olaf Lessenich
 *
 * @param <T>
 *            type of artifact
 *
 */
public class DeleteOperation<T extends Artifact<T>> extends Operation<T> {

    private static final Logger LOG = Logger.getLogger(DeleteOperation.class.getCanonicalName());

    /**
     * The <code>Artifact</code> that is deleted by the operation.
     */
    private T artifact;

    /**
     * The output <code>Artifact</code>.
     */
    private T target;

    private MergeScenario<T> mergeScenario;
    private String condition;

    /**
     * Constructs a new <code>DeleteOperation</code> deleting the given <code>artifact</code> from <code>target</code>.
     *
     * @param artifact
     *         the <code>Artifact</code> to be deleted
     * @param target
     *         the <code>Artifact</code> to delete from
     * @param mergeScenario
     *         the current <code>MergeScenario</code>
     * @param condition
     *         the condition under which the node is NOT deleted
     */
    public DeleteOperation(T artifact, T target, MergeScenario<T> mergeScenario, String condition) {
        this.artifact = artifact;
        this.target = target;
        this.mergeScenario = mergeScenario;

        if (condition != null) {
            this.condition = condition;
        }
    }

    @Override
    public void apply(MergeContext context) throws IOException {
        assert (artifact != null);
        assert (artifact.exists()) : "Artifact does not exist: " + artifact;

        LOG.fine(() -> "Applying: " + this);

        if (context.isConditionalMerge(artifact) && condition != null) {
            // we need to insert a choice node
            T choice = target.createChoiceArtifact(condition, artifact);
            assert (choice.isChoice());
            target.addChild(choice);
        } else {
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
            // which then calls deleteChildren() on the created Program.
        }

        if (context.hasStatistics()) {
            Statistics statistics = context.getStatistics();
            MergeScenarioStatistics mScenarioStatistics = statistics.getCurrentFileMergeScenarioStatistics();
            ElementStatistics element = mScenarioStatistics.getTypeStatistics(artifact.getRevision(), artifact.getType());

            element.incrementNumDeleted();
            artifact.deleteOpStatistics(mScenarioStatistics, context);
        }
    }

    @Override
    public String getName() {
        return "DELETE";
    }

    @Override
    public String toString() {
        return getId() + ": " + getName() + " " + artifact;
    }
}
