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
package de.fosd.jdime.matcher.ordered.simpleTree;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.matcher.MatcherInterface;
import de.fosd.jdime.matcher.matching.Matching;
import de.fosd.jdime.matcher.matching.Matchings;
import de.fosd.jdime.matcher.ordered.OrderedMatcher;

/**
 * This ordered matcher implements a variant of Yang's Simple Tree Matching.
 * TODO: This needs more explanation, I'll fix that soon.
 *
 * @param <T>
 *         type of artifacts
 * @author Olaf Lessenich
 */
public class SimpleTreeMatcher<T extends Artifact<T>> extends OrderedMatcher<T> {

    private static final String ID = SimpleTreeMatcher.class.getSimpleName();

    /**
     * Constructs a new <code>SimpleTreeMatcher</code> using the given <code>matcher</code> for recursive calls.
     *
     * @param matcher
     *         the parent <code>MatcherInterface</code>
     */
    public SimpleTreeMatcher(MatcherInterface<T> matcher) {
        super(matcher);
    }

    /**
     * {@inheritDoc}
     * <p>
     * TODO: this really needs documentation. I'll soon take care of that.
     */
    @Override
    public Matchings<T> match(MergeContext context, T left, T right) {
        int rootMatching = left.matches(right) ? 1 : 0;

        // number of first-level subtrees of t1
        int m = left.getNumChildren();

        // number of first-level subtrees of t2
        int n = right.getNumChildren();

        int[][] matrixM = new int[m + 1][n + 1];

        @SuppressWarnings("unchecked")
        Entry<T>[][] matrixT = (Entry<T>[][]) Array.newInstance(Entry.class, m + 1, n + 1);

        // initialize first column matrix
        for (int i = 0; i <= m; i++) {
            matrixM[i][0] = 0;
        }

        // initialize first row of matrix
        for (int j = 0; j <= n; j++) {
            matrixM[0][j] = 0;
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                T leftChild = left.getChild(i - 1);
                T rightChild = right.getChild(j - 1);

                Matchings<T> w = matcher.match(context, leftChild, rightChild);
                Matching<T> matching = w.get(leftChild, rightChild).get();

                if (matrixM[i][j - 1] > matrixM[i - 1][j]) {
                    if (matrixM[i][j - 1] > matrixM[i - 1][j - 1] + matching.getScore()) {
                        matrixM[i][j] = matrixM[i][j - 1];
                        matrixT[i][j] = new Entry<>(Direction.LEFT, w);
                    } else {
                        matrixM[i][j] = matrixM[i - 1][j - 1] + matching.getScore();
                        matrixT[i][j] = new Entry<>(Direction.DIAG, w);
                    }
                } else {
                    if (matrixM[i - 1][j] > matrixM[i - 1][j - 1] + matching.getScore()) {
                        matrixM[i][j] = matrixM[i - 1][j];
                        matrixT[i][j] = new Entry<>(Direction.TOP, w);
                    } else {
                        matrixM[i][j] = matrixM[i - 1][j - 1] + matching.getScore();
                        matrixT[i][j] = new Entry<>(Direction.DIAG, w);
                    }
                }
            }
        }

        int i = m;
        int j = n;
        List<Matchings<T>> children = new ArrayList<>();

        while (i >= 1 && j >= 1) {
            switch (matrixT[i][j].getDirection()) {
                case TOP:
                    i--;
                    break;
                case LEFT:
                    j--;
                    break;
                case DIAG:
                    if (matrixM[i][j] > matrixM[i - 1][j - 1]) {
                        children.add(matrixT[i][j].getMatching());
                    }
                    i--;
                    j--;
                    break;
                default:
                    break;
            }
        }

        // total matching score for these trees is the score of the matched children + the matching of the root nodes
        Matching<T> matching = new Matching<>(left, right, matrixM[m][n] + rootMatching);
        matching.setAlgorithm(ID);

        Matchings<T> matchings = new Matchings<>();
        matchings.add(matching);
        matchings.addAllMatchings(children);

        return matchings;
    }
}
