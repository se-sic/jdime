package de.fosd.jdime.matcher.ordered.mceSubtree;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.matcher.Matcher;
import de.fosd.jdime.matcher.Matching;
import de.fosd.jdime.matcher.NewMatching;
import de.fosd.jdime.matcher.ordered.OrderedMatcher;

import java.util.Set;

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

        Set<NewMatching<T>> matchings = BalancedSequence.lcs(s, t);

        // TODO returning nonsense for now until we can return a Set of NewMatchings as produced by the BalancedSequence
        return new Matching<>(left, right, 1);
    }
}
