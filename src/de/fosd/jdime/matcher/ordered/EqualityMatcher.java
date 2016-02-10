/**
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2015 University of Passau, Germany
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
package de.fosd.jdime.matcher.ordered;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.matcher.MatcherInterface;
import de.fosd.jdime.matcher.matching.Matching;
import de.fosd.jdime.matcher.matching.Matchings;

/**
 * The <code>EqualityMatcher</code> can be used to compute <code>Matchings</code> for identical trees.
 * It traverses two ordered trees in post order and produces respective <code>Matchings</code>.
 * <p>
 * <code>EqualityMatcher</code> does not use its parent matcher to dispatch match() calls, and uses its own
 * implementation instead.
 * <p>
 * Usage:<br/>
 * To check whether the trees are equal, extract the <code>Matching</code> with the highest score and compare it
 * with the size of the trees.
 *
 * @param <T> type of <code>Artifact</code>
 * @author Olaf Lessenich
 */
public class EqualityMatcher<T extends Artifact<T>> extends OrderedMatcher<T> {

    /**
     * Constructs a new <code>EqualityMatcher</code>.<br/>
     * This matcher does not use the parent matcher to dispatch further calls.
     *
     * @param matcher the parent <code>MatcherInterface</code>
     */
    public EqualityMatcher(MatcherInterface<T> matcher) {
        super(matcher);
    }

    @Override
    public Matchings<T> match(MergeContext context, T left, T right) {
        String id = getClass().getSimpleName();

        Matchings<T> matchings = new Matchings<>();
        int score = 0;
        boolean identicalSubtree = true;

        for (int i = 0; i < Math.min(left.getNumChildren(), right.getNumChildren()); i++) {
            T leftChild = left.getChild(i);
            T rightChild = right.getChild(i);

            Matchings<T> childMatchings = match(context, leftChild, rightChild);
            matchings.addAll(childMatchings);

            if (childMatchings.get(leftChild, rightChild).isPresent()) {
                Matching<T> childMatching = childMatchings.get(leftChild, rightChild).get();

                if (childMatching.hasFullyMatched()) {
                    score += childMatching.getScore();
                } else {
                    identicalSubtree = false;
                }
            }
        }

        if (left.matches(right) && left.getNumChildren() == right.getNumChildren() && identicalSubtree) {
            LOG.finest(() -> {
                String format = "%s - Trees are equal: (%s, %s)";
                return String.format(format, id, left.getId(), right.getId());
            });

            score++;
        } else {
            LOG.finest(() -> {
                String format = "%s - Trees are NOT equal: (%s, %s)";
                return String.format(format, id, left.getId(), right.getId());
            });

        }

        matchings.addAll(Matchings.of(left, right, score));

        for (Matching<T> matching : matchings) {
            matching.setAlgorithm(id);
        }

        return matchings;
    }
}
