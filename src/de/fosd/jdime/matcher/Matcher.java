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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.matcher.matching.Color;
import de.fosd.jdime.matcher.matching.Matching;
import de.fosd.jdime.matcher.matching.Matchings;
import de.fosd.jdime.matcher.ordered.OrderedMatcher;
import de.fosd.jdime.matcher.ordered.mceSubtree.MCESubtreeMatcher;
import de.fosd.jdime.matcher.ordered.simpleTree.SimpleTreeMatcher;
import de.fosd.jdime.matcher.unordered.UniqueLabelMatcher;
import de.fosd.jdime.matcher.unordered.UnorderedMatcher;
import de.fosd.jdime.matcher.unordered.assignmentProblem.HungarianMatcher;

import static de.fosd.jdime.JDimeConfig.USE_MCESUBTREE_MATCHER;
import static de.fosd.jdime.JDimeConfig.getConfig;

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
public class Matcher<T extends Artifact<T>> {

    private static final Logger LOG = Logger.getLogger(Matcher.class.getCanonicalName());
    private static final String ID = Matcher.class.getSimpleName();

    private boolean useMCESubtreeMatcher;

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
        unorderedMatcher = new HungarianMatcher<>(this::match);
        unorderedLabelMatcher = new UniqueLabelMatcher<>(this::match);
        orderedMatcher = new SimpleTreeMatcher<>(this::match);
        mceSubtreeMatcher = new MCESubtreeMatcher<>(this::match);
        useMCESubtreeMatcher = getConfig().getBoolean(USE_MCESUBTREE_MATCHER).orElse(false);
    }

    /**
     * Compares two nodes and returns matchings between them and possibly their sub-nodes.
     *
     * @param context
     *         <code>MergeContext</code>
     * @param left
     *         left node
     * @param right
     *         right node
     * @param color
     *         color of the matching (for debug output only)
     * @return <code>Matchings</code> of the two nodes
     */
    public Matchings<T> match(MergeContext context, T left, T right, Color color) {
        Matchings<T> matchings = match(context, left, right, context.getLookAhead());
        Matching<T> matching = matchings.get(left, right).get();

        LOG.fine(() -> String.format("match(%s, %s) = %d", left.getRevision(), right.getRevision(), matching.getScore()));
        LOG.fine(this::getLog);

        storeMatchings(context, matchings, color);

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest(String.format("Dumping matching of %s and %s", left.getRevision(), right.getRevision()));
            System.out.println(matchings);
        }

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine(left.getRevision() + ".dumpTree():");
            System.out.println(left.dumpTree());

            LOG.fine(right.getRevision() + ".dumpTree():");
            System.out.println(right.dumpTree());
        }

        return matchings;
    }

    /**
     * @see MatcherInterface#match(MergeContext, Artifact, Artifact, int)
     */
    private Matchings<T> match(MergeContext context, T left, T right, int lookAhead) {

        if (left.isConflict()) {
            Matchings<T> m = Matchings.of(left, right, 0);
            m.get(left, right).get().setAlgorithm(ID);

            return m;
        }

        if (left.isChoice()) {
            // We have to split the choice node into its variants and create a matching for each one.
            // The highest matching is returned.

            LOG.finest(() -> {
                String name = getClass().getSimpleName();
                return String.format("%s encountered a choice node (%s)", name, left.getId());
            });

            Map<Integer, Matchings<T>> variantMatches = new HashMap<>();

            for (T variant: left.getVariants().values()) {
                LOG.finest(() -> {
                    String name = getClass().getSimpleName();
                    return String.format("%s.match(%s, %s)", name, variant.getId(), right.getId());
                });

                Matchings<T> cur = match(context, variant, right, lookAhead);
                Matching<T> highest = cur.get(variant, right).get();
                variantMatches.put(highest.getScore(), cur);
            }

            Matchings<T> maxMatching = variantMatches.get(Collections.max(variantMatches.keySet()));

            LOG.finest(() -> {
                String name = this.getClass().getSimpleName();
                return String.format("%s: highest match: %s", name, maxMatching);
            });

            return maxMatching;
        }

        int rootMatching = left.matches(right) ? 1 : 0;

        if (rootMatching == 0) {
            if (lookAhead == 0) {
                /*
                 * The roots do not match and we cannot use the look-ahead feature.  We therefore ignore the rest of the
                 * subtrees and return early to save time.
                 */

                LOG.finest(() -> {
                    String format = "%s - early return while matching %s and %s (LookAhead = %d)";
                    return String.format(format, ID, left.getId(), right.getId(), context.getLookAhead());
                });

                Matchings<T> m = Matchings.of(left, right, rootMatching);
                m.get(left, right).get().setAlgorithm(ID);

                return m;
            } else if (lookAhead > 0) {
                lookAhead = lookAhead - 1;
            }
        } else if (context.isLookAhead()) {
            lookAhead = context.getLookAhead();
        }

        boolean fullyOrdered = useMCESubtreeMatcher;
        boolean isOrdered = false;
        boolean uniqueLabels = true;

        Queue<T> wait = new LinkedList<>(left.getChildren());
        wait.addAll(right.getChildren());

        while (fullyOrdered && !wait.isEmpty()) {
            T node = wait.poll();
            fullyOrdered = node.isOrdered();

            node.getChildren().forEach(wait::offer);
        }
        
        for (int i = 0; !isOrdered && i < left.getNumChildren(); i++) {
            T leftChild = left.getChild(i);

            if (leftChild.isOrdered()) {
                isOrdered = true;
            }

            if (!uniqueLabels || !leftChild.getUniqueLabel().isPresent()) {
                uniqueLabels = false;
            }
        }

        for (int i = 0; !isOrdered && i < right.getNumChildren(); i++) {
            T rightChild = right.getChild(i);

            if (rightChild.isOrdered()) {
                isOrdered = true;
            }

            if (!uniqueLabels || !rightChild.getUniqueLabel().isPresent()) {
                uniqueLabels = false;
            }
        }

        calls++;

        if (fullyOrdered) {
            orderedCalls++;

            logMatcherUse(mceSubtreeMatcher.getClass(), left, right);
            return mceSubtreeMatcher.match(context, left, right, lookAhead);
        }
        
        if (isOrdered) {
            orderedCalls++;

            logMatcherUse(orderedMatcher.getClass(), left, right);
            return orderedMatcher.match(context, left, right, lookAhead);
        } else {
            unorderedCalls++;

            if (uniqueLabels) {
                logMatcherUse(unorderedLabelMatcher.getClass(), left, right);
                return unorderedLabelMatcher.match(context, left, right, lookAhead);
            } else {
                logMatcherUse(unorderedMatcher.getClass(), left, right);
                return unorderedMatcher.match(context, left, right, lookAhead);
            }
        }
    }

    /**
     * Logs the use of a <code>MatcherInterface</code> implementation to match <code>left</code> and
     * <code>right</code>.
     *
     * @param c the <code>MatcherInterface</code> that is used
     * @param left the left <code>Artifact</code> that is matched
     * @param right the right <code>Artifact</code> that is matched
     */
    private void logMatcherUse(Class<?> c, T left, T right) {
        LOG.finest(() -> {
            String matcherName = c.getClass().getSimpleName();
            return String.format("%s.match(%s, %s)", matcherName, left.getId(), right.getId());
        });
    }

    /**
     * Stores the <code>Matching</code>s contained in <code>matchings</code> in the <code>Artifact</code>s they
     * match.
     *
     * @param context
     *         the <code>MergeContext</code> of the current merge
     * @param matchings
     *         the <code>Matchings</code> to store
     * @param color
     *         the <code>Color</code> used to highlight the matchings in the debug output
     */
    private void storeMatchings(MergeContext context, Matchings<T> matchings, Color color) {
        LOG.finest("Store matching information within nodes.");

        for (Matching<T> matching : matchings.optimized()) {

            if (matching.getScore() > 0) {
                T left = matching.getLeft();
                T right = matching.getRight();

                if (context.getLookAhead() == MergeContext.LOOKAHEAD_OFF && !left.matches(right)) {
                    String format = "Tried to store a non-lookahead matching between %s and %s that do not match.";
                    String msg = String.format(format, left.getId(), right.getId());
                    throw new RuntimeException(msg);
                }

                matching.setHighlightColor(color);
                left.addMatching(matching);
                right.addMatching(matching);
            }
        }
    }

    /**
     * Returns a formatted string describing the logged call counts.
     *
     * @return a log of the call counts
     */
    private String getLog() {
        assert (calls == unorderedCalls + orderedCalls) : "Wrong sum for matcher calls";
        return "Matcher calls (all/ordered/unordered): " + calls + "/" + orderedCalls + "/" + unorderedCalls;
    }
}
