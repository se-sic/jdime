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
package de.fosd.jdime.util;

import java.util.Objects;

/**
 * A <code>Tuple</code> whose {@link #equals(Object)} and {@link #hashCode()} methods are implemented for unordered
 * value equality.
 *
 * @param <X>
 *         the type of the first object
 * @param <Y>
 *         the type of the second object
 * @author Georg Seibt
 */
public class UnorderedTuple<X, Y> implements Cloneable {

    private X x;
    private Y y;

    /**
     * Constructs an <code>UnorderedTuple</code> of the two given objects.
     *
     * @param x
     *         the first object
     * @param y
     *         the second object
     * @param <X>
     *         the type of the first object
     * @param <Y>
     *         the type of the second object
     * @return an <code>UnorderedTuple</code> containing <code>x</code> and <code>y</code>
     */
    public static <X, Y> UnorderedTuple<X, Y> of(X x, Y y) {
        return new UnorderedTuple<>(x, y);
    }

    /**
     * Constructs a new <code>UnorderedTuple</code> containing the given objects.
     *
     * @param x
     *         the first object
     * @param y
     *         the second object
     */
    private UnorderedTuple(X x, Y y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns the first object contained in the <code>UnorderedTuple</code>.
     *
     * @return the first object
     */
    public X getX() {
        return x;
    }

    /**
     * Sets the first object contained in the <code>UnorderedTuple</code> to the given value.
     *
     * @param x
     *         the new first object
     */
    public void setX(X x) {
        this.x = x;
    }

    /**
     * Returns the second object contained in the <code>UnorderedTuple</code>.
     *
     * @return the second object
     */
    public Y getY() {
        return y;
    }

    /**
     * Sets the second object contained in the <code>UnorderedTuple</code> to the given value.
     *
     * @param y
     *         the new second object
     */
    public void setY(Y y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UnorderedTuple<?, ?> tuple = (UnorderedTuple<?, ?>) o;

        if (Objects.equals(x, tuple.x) && Objects.equals(y, tuple.y)) {
            return true;
        }

        return Objects.equals(x, tuple.y) && Objects.equals(y, tuple.x);
    }

    @Override
    public int hashCode() {
        int hashX = x == null ? 0 : x.hashCode();
        int hashY = y == null ? 0 : y.hashCode();

        return hashX ^ hashY;
    }

    @Override
    @SuppressWarnings("unchecked")
    public UnorderedTuple<X, Y> clone() {

        try {
            return (UnorderedTuple<X, Y>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
