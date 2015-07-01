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
package de.fosd.jdime.matcher;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import de.fosd.jdime.Main;
import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.matcher.ordered.OrderedMatcher;
import de.fosd.jdime.matcher.ordered.SimpleTreeMatcher;
import de.fosd.jdime.matcher.ordered.mceSubtree.MCESubtreeMatcher;
import de.fosd.jdime.matcher.unordered.LPMatcher;
import de.fosd.jdime.matcher.unordered.UniqueLabelMatcher;
import de.fosd.jdime.matcher.unordered.UnorderedMatcher;
import org.apache.commons.lang3.ClassUtils;
import org.apache.log4j.Logger;

/**
 * A <code>Matcher</code> is used to compare two <code>Artifacts</code> and to
 * compute and store <code>Matching</code>s.
 * <p>
 * The computation of <code>Matching</code>s is done recursively. Depending on
 * the <code>Artifact</code>, the matcher decides whether the order of elements
 * is important (e.g., statements within a method in a Java AST) or not (e.g.,
 * method declarations in a Java AST) for syntactic correctness. Then either an
 * implementation of <code>OrderedMatcher</code> or
 * <code>UnorderedMatcher</code> is called to compute the actual <code>Matching</code>.
 * Usually, those subclass implementations use this <code>Matcher</code>
 * superclass for the recursive call of the match() method.
 * <p>
 * When the computation is done and the best combination of matches have been
 * selected, they are stored recursively within the <code>Artifact</code> nodes
 * themselves, assigning each matched <code>Artifact</code> a pointer to the
 * corresponding matching <code>Artifact</code>.
 *
 * @author Olaf Lessenich
 *
 * @param <T> type of <code>Artifact</code>
 */
public class Matcher<T extends Artifact<T>> implements MatchingInterface<T> {

	private static final Logger LOG = Logger.getLogger(ClassUtils.getShortClassName(Matcher.class));
	private static final String USE_MCESUBTREE_MATCHER = "USE_MCESUBTREE_MATCHER";

	private int calls = 0;
	private int orderedCalls = 0;
	private int unorderedCalls = 0;

	private UnorderedMatcher<T> unorderedMatcher;
	private UnorderedMatcher<T> unorderedLabelMatcher;
	private OrderedMatcher<T> orderedMatcher;
    private OrderedMatcher<T> mceSubtreeMatcher;

	/**
	 * Constructs a new <code>Matcher</code>.
	 */
	public Matcher() {
		unorderedMatcher = new LPMatcher<>(this);
		unorderedLabelMatcher = new UniqueLabelMatcher<>(this);
		orderedMatcher = new SimpleTreeMatcher<>(this);
        mceSubtreeMatcher = new MCESubtreeMatcher<>(this);
	}

	@Override
	public Matchings<T> match(MergeContext context, T left, T right, int lookAhead) {
		boolean fullyOrdered = Main.config.getBoolean(USE_MCESUBTREE_MATCHER).orElse(false);
        boolean isOrdered = false;
		boolean uniqueLabels = true;

        Queue<T> wait = new LinkedList<>(Arrays.asList(left, right));
        while (fullyOrdered && !wait.isEmpty()) {
            T node = wait.poll();
            fullyOrdered = node.isOrdered();

            for (T t : node.getChildren()) {
                wait.offer(t);
            }
        }
        
		for (int i = 0; !isOrdered && i < left.getNumChildren(); i++) {
			T leftChild = left.getChild(i);
			if (leftChild.isOrdered()) {
				isOrdered = true;
			}
			if (!uniqueLabels || !leftChild.hasUniqueLabels()) {
				uniqueLabels = false;
			}
		}

		for (int i = 0; !isOrdered && i < right.getNumChildren(); i++) {
			T rightChild = right.getChild(i);
			if (rightChild.isOrdered()) {
				isOrdered = true;
			}
			if (!uniqueLabels || !rightChild.hasUniqueLabels()) {
				uniqueLabels = false;
			}
		}

		calls++;

        if (fullyOrdered) {
            orderedCalls++;
            
            if (LOG.isTraceEnabled()) {
                String matcherName = mceSubtreeMatcher.getClass().getSimpleName();
                LOG.trace(String.format("%s.match(%s, %s)", matcherName, left.getId(), right.getId()));
            }
            
            return mceSubtreeMatcher.match(context, left, right, lookAhead);
        }
        
		if (isOrdered) {
			orderedCalls++;

			if (LOG.isTraceEnabled()) {
				String matcherName = orderedMatcher.getClass().getSimpleName();
				LOG.trace(String.format("%s.match(%s, %s)", matcherName, left.getId(), right.getId()));
			}

			return orderedMatcher.match(context, left, right, lookAhead);
		} else {
			unorderedCalls++;

			if (uniqueLabels) {
				if (LOG.isTraceEnabled()) {
					String matcherName = unorderedLabelMatcher.getClass().getSimpleName();
					LOG.trace(String.format("%s.match(%s, %s)", matcherName, left.getId(), right.getId()));
				}

				return unorderedLabelMatcher.match(context, left, right, lookAhead);
			} else {
				if (LOG.isTraceEnabled()) {
					String matcherName = unorderedMatcher.getClass().getSimpleName();
					LOG.trace(String.format("%s.match(%s, %s)", matcherName, left.getId(), right.getId()));
				}

				return unorderedMatcher.match(context, left, right, lookAhead);
			}
		}
	}

	/**
	 * Stores the <code>Matching</code>s contained in <code>matchings</code> in the <code>Artifact</code>s they
	 * match.
	 *
	 * @param context
	 * 		the <code>MergeContext</code> of the current merge
	 * @param matchings
	 * 		the <code>Matchings</code> to store
	 * @param color
	 * 		the <code>Color</code> used to highlight the matchings in the debug output
	 */
	public void storeMatchings(MergeContext context, Matchings<T> matchings, Color color) {

		for (Matching<T> matching : matchings.optimized()) {

			if (matching.getScore() > 0) {
				T left = matching.getLeft();
				T right = matching.getRight();

				// TODO: collect statistical data about matching scores per language element and look-ahead setting
				if (left.matches(right)) {
					// regular top-down matching where the compared nodes do match
					matching.setHighlightColor(color);
					left.addMatching(matching);
					right.addMatching(matching);

					// just for statistics
					context.matchedElement(left);
					context.matchedElement(right);
				} else if (context.getLookAhead() != MergeContext.LOOKAHEAD_OFF) {
					// the compared nodes do not match but look-ahead is active and found matchings in the subtree

					// just for statistics
					context.skippedLeftElement(left, matching.getScore());
					context.skippedRightElement(left, matching.getScore());
				} else {
					// the compared nodes do not match and look-ahead is inactive: this is a serious bug!
					String msg = "Tried to store matching tree when lookahead is off and nodes do not match!";
					throw new RuntimeException(msg);
				}
			}
		}
	}

	/**
	 * Resets the call counters.
	 */
	public void reset() {
		calls = 0;
		unorderedCalls = 0;
		orderedCalls = 0;
	}

	/**
	 * Returns a formatted string describing the logged call counts.
	 *
	 * @return a log of the call counts
	 */
	public String getLog() {
		assert (calls == unorderedCalls + orderedCalls) : "Wrong sum for matcher calls";
		return "Matcher calls (all/ordered/unordered): " + calls + "/" + orderedCalls + "/" + unorderedCalls;
	}
}
