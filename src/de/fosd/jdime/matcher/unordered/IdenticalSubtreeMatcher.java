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

    public IdenticalSubtreeMatcher() {
        this.matches = new HashMap<>();
    }

    public void matchTrees(T leftRoot, T rightRoot) {
        Map<String, T> leftUnique = uniqueHashes(leftRoot);
        Map<String, T> rightUnique = uniqueHashes(rightRoot);
        matchSubtree(leftUnique.keySet(), leftRoot, rightUnique);
    }

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

    @Override
    public Matchings<T> match(MergeContext context, T left, T right) {

        if (matches.get(left) == right) {
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
