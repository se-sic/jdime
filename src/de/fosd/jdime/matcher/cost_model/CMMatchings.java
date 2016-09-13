package de.fosd.jdime.matcher.cost_model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.Artifacts;

/**
 * A list of <code>CMMatching</code>s that additionally contains the roots of the left and right trees that
 * are being matched.
 *
 * @param <T>
 *         the type of the <code>Artifact</code>s
 */
final class CMMatchings<T extends Artifact<T>> extends ArrayList<CMMatching<T>> {

    T left;
    T right;

    public CMMatchings(int initialCapacity, T left, T right) {
        super(initialCapacity);
        this.left = left;
        this.right = right;
    }

    public CMMatchings(T left, T right) {
        this.left = left;
        this.right = right;
    }

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

    Map<T, T> asMap() {
        return stream().collect(HashMap::new, (map, matching) -> {
            map.put(matching.m, matching.n);
            map.put(matching.n, matching.m);
        }, HashMap::putAll);
    }
}
