/*
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
 */
package de.fosd.jdime.matcher.unordered;

import java.util.ArrayList;
import java.util.List;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.Tuple;
import de.fosd.jdime.matcher.Matcher;
import de.fosd.jdime.matcher.Matching;
import de.fosd.jdime.matcher.Matchings;
import blogspot.software_and_algorithms.stern_library.optimization.HungarianAlgorithm;

/**
 * This unordered matcher uses the hungarian algorithm to solve the assignment
 * problem.
 *
 * The implementation of the hungarian algorithm has been taken from
 * https://github.com/KevinStern/software-and-algorithms
 * which uses the MIT license. It implements the O(n^3) version of the
 * hungarian algorithm.
 *
 * @param <T>
 *         type of artifact
 * @author Olaf Lessenich
 */
public class HungarianMatcher<T extends Artifact<T>> extends AssignmentProblemMatcher<T> {

    private String id = getClass().getSimpleName();

    /**
     * Constructs a new <code>HungarianMatcher</code> using the given <code>Matcher</code> for recursive calls.
     *
     * @param matcher
     *         the parent <code>Matcher</code>
     */
    public HungarianMatcher(final Matcher<T> matcher) {
        super(matcher);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected Matchings<T> solveAssignmentProblem(T left, T right, Tuple<Integer, Matchings<T>>[][] childrenMatching, int rootMatching) {
        int m = childrenMatching.length;
        int n = childrenMatching[0].length;
        int[][] matrix = new int[m][n];

        /* We want to solve the assignment problem for maximum values,
         * therefore we have to adjust the matrix by subtracting each value
         * from the maximum value. */
        int max = 0;

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = childrenMatching[i][j].x;
                if (matrix[i][j] > max)
                    max = matrix[i][j];
            }
        }

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                matrix[i][j] = max - matrix[i][j];
            }
        }

        /* Solve via hungarian algorithm. */
        HungarianAlgorithm alg = new HungarianAlgorithm(matrix);
        int[] bestMatches = alg.execute();

        /* Build a list containing the relevant matches. */
        List<Matchings<T>> children = new ArrayList<>();
        int score = 0;

        for (int i = 0; i < bestMatches.length; i++) {
            int j = bestMatches[i];

            if (j < 0)
                continue;

            Tuple<Integer, Matchings<T>> curMatching = childrenMatching[i][j];

            if (curMatching.x > 0) {
                children.add(curMatching.y);
                score += curMatching.x;
            }
        }

        Matching<T> matching = new Matching<>(left, right, score + rootMatching);
        matching.setAlgorithm(id);

        Matchings<T> result = new Matchings<>();
        result.add(matching);
        result.addAllMatchings(children);

        return result;
    }
}
