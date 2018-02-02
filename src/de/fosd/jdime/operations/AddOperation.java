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
 * An {@link Operation} that adds a given {@link Artifact} to the children of another {@link Artifact}.
 *
 * @param <T>
 *         the type of the {@link Artifact Artifacts}
 */
public class AddOperation<T extends Artifact<T>> extends Operation<T> {

    private static final Logger LOG = Logger.getLogger(AddOperation.class.getCanonicalName());

    /**
     * The {@link Artifact} that is being added to the children of {@link #target}.
     */
    private T artifact;

    /**
     * The {@link Artifact} to whose children {@link #artifact} is added.
     */
    private T target;

    private String condition;

    /**
     * Constructs a new <code>AddOperation</code> adding the given <code>artifact</code> to <code>target</code>.
     *
     * @param artifact
     *         the <code>Artifact</code> to be added
     * @param target
     *         the <code>Artifact</code> to add to
     * @param condition
     *         the presence condition for <code>artifact</code> or <code>null</code>
     */
    public AddOperation(T artifact, T target, String condition) {
        Objects.requireNonNull(artifact, "The artifact to be added must not be null.");
        Objects.requireNonNull(target, "The target to be added to must not be null.");

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
            target.addChild(artifact);
        }

        if (context.hasStatistics()) {
            Statistics statistics = context.getStatistics();
            MergeScenarioStatistics mScenarioStatistics = statistics.getCurrentFileMergeScenarioStatistics();

            artifact.addOpStatistics(mScenarioStatistics, context);
        }
    }

    @Override
    public String toString() {
        if (condition == null) {
            return String.format("%s: %s TO %s", getId(), artifact.getId(), target.getId());
        } else {
            return String.format("%s: %s TO %s CONDITION %s", getId(), artifact.getId(), target.getId(), condition);
        }
    }
}
