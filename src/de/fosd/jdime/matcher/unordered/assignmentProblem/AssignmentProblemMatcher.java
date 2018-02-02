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
package de.fosd.jdime.matcher.unordered.assignmentProblem;

import java.lang.reflect.Array;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.matcher.MatcherInterface;
import de.fosd.jdime.matcher.matching.Matching;
import de.fosd.jdime.matcher.matching.Matchings;
import de.fosd.jdime.matcher.unordered.UnorderedMatcher;
import de.fosd.jdime.util.Tuple;

/**
 * <code>UnorderedMatcher</code> that solves the assignment problem, which
 * consists of finding a maximum weight matching in a weighted bipartite graph.
 *
 * @param <T>
 *         type of artifact
 * @author Olaf Lessenich
 */
public abstract class AssignmentProblemMatcher<T extends Artifact<T>> extends UnorderedMatcher<T> {

    private static final String ID = AssignmentProblemMatcher.class.getSimpleName();

    /**
     * Constructs a new <code>AssignmentProblemMatcher</code> using the given <code>matcher</code> for recursive calls.
     *
     * @param matcher
     *         the parent <code>MatcherInterface</code>
     */
    public AssignmentProblemMatcher(MatcherInterface<T> matcher) {
        super(matcher);
    }

    /**
     * {@inheritDoc}
     * <p>
     * TODO: this really needs documentation. I'll soon take care of that.
     */
    @Override
    public final Matchings<T> match(final MergeContext context, final T left, final T right) {
        int rootMatching = left.matches(right) ? 1 : 0;

        // number of first-level subtrees of t1
        int m = left.getNumChildren();

        // number of first-level subtrees of t2
        int n = right.getNumChildren();

        if (m == 0 || n == 0) {
            Matchings<T> matchings = Matchings.of(left, right, rootMatching);
            matchings.get(left, right).get().setAlgorithm(ID);

            return matchings;
        }

        @SuppressWarnings("unchecked")
        Tuple<Integer, Matchings<T>>[][] matchings = (Tuple<Integer, Matchings<T>>[][]) Array.newInstance(Tuple.class, m, n);

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                matchings[i][j] = Tuple.of(0, new Matchings<>());
            }
        }

        T childT1;
        T childT2;

        for (int i = 0; i < m; i++) {
            childT1 = left.getChild(i);
            for (int j = 0; j < n; j++) {
                childT2 = right.getChild(j);
                Matchings<T> w = matcher.match(context, childT1, childT2);
                Matching<T> matching = w.get(childT1, childT2).get();
                matchings[i][j] = Tuple.of(matching.getScore(), w);
            }
        }

        return solveAssignmentProblem(left, right, matchings, rootMatching);
    }

    /**
     * Solves the assignment problem, which consists of finding a maximum
     * weight matching in a weighted bipartite graph.
     *
     * @param left
     *            left artifact
     * @param right
     *            right artifact
     * @param childrenMatching
     *            matrix of matchings
     * @return matching of root nodes
     */
    protected abstract Matchings<T> solveAssignmentProblem(T left, T right, Tuple<Integer, Matchings<T>>[][] childrenMatching, int rootMatching);

}
