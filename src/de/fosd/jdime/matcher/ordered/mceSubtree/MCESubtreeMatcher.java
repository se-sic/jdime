package de.fosd.jdime.matcher.ordered.mceSubtree;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.matcher.MatcherInterface;
import de.fosd.jdime.matcher.matching.Matching;
import de.fosd.jdime.matcher.matching.Matchings;
import de.fosd.jdime.matcher.ordered.OrderedMatcher;

/**
 * A <code>OrderedMatcher</code> that uses the <code>BalancedSequence</code> class to match <code>Artifact</code>s.
 * Its {@link #match(MergeContext, Artifact, Artifact, int, int)} method assumes that the given <code>Artifact</code>s
 * may be interpreted as ordered trees whose nodes are labeled via their {@link Artifact#matches(Artifact)} method.
 *
 * @param <T>
 *         the type of the <code>Artifact</code>s
 */
public class MCESubtreeMatcher<T extends Artifact<T>> extends OrderedMatcher<T> {

    private static final String ID = MCESubtreeMatcher.class.getSimpleName();

    /**
     * Constructs a new <code>OrderedMatcher</code>
     *
     * @param matcher
     *         the parent <code>MatcherInterface</code>
     */
    public MCESubtreeMatcher(MatcherInterface<T> matcher) {
        super(matcher);
    }

    @Override
    public Matchings<T> match(MergeContext context, T left, T right, int leftLAH, int rightLAH) {
        BalancedSequence<T> lSeq;
        BalancedSequence<T> rSeq;

        if (leftLAH == MergeContext.LOOKAHEAD_FULL) {
            lSeq = new BalancedSequence<>(left);
        } else {
            lSeq = new BalancedSequence<>(left, leftLAH);
        }

        if (rightLAH == MergeContext.LOOKAHEAD_FULL) {
            rSeq = new BalancedSequence<>(left);
        } else {
            rSeq = new BalancedSequence<>(left, leftLAH);
        }

        Matchings<T> matchings = new Matchings<>();
        Matching<T> matching = new Matching<>(left, right, BalancedSequence.lcs(lSeq, rSeq));

        matching.setAlgorithm(ID);
        matchings.add(matching);

        for (T lChild : left.getChildren()) {
            for (T rChild : right.getChildren()) {
                matchings.addAll(matcher.match(context, lChild, rChild, leftLAH, rightLAH));
            }
        }

        return matchings;
    }
}
