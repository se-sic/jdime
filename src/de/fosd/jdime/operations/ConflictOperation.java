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

/**
 * An {@link Operation} that adds a conflict or choice node to the target artifact depending on the
 * {@link MergeContext} configuration.
 *
 * @param <T>
 *         the type of the {@link Artifact Artifacts}
 */
public class ConflictOperation<T extends Artifact<T>> extends Operation<T> {

    private static final Logger LOG = Logger.getLogger(ConflictOperation.class.getCanonicalName());
    
    private T left;
    private T right;

    /**
     * The target {@link Artifact} to add a conflict or choice node to.
     */
    private T target;

    private String leftCondition;
    private String rightCondition;

    /**
     * Constructs a new {@link ConflictOperation} adding a conflict representation between the {@code left} and
     * {@code right} alternative to the {@code target}.
     *
     * @param left
     *         the left alternative
     * @param right
     *         the right alternative
     * @param target
     *         the target {@link Artifact} to add a conflict representation to
     * @param leftCondition
     *         the condition for the left alternative, may be {@code null}
     * @param rightCondition
     *         the condition for the right alternative, may be {@code null}
     */
    public ConflictOperation(T left, T right, T target, String leftCondition, String rightCondition) {
        Objects.requireNonNull(target, "The parent for the conflict must not be null.");

        this.left = left;
        this.right = right;
        this.target = target;

        if (leftCondition != null) {
            this.leftCondition = leftCondition;
        }

        if (rightCondition != null) {
            this.rightCondition = rightCondition;
        }
    }

    @Override
    public void apply(MergeContext context) {
        LOG.fine(() -> "Applying: " + this);

        if (context.isConditionalMerge(left) && leftCondition != null && rightCondition != null) {
            LOG.fine("Creating a choice node.");

            T choice;
            if (left.isChoice()) {
                choice = left;
            } else {
                choice = target.createChoiceArtifact(leftCondition, left);
            }

            choice.addVariant(rightCondition, right);
            target.addChild(choice);
        } else {
            LOG.fine("Creating a conflict node.");
            target.addChild(target.createConflictArtifact(left, right));
        }
    }

    @Override
    public final String toString() {
        String lId = left != null ? left.getId() : "NONE";
        String rId = right != null ? right.getId() : "NONE";
        String tId = target.getId();

        if (leftCondition == null && rightCondition == null) {
            return String.format("%s: BETWEEN %s AND %s UNDER %s", getId(), lId, rId, tId);
        } else {
            String format = "%s: BETWEEN %s (CONDITION %s) AND %s (CONDITION %s) UNDER %s";
            return String.format(format, getId(), lId, leftCondition, rId, rightCondition, tId);
        }
    }
}
