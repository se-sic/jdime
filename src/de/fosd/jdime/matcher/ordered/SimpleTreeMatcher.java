/*******************************************************************************
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
 *******************************************************************************/
package de.fosd.jdime.matcher.ordered;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ClassUtils;
import org.apache.log4j.Logger;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
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

	private static final Logger LOG = Logger.getLogger(ClassUtils
			.getShortClassName(Matcher.class));

	/**
	 * @param matcher
	 *            matcher
	 */
	public SimpleTreeMatcher(final Matcher<T> matcher) {
		super(matcher);
	}

	/**
	 * TODO: this really needs documentation. I'll soon take care of that.
	 *
	 * @param context <code>MergeContext</code>
	 * @param left
	 * @param right
	 * @param lookAhead How many levels to keep searching for matches in the
	 * subtree if the currently compared nodes are not equal. If there are no
	 * matches within the specified number of levels, do not look for matches
	 * deeper in the subtree. If this is set to LOOKAHEAD_OFF, the matcher will
	 * stop looking for subtree matches if two nodes do not match. If this is
	 * set to LOOKAHEAD_FULL, the matcher will look at the entire subtree.  The
	 * default ist to do no look-ahead matching.
	 * @return
	 */
	@Override
	public final Matching<T> match(final MergeContext context, final T left, final T right, int lookAhead) {
		String id = "stm";

		int rootMatching = left.matches(right) ? 1 : 0;

		if (rootMatching == 0) {
			if (lookAhead == 0) {
				// roots contain distinct symbols and we cannot use the look-ahead feature
				// therefore, we ignore the rest of the subtrees and return early to save time
				if (LOG.isTraceEnabled()) {
					LOG.trace(id + " - " + "early return while matching " + left.getId()
							+ " and " + right.getId() + " (LookAhead = " + context.getLookAhead() + ")");
				}
				return new Matching<>(left, right, rootMatching);
			} else {
				lookAhead = lookAhead - 1;
			}
		} else if (context.isLookAhead()) {
			lookAhead = context.getLookAhead();
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

				Matching<T> w = matcher.match(context, left.getChild(i - 1), right.getChild(j - 1), lookAhead);
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
        List<Matching<T>> children = new ArrayList<>();

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

		// total matching score for these trees is the score of the matched children + the matching of the root nodes
		Matching<T> matching = new Matching<>(left, right, matrixM[m][n] + rootMatching);
		matching.setChildren(children);
		return matching;
	}
}
