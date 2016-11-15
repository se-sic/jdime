package de.fosd.jdime.matcher;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import de.fosd.jdime.artifact.Artifact;

/**
 * Caches various properties of {@link Artifact} trees used by the {@link Matcher}.
 *
 * @param <T>
 *         the type of the <code>Artifact</code>s
 */
class MatcherCache<T extends Artifact<T>> {

    private Map<Artifact<T>, Boolean> orderedChildren;
    private Map<Artifact<T>, Boolean> uniquelyLabeledChildren;
    private Map<Artifact<T>, Boolean> fullyOrdered;

    /**
     * Constructs a new empty {@link MatcherCache}.
     */
    MatcherCache() {
        this.orderedChildren = new HashMap<>();
        this.uniquelyLabeledChildren = new HashMap<>();
        this.fullyOrdered = new HashMap<>();
    }

    /**
     * Returns whether the given {@code artifact} has only uniquely labeled children.
     *
     * @param artifact
     *         the {@link Artifact} to check for uniquely labeled children
     * @return true iff all children of the given {@code artifact} have a unique label
     * @see Artifact#getUniqueLabel()
     */
    boolean uniquelyLabeledChildren(T artifact) {
        return uniquelyLabeledChildren.computeIfAbsent(artifact, a ->
                a.getChildren().stream().map(T::getUniqueLabel).allMatch(Optional::isPresent));
    }

    /**
     * Returns whether any child of the given {@code artifact} is ordered.
     *
     * @param artifact
     *         the {@link Artifact} to check for ordered children
     * @return true iff any child of the given {@code artifact} is ordered
     * @see Artifact#isOrdered()
     */
    boolean orderedChildren(T artifact) {
        return orderedChildren.computeIfAbsent(artifact, a ->
                a.getChildren().stream().anyMatch(T::isOrdered));
    }

    /**
     * Returns whether the tree rooted in {@code artifact} is fully ordered.
     *
     * @param artifact
     *         the root of the {@link Artifact} tree to check for full ordering
     * @return true iff the tree rooted in {@code artifact} is fully ordered
     * @see Artifact#isOrdered()
     */
    boolean fullyOrdered(T artifact) {
        return fullyOrdered.computeIfAbsent(artifact, a ->
                a.isOrdered() && a.getChildren().stream().allMatch(this::fullyOrdered));
    }
}
