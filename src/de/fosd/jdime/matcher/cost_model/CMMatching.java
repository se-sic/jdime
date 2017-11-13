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
package de.fosd.jdime.matcher.cost_model;

import java.util.Objects;

import de.fosd.jdime.artifact.Artifact;

/**
 * A pair of matching artifacts. This class is used instead of <code>Matching</code> because the
 * <code>CostModelMatcher</code> needs different semantics than the <code>Matching</code> provides. One difference is
 * that either one of the artifacts being matched may be <code>null</code> representing the no-match node.
 * This class also contains fields to store the exact and bounded costs for this matching.
 *
 * @param <T>
 *         the type of the artifacts
 */
final class CMMatching<T extends Artifact<T>> {

    final T m;
    final T n;

    private float exactCost;
    private Bounds costBounds;

    /**
     * Constructs a new <code>CMMatching</code> between <code>m</code> and <code>n</code>.
     *
     * @param m
     *         the left artifact
     * @param n
     *         the right artifact
     */
    CMMatching(T m, T n) {
        this.m = m;
        this.n = n;
    }

    /**
     * Returns whether this <code>CMMatching</code> represents a no-match.
     *
     * @return true iff one of the artifacts being matched is <code>null</code>
     */
    public boolean isNoMatch() {
        return m == null || n == null;
    }

    /**
     * Returns whether one of the artifacts being matched is <code>t</code>.
     *
     * @param t
     *         the artifact to test
     * @return true iff <code>m</code> or <code>n</code> is <code>t</code>
     */
    public boolean contains(T t) {
        return m == t || n == t;
    }

    /**
     * If this <code>CMMatching</code> contains <code>t</code>, this method returns the artifact <code>t</code> is being
     * matched with.
     *
     * @param t
     *         the <code>Artifact</code> to return the matching partner for
     * @return the <code>Artifact</code> <code>t</code> is being matched with
     * @throws IllegalArgumentException
     *         if this <code>CMMatching</code> does not contain <code>t</code>
     */
    public T other(T t) {
        if (m == t) {
            return n;
        } else if (n == t) {
            return m;
        } else {
            throw new IllegalArgumentException(t + " is not part of " + this);
        }
    }

    /**
     * Returns the exact cost of this matching.
     *
     * @return the exact cost
     */
    public float getExactCost() {
        return exactCost;
    }

    /**
     * Sets the exact cost of this matching.
     *
     * @param exactCost
     *         the new exact cost
     */
    public void setExactCost(float exactCost) {
        this.exactCost = exactCost;
    }

    /**
     * Returns the bounded cost for this matching.
     *
     * @return the bounded cost
     */
    public Bounds getCostBounds() {
        return costBounds;
    }

    /**
     * Sets the bounded cost for this matching.
     *
     * @param lower
     *         the new lower bound
     * @param upper
     *         the new upper bound
     */
    public void setBounds(float lower, float upper) {
        if (costBounds == null) {
            setCostBounds(new Bounds(lower, upper));
        } else {
            costBounds.setLower(lower);
            costBounds.setUpper(upper);
        }
    }

    /**
     * Sets the bounded cost for this matching
     *
     * @param costBounds
     *         the new bounded costs
     */
    public void setCostBounds(Bounds costBounds) {
        this.costBounds = costBounds;
    }

    @Override
    public String toString() {
        return String.format("{%s, %s, %f, %s}", m, n, exactCost, costBounds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CMMatching<?> that = (CMMatching<?>) o;
        return Objects.equals(m, that.m) && Objects.equals(n, that.n);
    }

    @Override
    public int hashCode() {
        return Objects.hash(m, n);
    }
}
