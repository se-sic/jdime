/**
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
 *     Georg Seibt <seibt@fim.uni-passau.de>
 */
package de.fosd.jdime.operations;

import java.util.Objects;
import java.util.logging.Logger;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.config.merge.MergeScenario;
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

    private MergeScenario<T> mergeScenario;
    private String condition;

    /**
     * Constructs a new <code>AddOperation</code> adding the given <code>artifact</code> to <code>target</code>.
     *
     * @param artifact
     *         the <code>Artifact</code> to be added
     * @param target
     *         the <code>Artifact</code> to add to
     * @param mergeScenario
     *         the current <code>MergeScenario</code>
     * @param condition
     *         the presence condition for <code>artifact</code> or <code>null</code>
     */
    public AddOperation(T artifact, T target, MergeScenario<T> mergeScenario, String condition) {
        Objects.requireNonNull(artifact, "The artifact to be added must not be null.");
        Objects.requireNonNull(target, "The target to be added to must not be null.");

        this.artifact = artifact;
        this.target = target;
        this.mergeScenario = mergeScenario;

        if (condition != null) {
            this.condition = condition;
        }
    }

    @Override
    public void apply(MergeContext context) {
        assert (artifact != null);
        assert (artifact.exists()) : "Artifact does not exist: " + artifact;

        LOG.fine(() -> "Applying: " + this);

        if (artifact.isChoice()) {
            target.addChild(artifact);
            return;
        }

        if (context.isConditionalMerge(artifact) && condition != null) {
            T choice = target.createChoiceArtifact(condition, artifact);
            assert (choice.isChoice());
            target.addChild(choice);
        } else {
            LOG.fine("no conditions");
            target.addChild(artifact.clone());
        }

        if (context.hasStatistics()) {
            Statistics statistics = context.getStatistics();
            MergeScenarioStatistics mScenarioStatistics = statistics.getCurrentFileMergeScenarioStatistics();

            artifact.addOpStatistics(mScenarioStatistics, context);
        }
    }

    @Override
    public String getName() {
        return "ADD";
    }

    @Override
    public String toString() {
        return getId() + ": " + getName() + " " + artifact + " (" + condition + ")";
    }
}
