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

import java.util.logging.Logger;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.common.MergeContext;

/**
 * @author Olaf Lessenich
 *
 * @param <T>
 *            type of artifact
 */
public class ConflictOperation<T extends Artifact<T>> extends Operation<T> {

    private static final Logger LOG = Logger.getLogger(ConflictOperation.class.getCanonicalName());
    
    private T type;
    private T left;
    private T right;

    /**
     * Output Artifact.
     */
    private T target;

    private String leftCondition;
    private String rightCondition;

    /**
     * Class constructor.
     *
     * @param left left alternatives
     * @param right right alternatives
     * @param target target node
     */
    public ConflictOperation(final T left, final T right, final T target, final String leftCondition,
                             final String rightCondition) {
        super();
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

        if (target != null) {
            assert (target.exists());

            if (context.isConditionalMerge(left) && leftCondition != null && rightCondition != null) {
                LOG.fine("Create choice node");
                T choice;
                if (left.isChoice()) {
                    choice = left;
                } else {
                    choice = target.createChoiceArtifact(leftCondition, left);
                }

                assert (choice.isChoice());
                choice.addVariant(rightCondition, right);
                target.addChild(choice);
            } else {
                LOG.fine("Create conflict node");
                T conflict = target.createConflictArtifact(left, right);
                assert (conflict.isConflict());
                target.addChild(conflict);
            }
        }
    }

    @Override
    public final String getName() {
        return "CONFLICT";
    }

    @Override
    public final String toString() {
        return getId() + ": " + getName() + " {" + left + "} <~~> {" + right
                + "}";
    }
}
