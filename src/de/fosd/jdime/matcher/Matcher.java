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
package de.fosd.jdime.matcher;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.config.merge.Revision;
import de.fosd.jdime.matcher.cost_model.CMMode;
import de.fosd.jdime.matcher.cost_model.CostModelMatcher;
import de.fosd.jdime.matcher.matching.Color;
import de.fosd.jdime.matcher.matching.LookAheadMatching;
import de.fosd.jdime.matcher.matching.Matching;
import de.fosd.jdime.matcher.matching.Matchings;
import de.fosd.jdime.matcher.ordered.EqualityMatcher;
import de.fosd.jdime.matcher.ordered.OrderedMatcher;
import de.fosd.jdime.matcher.ordered.mceSubtree.MCESubtreeMatcher;
import de.fosd.jdime.matcher.ordered.simpleTree.SimpleTreeMatcher;
import de.fosd.jdime.matcher.unordered.IdenticalSubtreeMatcher;
import de.fosd.jdime.matcher.unordered.UniqueLabelMatcher;
import de.fosd.jdime.matcher.unordered.UnorderedMatcher;
import de.fosd.jdime.matcher.unordered.assignmentProblem.HungarianMatcher;
import de.fosd.jdime.stats.KeyEnums;
import de.fosd.jdime.strdump.DumpMode;
import de.fosd.jdime.util.UnorderedTuple;

import static de.fosd.jdime.config.merge.MergeContext.LOOKAHEAD_OFF;
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

    private UnorderedMatcher<T> unorderedMatcher;
    private UnorderedMatcher<T> unorderedLabelMatcher;
    private OrderedMatcher<T> orderedMatcher;
    private OrderedMatcher<T> mceSubtreeMatcher;

    private IdenticalSubtreeMatcher<T> idSubtreeMatcher;

    private CostModelMatcher<T> cmMatcher;

    private T leftRoot;
    private T rightRoot;

    private Map<T, MatcherCache<T>> caches;
    private MatcherCache<T> leftCache;
    private MatcherCache<T> rightCache;

    /**
     * Constructs a new {@link Matcher} matching the given trees.
     *
     * @param leftRoot
     *         the root of the left tree
     * @param rightRoot
     *         the root of the right tree
     */
    public Matcher(T leftRoot, T rightRoot) {

        // no method reference because this syntax makes setting a breakpoint for debugging easier
        MatcherInterface<T> rootMatcher = (context, left, right) -> {
            return match(context, left, right);
        };

        this.unorderedMatcher = new HungarianMatcher<>(rootMatcher);
        this.unorderedLabelMatcher = new UniqueLabelMatcher<>(rootMatcher);
        this.orderedMatcher = new SimpleTreeMatcher<>(rootMatcher);
        this.mceSubtreeMatcher = new MCESubtreeMatcher<>(rootMatcher);

        this.idSubtreeMatcher = new IdenticalSubtreeMatcher<>();
        this.cmMatcher = new CostModelMatcher<>();

        this.leftRoot = leftRoot;
        this.rightRoot = rightRoot;

        this.caches = new HashMap<>();
    }

    /**
     * Constructs a new {@link Matcher} matching the given trees. All caches from {@code oldMatcher} will be reused.
     *
     * @param oldMatcher
     *         the {@link Matcher} whose caches are to be reused, for convenience it may be {@code null} in which case
     *         it is ignored
     * @param leftRoot
     *         the root of the left tree
     * @param rightRoot
     *         the root of the right tree
     */
    public Matcher(Matcher<T> oldMatcher, T leftRoot, T rightRoot) {
        this(leftRoot, rightRoot);

        if (oldMatcher != null) {
            this.caches.putAll(oldMatcher.caches);
        }
    }

    /**
     * Removes all cached data concerning the given tree.
     *
     * @param root
     *         the root of the tree whose cached information is the be removed
     */
    public void removeCache(T root) {
        caches.remove(root);
    }

    /**
     * Calculates the matchings between the trees this {@link Matcher} was constructed for and stores the resulting
     * {@link Matching matchings} in the matched {@link Artifact artifacts}.
     *
     * @param context
     *         the {@link MergeContext} containing the configuration values to be used for matching
     * @param color
     *         color of the matching (for debug output only)
     * @return <code>Matchings</code> of the two nodes
     */
    public Matchings<T> match(MergeContext context, Color color) {
        Matchings<T> matchings;

        if (context.getCMMatcherMode() == CMMode.REPLACEMENT) {
            matchings = cmMatcher.match(context, leftRoot, rightRoot);
        } else {
            leftCache = caches.computeIfAbsent(leftRoot, i -> new MatcherCache<>());
            rightCache = caches.computeIfAbsent(rightRoot, i -> new MatcherCache<>());
            idSubtreeMatcher.matchTrees(leftRoot, rightRoot);

            matchings = match(context, leftRoot, rightRoot);

            if (context.getCMMatcherMode() == CMMode.POST_PROCESSOR && matchings.get(leftRoot, rightRoot).map(m -> !m.hasFullyMatched()).orElse(true)) {
                matchings = cmMatcher.match(context, leftRoot, rightRoot, matchings);
            }
        }

        matchings.get(leftRoot, rightRoot).ifPresent(m ->
            LOG.fine(() -> {
                Revision lRev = leftRoot.getRevision();
                Revision rRev = rightRoot.getRevision();
                return String.format("Matched revision %s and %s with score %d", lRev, rRev, m.getScore());
            })
        );

        storeMatchings(context, matchings, color);

        if (LOG.isLoggable(Level.FINEST)) {
            Revision lRev = leftRoot.getRevision();
            Revision rRev = rightRoot.getRevision();
            String msg = String.format("Dumping matching of %s and %s%n%s", lRev, rRev, matchings);
            LOG.finest(msg);
        }

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine(String.format("%s.dumpTree():%n%s", leftRoot.getRevision(), leftRoot.dump(DumpMode.PLAINTEXT_TREE)));
            LOG.fine(String.format("%s.dumpTree():%n%s", rightRoot.getRevision(), rightRoot.dump(DumpMode.PLAINTEXT_TREE)));
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

        /*
         * Before firing up potentially expensive matching algorithms, we check whether the trees are identical.
         * To avoid redundant calls, we save the matchings reported by EqualityMatcher and perform lookups on
         * subsequent runs.
         */
        Optional<Matchings<T>> trivialMatches = getTrivialMatchings(context, left, right);

        if (trivialMatches.isPresent()) {
            logMatcherUse(EqualityMatcher.class, left, right);
            return trivialMatches.get();
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

    /**
     * Returns the trivial Matchings if <code>left</code> and <code>right</code> are exactly equal as determined by
     * the <code>IdenticalSubtreeMatcher</code>.
     *
     * @param context
     *         the <code>MergeContext</code>
     * @param left
     *         the left tree
     * @param right
     *         the right tree
     * @return the <code>Matchings</code>
     */
    private Optional<Matchings<T>> getTrivialMatchings(MergeContext context, T left, T right) {
        if (idSubtreeMatcher.hasMatched(left, right)) {
            return Optional.of(idSubtreeMatcher.match(context, left, right));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Determines which <code>Matcher</code> to use for matching <code>left</code> and <code>right</code> and returns
     * the resulting <code>Matchings</code>.
     *
     * @param context
     *         the <code>MergeContext</code>
     * @param left
     *         the left tree
     * @param right
     *         the right tree
     * @return the <code>Matchings</code>
     */
    private Matchings<T> getMatchings(MergeContext context, T left, T right) {
        boolean fullyOrderedChildren = false;

        if (context.isUseMCESubtreeMatcher()) {
            boolean leftOrdered = left.getChildren().stream().allMatch(c -> leftCache.fullyOrdered(c));
            boolean rightOrdered = right.getChildren().stream().allMatch(c -> rightCache.fullyOrdered(c));
            fullyOrderedChildren = leftOrdered && rightOrdered;
        }

        boolean onlyOrderedChildren = leftCache.orderedChildren(left) && rightCache.orderedChildren(right);
        boolean onlyLabeledChildren = leftCache.uniquelyLabeledChildren(left) && rightCache.uniquelyLabeledChildren(right);

        Matchings<T> matchings;

        if (fullyOrderedChildren && context.isUseMCESubtreeMatcher()) {
            logMatcherUse(mceSubtreeMatcher.getClass(), left, right);
            matchings = mceSubtreeMatcher.match(context, left, right);
        } else if (onlyOrderedChildren) {
            logMatcherUse(orderedMatcher.getClass(), left, right);
            matchings = orderedMatcher.match(context, left, right);
        } else {
            if (onlyLabeledChildren) {
                logMatcherUse(unorderedLabelMatcher.getClass(), left, right);
                matchings = unorderedLabelMatcher.match(context, left, right);
            } else {
                logMatcherUse(unorderedMatcher.getClass(), left, right);
                matchings = unorderedMatcher.match(context, left, right);
            }
        }

        if (context.getCMMatcherMode() != CMMode.INTEGRATED) {
            return matchings;
        }

        Optional<Matching<T>> oMatch = matchings.get(left, right);

        if (oMatch.isPresent()) {
            Matching<T> prevMatch = oMatch.get();

            if (prevMatch.getPercentage() > 0 && prevMatch.getPercentage() < context.getCmReMatchBound()) { //TODO we may want to remove the first condition
                Matchings<T> newMatchings = cmMatcher.match(context, left, right);
                oMatch = newMatchings.get(left, right);

                if (oMatch.isPresent() && oMatch.get().getPercentage() > prevMatch.getPercentage()) {
                    matchings = newMatchings;
                }
            }
        } else {
            LOG.warning(() -> "Did not receive a matching for " + left + " " + right + " from the concrete matchers.");
        }

        return matchings;
    }

    /**
     * If <code>left</code> and <code>right</code> do not match, this method attempts to find two <code>Artifacts</code>
     * (children of <code>left</code> and <code>right</code>) with which to resume matching the two trees. Depending
     * on the type of the <code>Artifact</code>s a different lookahead will be performed. E.g. in the case of two
     * METHOD <code>Artifact</code>s they themselves will be returned to try and detect renamings. If one of them is
     * a TRY <code>Artifact</code>, the method will attempt to find a node matching the other and return them as a
     * tuple. This is an attempt to find code that was surrounded by a try/catch block.
     *
     * @param context
     *         the <code>MergeContext</code>
     * @param left
     *         the left tree
     * @param right
     *         the right tree
     * @return optionally the two <code>Artifact</code>s to try and match instead of <code>left</code> and
     *          <code>right</code>
     */
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
            return resume.map(t -> UnorderedTuple.of(t, right));
        } else if (rType == TRY) {
            Optional<T> resume = findMatchingNode(right, left, rightLAH);
            return resume.map(t -> UnorderedTuple.of(left, t));
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
            String matcherName = c.getSimpleName();
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
    public void storeMatchings(MergeContext context, Matchings<T> matchings, Color color) {
        LOG.finest("Store matching information within nodes.");

        for (Matching<T> matching : matchings.optimized()) {

            if (matching.getScore() > 0) {
                T left = matching.getLeft();
                T right = matching.getRight();

                KeyEnums.Type rType = right.getType();
                KeyEnums.Type lType = left.getType();

                if (context.getCMMatcherMode() == CMMode.OFF &&
                        context.getLookahead(lType) == LOOKAHEAD_OFF &&
                        context.getLookahead(rType) == LOOKAHEAD_OFF &&
                        !left.matches(right)) {

                    String format = "Tried to store a non-lookahead matching between %s and %s that do not match.\n"
                            + "The offending matching was created by %s!";
                    String msg = String.format(format, left.getId(), right.getId(), matching.getAlgorithm());
                    throw new RuntimeException(msg);
                }

                matching.setHighlightColor(color);
                left.addMatching(matching);
                right.addMatching(matching);
                LOG.finest(String.format("Store matching for %s and %s (%s).", left.getId(), right.getId(), matching.getAlgorithm()));
            }
        }
    }
}
