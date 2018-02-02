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
package de.fosd.jdime.matcher.unordered;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.matcher.MatcherInterface;
import de.fosd.jdime.matcher.matching.Matching;
import de.fosd.jdime.matcher.matching.Matchings;

/**
 * TODO: This needs more explanation.
 *
 * @param <T>
 *         type of artifact
 * @author Olaf Lessenich
 */
public class UniqueLabelMatcher<T extends Artifact<T>> extends UnorderedMatcher<T> {

    private static final String ID = UniqueLabelMatcher.class.getSimpleName();

    // We expect that the Artifacts have a unique label, if they do not an exception is to be expected.
    private final Comparator<T> comp = Comparator.comparing(o -> o.getUniqueLabel().get().get());

    /**
     * Constructs a new <code>UniqueLabelMatcher</code> using the given <code>matcher</code> for recursive calls.
     *
     * @param matcher
     *         the parent <code>MatcherInterface</code>
     */
    public UniqueLabelMatcher(MatcherInterface<T> matcher) {
        super(matcher);
    }

    /**
     * {@inheritDoc}
     * <p>
     * TODO: this needs explanation, I'll fix it soon.
     */
    @Override
    public final Matchings<T> match(final MergeContext context, final T left, final T right) {
        int rootMatching = left.matches(right) ? 1 : 0;

        if (left.getNumChildren() == 0 || right.getNumChildren() == 0) {
            Matchings<T> m = Matchings.of(left, right, rootMatching);
            m.get(left, right).get().setAlgorithm(ID);

            return m;
        }

        List<Matchings<T>> childrenMatchings = new ArrayList<>();
        List<T> leftChildren = new ArrayList<>(left.getChildren());
        List<T> rightChildren = new ArrayList<>(right.getChildren());

        leftChildren.sort(comp);
        rightChildren.sort(comp);

        Iterator<T> leftIt = leftChildren.iterator();
        Iterator<T> rightIt = rightChildren.iterator();
        T leftChild = leftIt.next();
        T rightChild = rightIt.next();
        int sum = 0;

        boolean done = false;
        while (!done) {
            int c = comp.compare(leftChild, rightChild);
            if (c < 0) {
                if (leftIt.hasNext()) {
                    leftChild = leftIt.next();
                } else {
                    done = true;
                }
            } else if (c > 0) {
                if (rightIt.hasNext()) {
                    rightChild = rightIt.next();
                } else {
                    done = true;
                }
            } else if (c == 0) {
                Matchings<T> childMatching = matcher.match(context, leftChild, rightChild);
                Matching<T> matching = childMatching.get(leftChild, rightChild).get();

                childrenMatchings.add(childMatching);
                sum += matching.getScore();

                if (leftIt.hasNext() && rightIt.hasNext()) {
                    leftChild = leftIt.next();
                    rightChild = rightIt.next();
                } else {
                    done = true;
                }
            }
        }

        Matchings<T> result = Matchings.of(left, right, sum + rootMatching);
        result.get(left, right).get().setAlgorithm(ID);
        result.addAllMatchings(childrenMatchings);

        return result;
    }
}
