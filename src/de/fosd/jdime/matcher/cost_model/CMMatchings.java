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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.artifact.Artifacts;

/**
 * A list of <code>CMMatching</code>s that additionally contains the roots of the left and right trees that
 * are being matched.
 *
 * @param <T>
 *         the type of the <code>Artifact</code>s
 */
final class CMMatchings<T extends Artifact<T>> extends ArrayList<CMMatching<T>> {

    private static final long serialVersionUID = 1L;

    T left;
    T right;

    /**
     * See {@link super#ArrayList(int)}.
     *
     * @param left
     *         the left root
     * @param right
     *         the right root
     */
    public CMMatchings(int initialCapacity, T left, T right) {
        super(initialCapacity);
        this.left = left;
        this.right = right;
    }

    /**
     * See {@link super#ArrayList()}.
     *
     * @param left
     *         the left root
     * @param right
     *         the right root
     */
    public CMMatchings(T left, T right) {
        this.left = left;
        this.right = right;
    }

    /**
     * See {@link super#ArrayList(Collection)}.
     *
     * @param left
     *         the left root
     * @param right
     *         the right root
     */
    public CMMatchings(Collection<? extends CMMatching<T>> c, T left, T right) {
        super(c);
        this.left = left;
        this.right = right;
    }

    /**
     * Checks whether matchings conform to the format required by
     * {@link CostModelMatcher#cost(CMMatchings, CMParameters)}. That is whether there is exactly
     * one matching for every artifact in the left and right tree matching that artifact to one from the opposite tree
     * (or null). No matchings containing artifacts that do not occur in the left or right tree are tolerated.
     *
     * @return whether this <code>CMMatchings</code> has a valid format
     */
    boolean sane() {
        Set<T> leftTree = new HashSet<>(Artifacts.dfs(left));
        Set<T> rightTree = new HashSet<>(Artifacts.dfs(right));

        if (left.getTreeSize() != leftTree.size() || right.getTreeSize() != rightTree.size()) {
            return false;
        }

        for (CMMatching<T> matching : this) {

            if (matching.m != null && !leftTree.remove(matching.m)) {
                return false;
            }

            if (matching.n != null && !rightTree.remove(matching.n)) {
                return false;
            }
        }

        return leftTree.isEmpty() && rightTree.isEmpty();
    }

    /**
     * Returns this list of matchings as a <code>Map</code>. The map will contain matchings (l => r) and (r => l) for
     * every matching [l, r].
     *
     * @return the matchings as a map
     */
    Map<T, T> asMap() {
        return stream().filter(m -> !m.isNoMatch()).collect(HashMap::new, (map, matching) -> {
            map.put(matching.m, matching.n);
            map.put(matching.n, matching.m);
        }, HashMap::putAll);
    }
}
