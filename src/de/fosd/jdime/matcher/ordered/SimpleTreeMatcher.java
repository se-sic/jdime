/*******************************************************************************
 * Copyright (C) 2013, 2014 Olaf Lessenich.
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
 *******************************************************************************/
package de.fosd.jdime.matcher.ordered;

import java.util.LinkedList;
import java.util.List;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.matcher.Direction;
import de.fosd.jdime.matcher.Entry;
import de.fosd.jdime.matcher.Matcher;
import de.fosd.jdime.matcher.Matching;

/**
 * This ordered matcher implements a variant of Yang's Simple Tree Matching.
 * TODO: This needs more explanation, I'll fix that soon.
 *
 * @author Olaf Lessenich
 *
 * @param <T>
 *            type of artifacts
 */
public class SimpleTreeMatcher<T extends Artifact<T>> extends OrderedMatcher<T> {

	/**
	 * @param matcher
	 *            matcher
	 */
	public SimpleTreeMatcher(final Matcher<T> matcher) {
		super(matcher);
	}

	@Override
	public final Matching<T> match(final T left, final T right) {
		String id = "stm";

		if (!left.matches(right)) {
			// roots contain distinct symbols
			return new Matching<>(left, right, 0);
		}

		// number of first-level subtrees of t1
		int m = left.getNumChildren();

		// number of first-level subtrees of t2
		int n = right.getNumChildren();

		int[][] matrixM = new int[m + 1][n + 1];

		@SuppressWarnings("unchecked")
		Entry<T>[][] matrixT = new Entry[m + 1][n + 1];

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

				Matching<T> w = matcher.match(left.getChild(i - 1),
						right.getChild(j - 1));
				if (matrixM[i][j - 1] > matrixM[i - 1][j]) {
					if (matrixM[i][j - 1] > matrixM[i - 1][j - 1]
							+ w.getScore()) {
						matrixM[i][j] = matrixM[i][j - 1];
						matrixT[i][j] = new Entry<>(Direction.LEFT, w);
					} else {
						matrixM[i][j] = matrixM[i - 1][j - 1] + w.getScore();
						matrixT[i][j] = new Entry<>(Direction.DIAG, w);
					}
				} else {
					if (matrixM[i - 1][j] > matrixM[i - 1][j - 1]
							+ w.getScore()) {
						matrixM[i][j] = matrixM[i - 1][j];
						matrixT[i][j] = new Entry<>(Direction.TOP, w);
					} else {
						matrixM[i][j] = matrixM[i - 1][j - 1] + w.getScore();
						matrixT[i][j] = new Entry<>(Direction.DIAG, w);
					}
				}
			}
		}

		int i = m;
		int j = n;
		List<Matching<T>> children = new LinkedList<>();

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
					// markMatching("stm",
					// matrixM[i][j]-matrixM[i - 1][j - 1],
					// t1.getChild(i - 1), t2.getChild(j - 1));
					children.add(matrixT[i][j].getMatching());
					matrixT[i][j].getMatching().setAlgorithm(id);
				}
				i--;
				j--;
				break;
			default:
				break;
			}
		}

		Matching<T> matching = new Matching<>(left, right, matrixM[m][n] + 1);
		matching.setChildren(children);
		return matching;
	}
}
