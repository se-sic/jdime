package de.fosd.jdime.matcher.cost_model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.ArtifactList;
import de.fosd.jdime.common.Artifacts;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.Tuple;
import de.fosd.jdime.matcher.MatcherInterface;
import de.fosd.jdime.matcher.matching.Matching;
import de.fosd.jdime.matcher.matching.Matchings;

import static de.fosd.jdime.matcher.cost_model.Bounds.BY_LOWER_UPPER;
import static java.lang.Integer.toHexString;
import static java.lang.System.identityHashCode;
import static java.util.Comparator.comparing;
import static java.util.logging.Level.FINEST;
import static java.util.stream.Collectors.summingDouble;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

public class CostModelMatcher<T extends Artifact<T>> implements MatcherInterface<T> {

    private static final Logger LOG = Logger.getLogger(CostModelMatcher.class.getCanonicalName());

    @FunctionalInterface
    public interface SimpleWeightFunction<T extends Artifact<T>> {

        float weigh(CostModelMatching<T> matching);
    }

    @FunctionalInterface
    public interface WeightFunction<T extends Artifact<T>> {

        float weigh(CostModelMatching<T>  matching, float quantity);
    }

    /**
     * The return type of {@link #objective(CMMatchings, CMParameters)} containing the value of the objective
     * function and the exact cost of the newly proposed set of <code>CostModelMatching</code>s.
     */
    private final class ObjectiveValue {

        public final double objValue;
        public final float matchingsCost;

        public ObjectiveValue(double objValue, float matchingsCost) {
            this.objValue = objValue;
            this.matchingsCost = matchingsCost;
        }
    }

    /**
     * The return type of {@link #acceptanceProb(double, CMMatchings, CMParameters)} containing the probability
     * of the newly proposed set of <code>CostModelMatching</code>s being accepted for the next iteration and the
     * <code>ObjectiveValue</code> for the proposed matchings.
     */
    private final class AcceptanceProbability {

        public final double acceptanceProbability;
        public final ObjectiveValue mHatObjectiveValue;

        public AcceptanceProbability(double acceptanceProbability, ObjectiveValue mHatObjectiveValue) {
            this.acceptanceProbability = acceptanceProbability;
            this.mHatObjectiveValue = mHatObjectiveValue;
        }
    }

    public float cost(MergeContext context, Matchings<T> matchings, T left, T right) {

        if (matchings.isEmpty()) {
            return 0;
        }

        Set<T> leftUnmatched = new LinkedHashSet<>(Artifacts.dfs(left));
        Set<T> rightUnmatched = new LinkedHashSet<>(Artifacts.dfs(right));

        CMMatchings<T> cmMatchings = new CMMatchings<>(left, right);

        for (Matching<T> matching : matchings) {
            cmMatchings.add(new CostModelMatching<>(matching.getLeft(), matching.getRight()));

            leftUnmatched.remove(matching.getLeft());
            rightUnmatched.remove(matching.getRight());
        }

        for (T l : leftUnmatched) {
            cmMatchings.add(new CostModelMatching<>(l, null));
        }

        for (T r : rightUnmatched) {
            cmMatchings.add(new CostModelMatching<>(null, r));
        }

        return cost(cmMatchings, new CMParameters<>(context));
    }

    /**
     * Returns the exact cost of the given <code>matchings</code>. This assumes that <code>matchings</code> contains
     * for every node in the left and right tree exactly one <code>CostModelMatching</code> containing the node.
     * The exact cost computed for every <code>CostModelMatching</code> can be retrieved using
     * ({@link CostModelMatching#getExactCost()} after this call.
     *
     * @param matchings
     *         the <code>CMMatchings</code>s to evaluate
     * @param parameters
     *          the <code>CMParameters</code> to use
     * @return the cost based on the weight functions in <code>parameters</code>
     */
    private float cost(CMMatchings<T> matchings, CMParameters<T> parameters) {

        if (!matchings.sane()) {
            throw new IllegalArgumentException("The given list of matchings has an invalid format. A list of " +
                    "matchings where every artifact from the left and right tree occurs in exactly one matching is " +
                    "required. Matchings matching artifacts that do not occur in the left or right tree are not " +
                    "allowed.");
        }

        if (matchings.isEmpty()) {
            return 0;
        }

        matchings.forEach(m -> exactCost(m, matchings, parameters));

        float sumCost = matchings.stream().collect(summingDouble(CostModelMatching::getExactCost)).floatValue();

        return (1.0f / (matchings.left.getTreeSize() + matchings.right.getTreeSize())) * sumCost;
    }

    /**
     * Sets the exact cost ({@link CostModelMatching#setExactCost(float)}) of the given <code>matching</code> based on
     * the given set of <code>matchings</code>.
     *
     *  @param matching
     *         the <code>CostModelMatching</code> to compute the cost for
     * @param matchings
     *         the complete <code>CostModelMatching</code>s
     * @param parameters
     *         the <code>CMParameters</code> to use
     */
    private void exactCost(CostModelMatching<T> matching, CMMatchings<T> matchings, CMParameters<T> parameters) {

        if (matching.isNoMatch()) {
            matching.setExactCost(parameters.wn);
            return;
        }

        float cR = renamingCost(matching, parameters);
        float cA = ancestryViolationCost(matching, matchings, parameters);
        float cS = siblingGroupBreakupCost(matching, matchings, parameters);
        float cO = orderingCost(matching, matchings, parameters);

        matching.setExactCost(cR + cA + cS + cO);
    }

    /**
     * Returns the cost for renaming the node. The cost will be zero if the <code>Artifact</code>s match according to
     * {@link Artifact#matches(Artifact)}, otherwise it is determined by the set renaming weight function
     * in <code>parameters</code>.
     *
     * @param matching
     *         the <code>CostModelMatching</code> to compute the cost for
     * @return the exact renaming cost of the <code>matching</code>
     */
    private float renamingCost(CostModelMatching<T> matching, CMParameters<T> parameters) {
        if (matching.m.matches(matching.n)) {
            return 0;
        } else {
            return parameters.wr.weigh(matching);
        }
    }

    private float ancestryViolationCost(CostModelMatching<T> matching, CMMatchings<T> matchings, CMParameters<T> parameters) {
        int numM = numAncestryViolatingChildren(matching.m, matching.n, matchings);
        int numN = numAncestryViolatingChildren(matching.n, matching.m, matchings);

        return parameters.wa.weigh(matching, numM + numN);
    }

    private int numAncestryViolatingChildren(T m, T n, CMMatchings<T> matchings) {
        ArtifactList<T> mChildren = m.getChildren();
        ArtifactList<T> nChildren = n.getChildren();

        Predicate<T> filter = a -> a != null && !nChildren.contains(a);

        return (int) mChildren.stream().map(mChild -> image(mChild, matchings)).filter(filter).count();
    }

    private float siblingGroupBreakupCost(CostModelMatching<T> matching, CMMatchings<T> matchings, CMParameters<T> parameters) {
        List<T> dMm, iMm;
        Set<T> fMm;
        List<T> dMn, iMn;
        Set<T> fMn;

        dMm = siblingDivergentSubset(matching.m, matching.n, matchings);
        iMm = siblingInvariantSubset(matching.m, matching.n, matchings);
        fMm = distinctSiblingFamilies(matching.m, matchings);

        dMn = siblingDivergentSubset(matching.n, matching.m, matchings);
        iMn = siblingInvariantSubset(matching.n, matching.m, matchings);
        fMn = distinctSiblingFamilies(matching.n, matchings);

        float mCost = (float) dMm.size() / (iMm.size() * fMm.size());
        float nCost = (float) dMn.size() / (iMn.size() * fMn.size());
        return parameters.ws.weigh(matching, mCost + nCost);
    }

    private List<T> siblingInvariantSubset(T m, T n, CMMatchings<T> matchings) {
        List<T> mSiblings = siblings(m);
        List<T> nSiblings = siblings(n);

        return mSiblings.stream().filter(s -> nSiblings.contains(image(s, matchings))).collect(toList());
    }

    private List<T> siblingDivergentSubset(T m, T n, CMMatchings<T> matchings) {
        List<T> inv = siblingInvariantSubset(m, n, matchings);
        return siblings(m).stream().filter(sibling -> !inv.contains(sibling) && image(sibling, matchings) != null).collect(toList());
    }

    private Set<T> distinctSiblingFamilies(T m, CMMatchings<T> matchings) {
        Function<T, T> image = mChild -> image(mChild, matchings);
        Predicate<T> notNull = t -> t != null;
        Function<T, T> getParent = Artifact::getParent;

        return siblings(m).stream().map(image).filter(notNull).map(getParent).collect(toSet());
    }

    private float orderingCost(CostModelMatching<T> matching, CMMatchings<T> matchings, CMParameters<T> parameters) {
        Stream<T> leftSiblings = otherSiblings(matching.m).stream();
        Stream<T> rightSiblings = otherSiblings(matching.n).stream();
        Stream<CostModelMatching<T>> s = concat(leftSiblings, rightSiblings).map(a -> matching(a, matchings)).filter(m -> !m.isNoMatch());

        if (s.anyMatch(toCheck -> violatesOrdering(toCheck, matching))) {
            return parameters.wo.weigh(matching);
        } else {
            return 0;
        }
    }

    private boolean violatesOrdering(CostModelMatching<T> toCheck, CostModelMatching<T> matching) {
        Tuple<T, T> leftSides = lca(toCheck.m, matching.m);
        Tuple<T, T> rightSides = lca(toCheck.n, matching.n);
        List<T> leftSiblings = siblings(leftSides.x);
        List<T> rightSiblings = siblings(rightSides.x);

        int leftXi = leftSiblings.indexOf(leftSides.x);
        int leftYi = leftSiblings.indexOf(leftSides.y);
        int rightXi = rightSiblings.indexOf(rightSides.x);
        int rightYi = rightSiblings.indexOf(rightSides.y);
        
        if (leftXi < leftYi) {
            return rightXi > rightYi;
        } else if (leftXi > leftYi) {
            return rightXi < rightYi;
        }

        return false; // TODO weird case, maybe true is better?
    }

    /**
     * Returns the path from the given <code>artifact</code> to the root node of the tree it is a part of.
     *
     * @param artifact
     *         the <code>Artifact</code> to return the path for
     * @return the path represented by a list of <code>Artifact</code>s beginning with <code>artifact</code> and ending
     *          with the root of the tree
     */
    public List<T> pathToRoot(T artifact) {
        List<T> path = new ArrayList<>();

        do {
            path.add(artifact);
            artifact = artifact.getParent();
        } while (artifact != null);

        return path;
    }

    /**
     * Finds the lowest pair of (possibly different) ancestors of <code>a</code> and <code>b</code> that are part of the
     * same sibling group.
     *
     * @param a
     *         the first <code>Artifact</code>
     * @param b
     *         the second <code>Artifact</code>
     * @return the ancestor of the first <code>Artifact</code> in the first position, that of the second in the second
     *          position
     */
    public Tuple<T, T> lca(T a, T b) {

        if (siblings(a).contains(b)) {
            return Tuple.of(a, b);
        }

        List<T> aPath = pathToRoot(a);
        List<T> bPath = pathToRoot(b);
        ListIterator<T> aIt = aPath.listIterator(aPath.size());
        ListIterator<T> bIt = bPath.listIterator(bPath.size());

        do {
            a = aIt.previous();
            b = bIt.previous();
        } while (a == b && (aIt.hasPrevious() && bIt.hasPrevious()));

        return Tuple.of(a, b);
    }

    /**
     * Finds the (first) <code>CostModelMatching</code> in <code>matchings</code> containing the given
     * <code>artifact</code>.
     *
     * @param artifact
     *         the <code>Artifact</code> for which the containing <code>CostModelMatching</code> is to be returned
     * @param matchings
     *         the current matchings
     * @return the <code>CostModelMatching</code> containing the <code>artifact</code>
     * @throws NoSuchElementException
     *         if no <code>CostModelMatching</code> containing <code>artifact</code> can be found in
     *         <code>matchings</code>
     */
    private CostModelMatching<T> matching(T artifact, CMMatchings<T> matchings) {
        //TODO this is very inefficient...

        Supplier<RuntimeException> ex = () -> new NoSuchElementException("No matching containing " + artifact + " found.");
        return matchings.stream().filter(m -> m.contains(artifact)).findFirst().orElseThrow(ex);
    }

    /**
     * Finds the (first) <code>CostModelMatching</code> in <code>matchings</code> containing the given
     * <code>artifact</code> and returns the other <code>Artifact</code> in the <code>CostModelMatching</code>.
     *
     * @param artifact
     *         the <code>Artifact</code> whose image is to be returned
     * @param matchings
     *         the current matchings
     * @return the matching partner of <code>artifact</code> in the given <code>matchings</code>
     * @throws NoSuchElementException
     *         if no <code>CostModelMatching</code> containing <code>artifact</code> can be found in
     *         <code>matchings</code>
     */
    private T image(T artifact, CMMatchings<T> matchings) {
        return matching(artifact, matchings).other(artifact);
    }

    /**
     * Sets the bounds ({@link CostModelMatching#setCostBounds(Bounds)}) for the cost of the given <code>matching</code>
     * based on the given <code>currentMatchings</code>.
     *
     * @param matching
     *         the <code>CostModelMatching</code> whose costs are to be bounded
     * @param currentMatchings
     *         the current <code>CMMatchings</code>s being considered
     * @param parameters
     *         the <code>CMParameters</code> to use
     */
    private void boundCost(CostModelMatching<T> matching, CMMatchings<T> currentMatchings, CMParameters<T> parameters) {

        if (matching.isNoMatch()) {
            matching.setBounds(parameters.wn, parameters.wn);
            return;
        }

        float cR = renamingCost(matching, parameters);
        Bounds cABounds = boundAncestryViolationCost(matching, currentMatchings, parameters);
        Bounds cSBounds = boundSiblingGroupBreakupCost(matching, currentMatchings, parameters);
        Bounds cOBounds = boundOrderingCost(matching, currentMatchings, parameters);

        float lower = cR + cABounds.getLower() + cSBounds.getLower() + cOBounds.getLower();
        float upper = cR + cABounds.getUpper() + cSBounds.getUpper() + cOBounds.getUpper();

        matching.setBounds(lower, upper);
    }

    private Bounds boundAncestryViolationCost(CostModelMatching<T> matching, CMMatchings<T> currentMatchings, CMParameters<T> parameters) {
        T m = matching.m;
        T n = matching.n;

        Stream<T> mLower = m.getChildren().stream().filter(mChild -> ancestryIndicator(mChild, n, currentMatchings, false));
        Stream<T> nLower = n.getChildren().stream().filter(nChild -> ancestryIndicator(nChild, m, currentMatchings, false));

        Stream<T> mUpper = m.getChildren().stream().filter(mChild -> ancestryIndicator(mChild, n, currentMatchings, true));
        Stream<T> nUpper = n.getChildren().stream().filter(nChild -> ancestryIndicator(nChild, m, currentMatchings, true));

        int lowerBound = (int) (mLower.count() + nLower.count());
        int upperBound = (int) (mUpper.count() + nUpper.count());

        return new Bounds(parameters.wa.weigh(matching, lowerBound), parameters.wa.weigh(matching, upperBound));
    }

    private boolean ancestryIndicator(T child, T n, CMMatchings<T> g, boolean upper) {
        Predicate<CostModelMatching<T>> contains = match -> match.contains(child);

        if (upper) {
            Predicate<CostModelMatching<T>> indicator = match -> {
                T partner = match.other(child);
                return !(partner == null || n.getChildren().contains(partner));
            };

            return g.stream().filter(contains).anyMatch(indicator);
        } else {
            Predicate<CostModelMatching<T>> indicator = match -> {
                T partner = match.other(child);
                return partner == null || n.getChildren().contains(partner);
            };

            return g.stream().filter(contains).noneMatch(indicator);
        }
    }

    private Bounds boundSiblingGroupBreakupCost(CostModelMatching<T> matching, CMMatchings<T> currentMatchings, CMParameters<T> parameters) {
        T m = matching.m;
        T n = matching.n;

        Bounds dMN = boundDistinctSibling(m, n, currentMatchings);
        Bounds dNM = boundDistinctSibling(n, m, currentMatchings);

        Bounds iMN = boundInvariantSiblings(m, n, currentMatchings);
        Bounds iNM = boundInvariantSiblings(n, m, currentMatchings);

        float mnLower = dMN.getLower() / (iMN.getUpper() * (dMN.getLower() + 1));
        float nmLower = dNM.getLower() / (iNM.getUpper() * (dNM.getLower() + 1));
        float lower = parameters.ws.weigh(matching, mnLower + nmLower);

        float mnUpper = dMN.getUpper() / iMN.getLower();
        float nmUpper = dNM.getUpper() / iNM.getLower();
        float upper = parameters.ws.weigh(matching, (mnUpper + nmUpper) / 2);

        return new Bounds(lower, upper);
    }

    private Bounds boundDistinctSibling(T m, T n, CMMatchings<T> currentMatchings) {
        long lower = otherSiblings(m).stream().filter(mSib -> distinctSiblingIndicator(mSib, n, currentMatchings, false)).count();
        long upper = otherSiblings(m).stream().filter(mSib -> distinctSiblingIndicator(mSib, n, currentMatchings, true)).count();

        return new Bounds(lower, upper);
    }

    private boolean distinctSiblingIndicator(T sibling, T n, CMMatchings<T> g, boolean upper) {
        Predicate<CostModelMatching<T>> contains = match -> match.contains(sibling);

        if (upper) {
            Predicate<CostModelMatching<T>> indicator = match -> {
                T partner = match.other(sibling);
                return !(partner == null || otherSiblings(n).contains(partner));
            };

            return g.stream().filter(contains).anyMatch(indicator);
        } else {
            Predicate<CostModelMatching<T>> indicator = match -> {
                T partner = match.other(sibling);
                return partner == null || otherSiblings(n).contains(partner);
            };

            return g.stream().filter(contains).noneMatch(indicator);
        }
    }

    private Bounds boundInvariantSiblings(T m, T n, CMMatchings<T> currentMatchings) {
        long lower = otherSiblings(m).stream().filter(mChild -> invariantSiblingIndicator(mChild, n, currentMatchings, false)).count();
        long upper = otherSiblings(m).stream().filter(mChild -> invariantSiblingIndicator(mChild, n, currentMatchings, true)).count();

        return new Bounds(lower + 1, upper + 1);
    }

    private boolean invariantSiblingIndicator(T child, T n, CMMatchings<T> g, boolean upper) {
        Predicate<CostModelMatching<T>> contains = match -> match.contains(child);
        Predicate<CostModelMatching<T>> indicator = match -> otherSiblings(n).contains(match.other(child));

        if (upper) {
            return g.stream().filter(contains).anyMatch(indicator);
        } else {
            return g.stream().filter(contains).allMatch(indicator);
        }
    }

    private Bounds boundOrderingCost(CostModelMatching<T> matching, CMMatchings<T> currentMatchings, CMParameters<T> parameters) {
        float lower, upper;

        Stream<T> siblings = concat(otherSiblings(matching.m).stream(), otherSiblings(matching.n).stream());

        boolean orderingPossible = siblings.allMatch(sib ->
            currentMatchings.stream().filter(match -> match.contains(sib)).anyMatch(match ->
                match.isNoMatch() || !violatesOrdering(match, matching)
            )
        );

        if (!orderingPossible) {
            lower = parameters.wo.weigh(matching);
            upper = lower;
        } else {
            lower = 0;

            siblings = concat(otherSiblings(matching.m).stream(), otherSiblings(matching.n).stream());

            boolean violationPossible = siblings.anyMatch(sib ->
                currentMatchings.stream().filter(match -> match.contains(sib)).anyMatch(match ->
                    !match.isNoMatch() && violatesOrdering(match, matching)
                )
            );

            upper = violationPossible ? parameters.wo.weigh(matching) : 0;
        }

        return new Bounds(lower, upper);
    }

    /**
     * Returns a new <code>List</code> containing the children of the parent of <code>artifact</code> or an empty
     * <code>List</code> for the root node. This includes the <code>artifact</code> itself.
     *
     * @param artifact
     *         the <code>Artifact</code> whose siblings are to be returned
     * @return the siblings of the given <code>artifact</code>
     */
    private List<T> siblings(T artifact) {
        T parent = artifact.getParent();

        if (parent == null) {
            return new ArrayList<>(Collections.singleton(artifact));
        } else {
            return new ArrayList<>(parent.getChildren());
        }
    }

    /**
     * Returns the siblings of <code>artifact</code> as in {@link #siblings(Artifact)} but does not include
     * <code>artifact</code> itself.
     *
     * @param artifact
     *         the <code>Artifact</code> whose siblings are to be returned
     * @return the siblings of the given <code>artifact</code>
     */
    private List<T> otherSiblings(T artifact) {
        List<T> siblings = siblings(artifact);
        siblings.remove(artifact);

        return siblings;
    }

    @Override
    public Matchings<T> match(MergeContext context, T left, T right) {
        CMParameters<T> parameters = new CMParameters<>(context);

        LOG.finer("Matching " + left + " and " + right + " using the " + getClass().getSimpleName());

        CMMatchings<T> m = initialize(left, right, parameters);
        ObjectiveValue mObjVal = objective(m, parameters);

        CMMatchings<T> lowest = m;
        float lowestCost = mObjVal.matchingsCost;

        for (int i = 0; i < context.costModelIterations; i++) {
            CMMatchings<T> mHat = propose(m, parameters);
            AcceptanceProbability mHatAccProb = acceptanceProb(mObjVal.objValue, mHat, parameters);

            if (parameters.chance(mHatAccProb.acceptanceProbability)) {

                log(FINEST, mHat, () -> "Accepting the matchings.");

                m = mHat;
                mObjVal = mHatAccProb.mHatObjectiveValue;
            }

            if (mHatAccProb.mHatObjectiveValue.matchingsCost < lowestCost) {

                log(FINEST, mHat, () -> "New lowest cost matchings found.");

                lowest = mHat;
                lowestCost = mHatAccProb.mHatObjectiveValue.matchingsCost;
            }

            LOG.finer("End of iteration " + i);
        }

        LOG.finer(() -> "Matching ended after " + context.costModelIterations + " iterations.");

        return convert(lowest);
    }

    /**
     * Converts a <code>List</code> of <code>CostModelMatching</code>s to an equivalent <code>Set</code> of
     * <code>Matching</code>s.
     *
     * @param matchings
     *         the <code>CostModelMatching</code>s to convert
     * @return the resulting <code>Matchings</code>
     */
    private Matchings<T> convert(CMMatchings<T> matchings) {
        return matchings.stream()
                        .filter(((Predicate<CostModelMatching<T>>) CostModelMatching::isNoMatch).negate())
                        .map(m -> new Matching<>(m.m, m.n, 0)) // TODO calculate a useful score
                        .collect(Matchings::new, Matchings::add, Matchings::addAll);
    }

    private CMMatchings<T> propose(CMMatchings<T> m, CMParameters<T> parameters) {
        Collections.sort(m, comparing(CostModelMatching::getExactCost));

        int j = parameters.rng.nextInt(m.size());
        CMMatchings<T> fixed = new CMMatchings<T>(m.subList(0, j), m.left, m.right);

        log(FINEST, m, () -> "Fixing the first " + j + " matchings from the last iteration. They are: " + fixed);

        CMMatchings<T> proposition = complete(fixed, parameters);

        log(FINEST, proposition, () -> "Proposing matchings for the next iteration: " + proposition);

        return proposition;
    }

    private CMMatchings<T> initialize(T left, T right, CMParameters<T> parameters) {
        CMMatchings<T> initial = complete(new CMMatchings<>(left, right), parameters);
        log(FINEST, initial, () -> "Initial set of matchings is: " + initial);

        return initial;
    }

    private CMMatchings<T> complete(CMMatchings<T> fixedMatchings, CMParameters<T> parameters) {
        CMMatchings<T> available = completeBipartiteGraph(fixedMatchings.left, fixedMatchings.right);
        Set<CostModelMatching<T>> fixed = new LinkedHashSet<>(fixedMatchings);

        fixed.forEach(m -> prune(m, available));

        while (fixed.size() != available.size()) {

            available.forEach(matching -> boundCost(matching, available, parameters));
            Collections.sort(available, comparing(CostModelMatching::getCostBounds, BY_LOWER_UPPER));

            CostModelMatching<T> matching;

            do {
                int i = 0;
                while (!parameters.chance(parameters.pAssign)) {
                    i = (i + 1) % available.size();
                }

                matching = available.get(i);
            } while (fixed.contains(matching));

            /* TODO
             * "If the candidate edge can be fixed (?) and at least one complete matching still exists (???)"
             *
             * if (???) {
             *     continue;
             * }
             */

            fixed.add(matching);
            prune(matching, available);
        }

        return new CMMatchings<>(fixed, fixedMatchings.left, fixedMatchings.right);
    }

    private void prune(CostModelMatching<T> matching, CMMatchings<T> g) {

        for (ListIterator<CostModelMatching<T>> it = g.listIterator(); it.hasNext();) {
            CostModelMatching<T> current = it.next();
            boolean neq = !matching.equals(current);

            if (neq && ((matching.m != null && matching.m == current.m) || (matching.n != null && matching.n == current.n))) {
                it.remove();
            }
        }
    }

    private CMMatchings<T> completeBipartiteGraph(T left, T right) {
        List<T> leftNodes = Artifacts.bfs(left);
        List<T> rightNodes = Artifacts.bfs(right);

        // add the "No Match" node
        leftNodes.add(null);
        rightNodes.add(null);

        CMMatchings<T> bipartiteGraph = new CMMatchings<>(left, right);

        for (T lNode : leftNodes) {
            for (T rNode : rightNodes) {

                if (lNode != null || rNode != null) {
                    bipartiteGraph.add(new CostModelMatching<>(lNode, rNode));
                }
            }
        }

        return bipartiteGraph;
    }

    private ObjectiveValue objective(CMMatchings<T> matchings, CMParameters<T> parameters) {
        float cost = cost(matchings, parameters);
        double objVal = Math.exp(-(parameters.beta * cost));

        log(FINEST, matchings, () -> "Cost of matchings is " + cost);
        log(FINEST, matchings, () -> "Objective function value is " + objVal);

        return new ObjectiveValue(objVal, cost);
    }

    private AcceptanceProbability acceptanceProb(double mObjectiveValue, CMMatchings<T> mHat, CMParameters<T> parameters) {
        ObjectiveValue mHatObjectiveValue = objective(mHat, parameters);
        double acceptanceProb = Math.min(1, mHatObjectiveValue.objValue / mObjectiveValue);

        log(FINEST, mHat, () -> "Acceptance probability for matchings is " + acceptanceProb);

        return new AcceptanceProbability(acceptanceProb, mHatObjectiveValue);
    }

    /**
     * Returns the hexadecimal identity hash code of the given <code>Object</code> as a <code>String</code>.
     *
     * @param o
     *         the <code>Object</code> to return the <code>String</code> id for
     * @return the <code>String</code> id
     */
    private String id(Object o) {
        return toHexString(identityHashCode(o));
    }

    /**
     * Logs the given <code>msg</code> using the {@link #LOG} and prepends the {@link #id(Object)} of the given
     * matchings.
     *
     * @param level
     *         the level to log at
     * @param matchings
     *         the matchings the message concerns
     * @param msg
     *         the message to log
     */
    private void log(Level level, CMMatchings<T> matchings, Supplier<String> msg) {
        LOG.log(level, () -> String.format("%-10s%s", id(matchings), msg.get()));
    }
}
