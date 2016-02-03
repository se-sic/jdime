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
package de.fosd.jdime.matcher;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.UnorderedTuple;
import de.fosd.jdime.matcher.matching.Color;
import de.fosd.jdime.matcher.matching.LookAheadMatching;
import de.fosd.jdime.matcher.matching.Matching;
import de.fosd.jdime.matcher.matching.Matchings;
import de.fosd.jdime.matcher.ordered.OrderedMatcher;
import de.fosd.jdime.matcher.ordered.mceSubtree.MCESubtreeMatcher;
import de.fosd.jdime.matcher.ordered.simpleTree.SimpleTreeMatcher;
import de.fosd.jdime.matcher.unordered.UniqueLabelMatcher;
import de.fosd.jdime.matcher.unordered.UnorderedMatcher;
import de.fosd.jdime.matcher.unordered.assignmentProblem.HungarianMatcher;
import de.fosd.jdime.stats.KeyEnums;
import de.fosd.jdime.strdump.DumpMode;

import static de.fosd.jdime.common.MergeContext.LOOKAHEAD_OFF;
import static de.fosd.jdime.stats.KeyEnums.Type.METHOD;
import static de.fosd.jdime.stats.KeyEnums.Type.TRY;

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

        // no method reference because this syntax makes setting a breakpoint for debugging easier
        MatcherInterface<T> rootMatcher = (context, left, right) -> {
            return match(context, left, right);
        };

        unorderedMatcher = new HungarianMatcher<>(rootMatcher);
        unorderedLabelMatcher = new UniqueLabelMatcher<>(rootMatcher);
        orderedMatcher = new SimpleTreeMatcher<>(rootMatcher);
        mceSubtreeMatcher = new MCESubtreeMatcher<>(rootMatcher);
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
        Matchings<T> matchings = match(context, left, right);
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
            System.out.println(left.dump(DumpMode.PLAINTEXT_TREE));

            LOG.fine(right.getRevision() + ".dumpTree():");
            System.out.println(right.dump(DumpMode.PLAINTEXT_TREE));
        }

        return matchings;
    }

    /**
     * @see MatcherInterface#match(MergeContext, Artifact, Artifact)
     */
    private Matchings<T> match(MergeContext context, T left, T right) {

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

                Matchings<T> cur = match(context, variant, right);
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

        if (!left.matches(right)) {
            Optional<UnorderedTuple<T, T>> resumeTuple = lookAhead(context, left, right);

            if (resumeTuple.isPresent()) {
                UnorderedTuple<T, T> toMatch = resumeTuple.get();

                Matchings<T> subMatchings = getMatchings(context, toMatch.getX(), toMatch.getY());
                Matching<T> subMatching = subMatchings.get(toMatch.getX(), toMatch.getY()).orElseThrow(() -> new RuntimeException("Hilfe"));

                Matching<T> lookAheadMatching = new LookAheadMatching<>(subMatching, left, right);

                subMatchings.remove(subMatching);
                subMatchings.add(lookAheadMatching);

                return subMatchings;
            } else {
                /*
                 * The roots do not match and we cannot use the look-ahead feature.  We therefore ignore the rest of the
                 * subtrees and return early to save time.
                 */

                LOG.finest(() -> {
                    String format = "%s - early return while matching %s and %s (LookAhead = %d)";
                    return String.format(format, ID, left.getId(), right.getId(), context.getLookAhead());
                });

                Matchings<T> m = Matchings.of(left, right, 0);
                m.get(left, right).get().setAlgorithm(ID);

                return m;
            }
        }

        return getMatchings(context, left, right);
    }

    private Matchings<T> getMatchings(MergeContext context, T left, T right) {
        boolean fullyOrdered = context.isUseMCESubtreeMatcher();
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
            return mceSubtreeMatcher.match(context, left, right);
        }

        if (isOrdered) {
            orderedCalls++;

            logMatcherUse(orderedMatcher.getClass(), left, right);
            return orderedMatcher.match(context, left, right);
        } else {
            unorderedCalls++;

            if (uniqueLabels) {
                logMatcherUse(unorderedLabelMatcher.getClass(), left, right);
                return unorderedLabelMatcher.match(context, left, right);
            } else {
                logMatcherUse(unorderedMatcher.getClass(), left, right);
                return unorderedMatcher.match(context, left, right);
            }
        }
    }

    private Optional<UnorderedTuple<T, T>> lookAhead(MergeContext context, T left, T right) {

        if (!context.isLookAhead()) {
            return Optional.empty();
        }

        KeyEnums.Type lType = left.getType();
        KeyEnums.Type rType = right.getType();
        int leftLAH = context.getLookahead(lType);
        int rightLAH = context.getLookahead(rType);

        if (leftLAH == LOOKAHEAD_OFF && rightLAH == LOOKAHEAD_OFF) {
            return Optional.empty();
        }

        if (lType == METHOD && rType == METHOD) {
            assert leftLAH != LOOKAHEAD_OFF && rightLAH != LOOKAHEAD_OFF;
            return Optional.of(UnorderedTuple.of(left, right));
        } else if (lType == TRY) {
            Optional<T> resume = findMatchingNode(left, right, leftLAH);

            if (resume.isPresent()) {
                return Optional.of(UnorderedTuple.of(resume.get(), right));
            } else {
                return Optional.empty();
            }
        } else if (rType == TRY) {
            Optional<T> resume = findMatchingNode(right, left, rightLAH);

            if (resume.isPresent()) {
                return Optional.of(UnorderedTuple.of(left, resume.get()));
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    /**
     * Performs a depth first search of the given <code>tree</code> and returns the first node matching
     * <code>nodeToFind</code> as per the {@link Artifact#matches(Artifact)} method.
     *
     * @param tree
     *         the tree to search in
     * @param nodeToFind
     *         the node to find a match for
     * @param maxDepth
     *         the maximum depth of nodes to consider (root is a depth 0)
     * @return optionally a matching node for <code>nodeToFind</code>
     */
    private Optional<T> findMatchingNode(T tree, T nodeToFind, int maxDepth) {

        if (maxDepth < 0) {
            return Optional.empty();
        }

        if (tree.matches(nodeToFind)) {
            return Optional.of(tree);
        }

        for (T child : tree.getChildren()) {
            Optional<T> matchingNode = findMatchingNode(child, nodeToFind, maxDepth - 1);

            if (matchingNode.isPresent()) {
                return matchingNode;
            }
        }

        return Optional.empty();
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

                KeyEnums.Type rType = right.getType();
                KeyEnums.Type lType = left.getType();

                if (context.getLookahead(lType) == LOOKAHEAD_OFF && context.getLookahead(rType) == LOOKAHEAD_OFF &&
                        !left.matches(right)) {

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
