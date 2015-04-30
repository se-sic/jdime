package de.fosd.jdime.matcher.ordered.mceSubtree;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.matcher.Matcher;
import de.fosd.jdime.matcher.Matchings;
import de.fosd.jdime.matcher.NewMatching;
import de.fosd.jdime.matcher.ordered.OrderedMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A <code>OrderedMatcher</code> that uses the <code>BalancedSequence</code> class to match <code>Artifact</code>s.
 * Its {@link #match(MergeContext, Artifact, Artifact, int)} method assumes that the given <code>Artifact</code>s
 * may be interpreted as ordered trees whose nodes are labeled via their {@link Artifact#matches(Artifact)} method.
 *
 * @param <T>
 *         the type of the <code>Artifact</code>s
 */
public class MCESubtreeMatcher<T extends Artifact<T>> extends OrderedMatcher<T> {

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
        BalancedSequence<T> s;
        BalancedSequence<T> t;

        if (lookAhead == MergeContext.LOOKAHEAD_FULL) {
            s = new BalancedSequence<>(left);
            t = new BalancedSequence<>(right);
        } else {
            s = new BalancedSequence<>(left, lookAhead);
            t = new BalancedSequence<>(right, lookAhead);
        }

        List<BalancedSequence<T>> leftSeqs = preOrder(s.getRoot()).stream().map(BalancedSequence<T>::new).collect(Collectors.toList());
        List<BalancedSequence<T>> rightSeqs = preOrder(t.getRoot()).stream().map(BalancedSequence<T>::new).collect(Collectors.toList());
        Set<NewMatching<T>> matchings = getMatchings(leftSeqs, rightSeqs);

        /*
         * Now we filter out the BalancedSequences in rightSequences which were produced by a node that is
         * already in the left tree.
         */
        rightSeqs.removeIf(rightSeq -> leftSeqs.stream().anyMatch(leftSeq -> rightSeq.getRoot().matches(leftSeq.getRoot())));

        matchings.addAll(getMatchings(rightSeqs, leftSeqs));

        Matchings<T> result = new Matchings<>();
        result.addAll(matchings);

        return result;
    }

    /**
     * Returns for every element of <code>left</code> a <code>NewMatching</code> with the element of
     * <code>right</code> for which the <code>results</code> array contains the highest score.
     *
     * @param left
     *         the <code>BalancedSequence</code>s of the nodes of the left tree
     * @param right
     *         the <code>BalancedSequence</code>s of the nodes of the right tree   @return a <code>Set</code> of
     *         <code>NewMatching</code>s of the described format
     */
    private Set<NewMatching<T>> getMatchings(List<BalancedSequence<T>> left, List<BalancedSequence<T>> right) {
        Set<NewMatching<T>> matchings = new HashSet<>();
        NewMatching<T> matching = null;

        for (BalancedSequence<T> leftSequence : left) {
            for (BalancedSequence<T> rightSequence : right) {
                Integer res = BalancedSequence.lcs(leftSequence, rightSequence);
                Integer score = leftSequence.getRoot().matches(rightSequence.getRoot()) ? res : res + 1;

                if (matching == null || matching.getScore() < score) {
                    matching = new NewMatching<>(leftSequence.getRoot(), rightSequence.getRoot(), score);
                }
            }

            matchings.add(matching);
            matching = null;
        }

        return matchings;
    }

    /**
     * Returns the tree with root <code>root</code> in pre-order.
     *
     * @param root
     *         the root of the tree
     *
     * @return the tree in pre-order
     */
    private List<T> preOrder(T root) {
        List<T> nodes = new ArrayList<>();
        Queue<T> waitQ = new LinkedList<>(Collections.singleton(root));
        T node;

        while (!waitQ.isEmpty()) {
            node = waitQ.poll();
            nodes.add(node);
            waitQ.addAll(node.getChildren());
        }

        return nodes;
    }
}
