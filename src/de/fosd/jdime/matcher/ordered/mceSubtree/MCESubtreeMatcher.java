package de.fosd.jdime.matcher.ordered.mceSubtree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.matcher.Matcher;
import de.fosd.jdime.matcher.matching.Matching;
import de.fosd.jdime.matcher.matching.Matchings;
import de.fosd.jdime.matcher.ordered.OrderedMatcher;

/**
 * A <code>OrderedMatcher</code> that uses the <code>BalancedSequence</code> class to match <code>Artifact</code>s.
 * Its {@link #match(MergeContext, Artifact, Artifact, int)} method assumes that the given <code>Artifact</code>s
 * may be interpreted as ordered trees whose nodes are labeled via their {@link Artifact#matches(Artifact)} method.
 *
 * @param <T>
 *         the type of the <code>Artifact</code>s
 */
public class MCESubtreeMatcher<T extends Artifact<T>> extends OrderedMatcher<T> {

    private static final ExecutorService EX = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static final String ID = MCESubtreeMatcher.class.getSimpleName();

    /**
     * Constructs a new <code>OrderedMatcher</code>
     *
     * @param matcher
     *         matcher
     */
    public MCESubtreeMatcher(Matcher<T> matcher) {
        super(matcher);
    }

    @Override
    public Matchings<T> match(MergeContext context, T left, T right, int lookAhead) {
        List<BalancedSequence<T>> leftSeqs = getSequences(left, lookAhead, new ArrayList<>());
        List<BalancedSequence<T>> rightSeqs = getSequences(right, lookAhead, new ArrayList<>());
        Set<Matching<T>> matchings = getMatchings(leftSeqs, rightSeqs);

        Matchings<T> result = new Matchings<>();
        result.addAll(matchings);

        return result;
    }

    /**
     * Returns for every element of <code>left</code> a <code>Matching</code> with every element of
     * <code>right</code>.
     *
     * @param left
     *         the <code>BalancedSequence</code>s of the nodes of the left tree
     * @param right
     *         the <code>BalancedSequence</code>s of the nodes of the right tree
     * @return a <code>Set</code> of <code>Matching</code>s
     */
    private Set<Matching<T>> getMatchings(List<BalancedSequence<T>> left, List<BalancedSequence<T>> right) {
        Set<Matching<T>> matchings = new HashSet<>();
        List<Callable<Void>> tasks = new ArrayList<>();

        for (BalancedSequence<T> leftSequence : left) {
            for (BalancedSequence<T> rightSequence : right) {
                Matching<T> matching = new Matching<>(leftSequence.getRoot(), rightSequence.getRoot(), 0);
                matching.setAlgorithm(ID);

                if (!matchings.contains(matching)) {
                    tasks.add(() -> {
                        matching.setScore(BalancedSequence.lcs(leftSequence, rightSequence));
                        return null;
                    });

                    matchings.add(matching);
                }
            }
        }

        try {
            List<Future<Void>> futures = EX.invokeAll(tasks);

            for (Future<Void> future : futures) {

                try {
                    future.get();
                } catch (ExecutionException e) {
                    LOG.log(Level.SEVERE, "LCS calculation threw an Exception.", e);
                }
            }
        } catch (InterruptedException ignored) {}

        return matchings;
    }

    /**
     * Transforms the given tree <code>root</code> into a list of <code>BalancedSequence</code>s. The given
     * <code>lookAhead</code> is decremented for every level the method descends into the tree.
     *
     * @param root
     *         the root of the tree to transform
     * @param lookAhead
     *         the number of levels to look ahead from the root node of the tree
     * @param list
     *         the <code>List</code> to fill with <code>BalancedSequence</code>s
     * @return the given <code>list</code> filled with the <code>BalancedSequence</code>s resulting from the tree
     */
    private List<BalancedSequence<T>> getSequences(T root, int lookAhead, List<BalancedSequence<T>> list) {

        if (lookAhead == MergeContext.LOOKAHEAD_FULL) {
            list.add(new BalancedSequence<>(root));
        } else if (lookAhead >= 0) {
            list.add(new BalancedSequence<>(root, lookAhead));
        }

        if (lookAhead == MergeContext.LOOKAHEAD_FULL) {
            root.getChildren().forEach(c -> getSequences(c, lookAhead, list));
        } else if (lookAhead > 0) {
            root.getChildren().forEach(c -> getSequences(c, lookAhead - 1, list));
        }

        return list;
    }
}
