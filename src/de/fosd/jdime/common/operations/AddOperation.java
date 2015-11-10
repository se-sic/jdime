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
 * The operation adds <code>Artifact</code>s.
 *
 * @author Olaf Lessenich
 *
 * @param <T>
 *            type of artifact
 *
 */
public class AddOperation<T extends Artifact<T>> extends Operation<T> {

    private static final Logger LOG = Logger.getLogger(AddOperation.class.getCanonicalName());

    /**
     * The <code>Artifact</code> that is added by the operation.
     */
    private T artifact;

    /**
     * The output <code>Artifact</code>.
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

        if (artifact.isChoice()) {
            target.addChild(artifact);
            return;
        }

        if (target != null) {
            assert (target.exists());

            if (context.isConditionalMerge(artifact) && condition != null) {
                T choice = target.createChoiceArtifact(condition, artifact);
                assert (choice.isChoice());
                target.addChild(choice);
            } else {
                LOG.fine("no conditions");
                target.addChild(artifact.clone());
            }
        }

        if (context.hasStatistics()) {
            Statistics statistics = context.getStatistics();
            MergeScenarioStatistics mScenarioStatistics = statistics.getScenarioStatistics(mergeScenario);
            ElementStatistics element = mScenarioStatistics.getTypeStatistics(artifact.getRevision(), artifact.getType());

            element.incrementNumAdded();
            artifact.addOpStatistics(mScenarioStatistics, context);
        }
    }

    @Override
    public String getName() {
        return "ADD";
    }

    /**
     * Returns the target <code>Artifact</code>
     * @return the target
     */
    public T getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return getId() + ": " + getName() + " " + artifact + " (" + condition + ")";
    }
}
