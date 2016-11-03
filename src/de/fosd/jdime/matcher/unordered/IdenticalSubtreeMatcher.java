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
     * {@link #match(MergeContext, Artifact, Artifact)} will return {@link Matchings} between the full trees if
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

    public boolean hasMatched(T left, T right) {
        return matches.get(left) == right;
    }

    @Override
    public Matchings<T> match(MergeContext context, T left, T right) {

        if (hasMatched(left, right)) {
            return constructMatchings(new Matchings<>(), left, left.getTreeSize());
        }

        return new Matchings<>();
    }

    private Matchings<T> constructMatchings(Matchings<T> matchings, T left, int treeSize) {
        Matching<T> matching = new Matching<>(left, matches.get(left), treeSize);
        matching.setAlgorithm(IdenticalSubtreeMatcher.class.getSimpleName());

        matchings.add(matching);
        left.getChildren().forEach(c -> constructMatchings(matchings, c, treeSize - 1));

        return matchings;
    }
}
