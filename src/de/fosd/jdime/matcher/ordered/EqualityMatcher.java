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

import java.util.Iterator;
import java.util.stream.Collectors;

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
    
    private static final String ID = EqualityMatcher.class.getSimpleName();
    
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
        Matchings<T> matchings = new Matchings<>();

        Iterator<T> lIt = left.getChildren().iterator();
        Iterator<T> rIt = right.getChildren().iterator();

        boolean allMatched = true;

        while (lIt.hasNext() && rIt.hasNext()) {
            T l = lIt.next();
            T r = rIt.next();
            Matchings<T> childMatchings = match(context, l, r);

            matchings.addAll(childMatchings);
            allMatched &= childMatchings.get(l, r).isPresent();
        }

        if (allMatched && left.getNumChildren() == right.getNumChildren() && left.matches(right)) {
            Integer sumScore = matchings.stream().map(Matching::getScore).collect(Collectors.summingInt(i -> i));

            LOG.finer(() -> {
                String format = "%s - Trees are equal: (%s, %s)";
                return String.format(format, ID, left.getId(), right.getId());
            });

            matchings.add(new Matching<>(left, right, sumScore + 1));
        } else {

            LOG.finer(() -> {
                String format = "%s - Trees are NOT equal: (%s, %s)";
                return String.format(format, ID, left.getId(), right.getId());
            });
        }

        matchings.stream().forEach(m -> m.setAlgorithm(ID));

        return matchings;
    }
}
