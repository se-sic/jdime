package de.fosd.jdime.matcher.ordered.mceSubtree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.Set;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.matcher.Matcher;
import de.fosd.jdime.matcher.Matching;
import de.fosd.jdime.matcher.NewMatching;
import de.fosd.jdime.matcher.ordered.OrderedMatcher;

/**
 * A <code>OrderedMatcher</code> that uses the <code>BalancedSequence</code> class to match <code>Artifact</code>s.
 * Its {@link this#match(MergeContext, Artifact, Artifact, int)} method assumes that the given <code>Artifact</code>s
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
    public Matching<T> match(MergeContext context, T left, T right, int lookAhead) {
        BalancedSequence<T> s;
        BalancedSequence<T> t;

        if (lookAhead == MergeContext.LOOKAHEAD_FULL) {
            s = new BalancedSequence<>(left);
            t = new BalancedSequence<>(right);
        } else {
            s = new BalancedSequence<>(left, lookAhead);
            t = new BalancedSequence<>(right, lookAhead);
        }

        List<BalancedSequence<T>> leftSequences = getSequences(preOrder(s.getRoot()));
        List<BalancedSequence<T>> rightSequences = getSequences(preOrder(t.getRoot()));
        Set<NewMatching<T>> matchings = getMatchings(leftSequences, rightSequences);

        /*
         * Now we filter out the BalancedSequences in rightSequences which were produced by a node that is
         * already in the left tree.
         */
        for (ListIterator<BalancedSequence<T>> it = rightSequences.listIterator(); it.hasNext(); ) {
            BalancedSequence<T> rightSeq = it.next();

            for (BalancedSequence<T> leftSeq : leftSequences) {

                if (rightSeq.getRoot().matches(leftSeq.getRoot())) {
                    it.remove();
                    break;
                }
            }
        }

        matchings.addAll(getMatchings(rightSequences, leftSequences));

        // TODO returning nonsense for now until we can return a Set of NewMatchings as produced by the BalancedSequence
        return new Matching<>(left, right, 1);
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

    /**
     * Transforms the <code>List of T</code> into a list containing the corresponding <code>BalancedSequence</code>s.
     *
     * @param nodes
     *         the nodes to transform
     *
     * @return the <code>BalancedSequence</code>s of the nodes
     */
    private List<BalancedSequence<T>> getSequences(List<T> nodes) {
        List<BalancedSequence<T>> sequences = new ArrayList<>(nodes.size());

        for (T node : nodes) {
            sequences.add(new BalancedSequence<>(node));
        }

        return sequences;
    }
}
