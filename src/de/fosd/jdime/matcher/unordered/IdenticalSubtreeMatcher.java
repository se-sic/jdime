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
package de.fosd.jdime.matcher.unordered;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.artifact.Artifacts;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.matcher.MatcherInterface;
import de.fosd.jdime.matcher.matching.Matching;
import de.fosd.jdime.matcher.matching.Matchings;

/**
 * A {@link MatcherInterface matcher} that constructs {@link Matchings} between subtrees that match according to their
 * {@link Artifact#getTreeHash()} method.
 *
 * @param <T>
 *         the type of the {@link Artifact Artifacts} being matched
 */
public class IdenticalSubtreeMatcher<T extends Artifact<T>> implements MatcherInterface<T> {

    private Map<T, T> matches;

    /**
     * Constructs a new {@link IdenticalSubtreeMatcher}.
     */
    public IdenticalSubtreeMatcher() {
        this.matches = new HashMap<>();
    }

    /**
     * Stores all matchings resulting from unique matching tree hashes in the left and right trees in this
     * {@link IdenticalSubtreeMatcher}. After this method was called, the
     * {@link #match(MergeContext, Artifact, Artifact)} method will return {@link Matchings} between the full trees if
     * {@link #hasMatched(Artifact, Artifact)} returns {@code true}.
     *
     * @param leftRoot
     *         the root of the left tree
     * @param rightRoot
     *         the root of the right tree
     */
    public void matchTrees(T leftRoot, T rightRoot) {
        Map<String, T> leftUnique = uniqueHashes(leftRoot);
        Map<String, T> rightUnique = uniqueHashes(rightRoot);
        matchSubtree(leftUnique.keySet(), leftRoot, rightUnique);
    }

    /**
     * If the tree hash of {@code left} is unique in the left tree, examines the unique hashes of the right tree and
     * tries to find the hash of {@code left}. If found, adds matchings between all nodes in the {@code left} and
     * matching right tree to {@link #matches}. Otherwise all other nodes in the {@code left} tree are examined in DFS
     * order.
     *
     * @param leftUnique
     *         the unique hashes in the left tree
     * @param left
     *         the node from the left tree to be examined
     * @param rightUnique
     *         the unique hashes in the right tree and their corresponding nodes
     */
    private void matchSubtree(Set<String> leftUnique, T left, Map<String, T> rightUnique) {
        String treeHash = left.getTreeHash();

        if (!leftUnique.contains(treeHash) || !rightUnique.containsKey(treeHash)) {
            left.getChildren().forEach(c -> matchSubtree(leftUnique, c, rightUnique));
            return;
        }

        T right = rightUnique.get(treeHash);

        Iterator<T> leftIt = Artifacts.dfsIterable(left).iterator();
        Iterator<T> rightIt = Artifacts.dfsIterable(right).iterator();

        while (leftIt.hasNext() && rightIt.hasNext()) {
            matches.put(leftIt.next(), rightIt.next());
        }
    }

    /**
     * Returns the subtree hashes that are unique in the given tree.
     *
     * @param treeRoot
     *         the root of the tree to examine
     * @return the unique hashes in the given tree and their corresponding nodes
     */
    private Map<String, T> uniqueHashes(T treeRoot) {
        Map<String, T> hashes = new HashMap<>();
        Set<String> notUnique = new HashSet<>();

        Artifacts.dfsStream(treeRoot).forEach(artifact -> {
            String treeHash = artifact.getTreeHash();

            if (hashes.put(treeHash, artifact) != null) {
                notUnique.add(treeHash);
            }
        });

        notUnique.forEach(hashes::remove);
        return hashes;
    }

    /**
     * Returns whether this {@link IdenticalSubtreeMatcher} has matched the {@link Artifact artifacts} {@code left} and
     * {@code right} (and therefore their whole subtrees.
     *
     * @param left
     *         the left {@link Artifact}
     * @param right
     *         the right {@link Artifact}
     * @return true, iff a previous call to {@link #matchTrees(Artifact, Artifact)} resulted in matching {@code left}
     * and {@code right}
     */
    public boolean hasMatched(T left, T right) {
        return matches.get(left) == right;
    }

    @Override
    public Matchings<T> match(MergeContext context, T left, T right) {
        Matchings<T> matchings = new Matchings<>();

        if (hasMatched(left, right)) {
            constructMatchings(matchings, left);
            return matchings;
        }

        return matchings;
    }

    /**
     * Adds {@link Matching matchings} between the subtrees rooted in {@code left} and its match in {@link #matches} to
     * the given {@code matchings}.
     *
     * @param matchings
     *         the {@link Matchings} to add to
     * @param left
     *         the root of the left tree that was matched
     * @return the score of the {@link Matching} added for {@code left}
     */
    private int constructMatchings(Matchings<T> matchings, T left) {
        int score = 1;

        if (left.hasChildren()) {
            score += left.getChildren().stream().mapToInt(c -> constructMatchings(matchings, c)).sum();
        }

        Matching<T> matching = new Matching<>(left, matches.get(left), score);

        matching.setAlgorithm(IdenticalSubtreeMatcher.class.getSimpleName());
        matchings.add(matching);

        return score;
    }
}
