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
package de.fosd.jdime.matcher.ordered.mceSubtree;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.matcher.MatcherInterface;
import de.fosd.jdime.matcher.matching.Matching;
import de.fosd.jdime.matcher.matching.Matchings;
import de.fosd.jdime.matcher.ordered.OrderedMatcher;

/**
 * A <code>OrderedMatcher</code> that uses the <code>BalancedSequence</code> class to match <code>Artifact</code>s.
 * Its {@link MatcherInterface#match(MergeContext, Artifact, Artifact)} method assumes that the given <code>Artifact</code>s
 * may be interpreted as ordered trees whose nodes are labeled via their {@link Artifact#matches(Artifact)} method.
 *
 * @param <T>
 *         the type of the <code>Artifact</code>s
 */
public class MCESubtreeMatcher<T extends Artifact<T>> extends OrderedMatcher<T> {

    private static final String ID = MCESubtreeMatcher.class.getSimpleName();

    private Map<BalancedSequence<T>, Set<BalancedSequence<T>>> decompositionCache;

    /**
     * Constructs a new <code>OrderedMatcher</code>
     *
     * @param matcher
     *         the parent <code>MatcherInterface</code>
     */
    public MCESubtreeMatcher(MatcherInterface<T> matcher) {
        super(matcher);
        this.decompositionCache = new HashMap<>();
    }

    @Override
    public Matchings<T> match(MergeContext context, T left, T right) {
        BalancedSequence<T> lSeq = new BalancedSequence<>(left);
        BalancedSequence<T> rSeq = new BalancedSequence<>(right);

        lSeq.setDecompositionCache(decompositionCache);
        rSeq.setDecompositionCache(decompositionCache);

        Matchings<T> matchings = new Matchings<>();
        Matching<T> matching = new Matching<>(left, right, BalancedSequence.lcs(lSeq, rSeq));

        matching.setAlgorithm(ID);
        matchings.add(matching);

        for (T lChild : left.getChildren()) {
            for (T rChild : right.getChildren()) {
                matchings.addAll(matcher.match(context, lChild, rChild));
            }
        }

        return matchings;
    }
}
