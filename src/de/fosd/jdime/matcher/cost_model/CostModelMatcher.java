package de.fosd.jdime.matcher.cost_model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Random;
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

public class CostModelMatcher<T extends Artifact<T>> implements MatcherInterface<T> {

    private static final Logger LOG = Logger.getLogger(CostModelMatcher.class.getCanonicalName());

    @FunctionalInterface
    private interface SimpleWeightFunction<T extends Artifact<T>> {

        float weigh(CostModelMatching<T> matching);
    }

    @FunctionalInterface
    private interface WeightFunction<T extends Artifact<T>> {

        float weigh(CostModelMatching<T>  matching, float quantity);
    }

    /**
     * The return type of {@link #objective(float, List)} containing the value of the objective function and the exact
     * cost of the newly proposed set of <code>CostModelMatching</code>s.
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
     * The return type of {@link #acceptanceProb(float, double, List)} containing the probability of the newly proposed
     * set of <code>CostModelMatching</code>s being accepted for the next iteration and the <code>ObjectiveValue</code>
     * for the proposed matchings.
     */
    private final class AcceptanceProbability {

        public final double acceptanceProbability;
        public final ObjectiveValue mHatObjectiveValue;

        public AcceptanceProbability(double acceptanceProbability, ObjectiveValue mHatObjectiveValue) {
            this.acceptanceProbability = acceptanceProbability;
            this.mHatObjectiveValue = mHatObjectiveValue;
        }
    }

    /**
     * The cost of not matching an artifact.
     */
    private float wn;

    /**
     * The function determining the cost of renaming an artifact. This cost is 0 if the artifacts match according to
     * {@link Artifact#matches}.
     */
    private SimpleWeightFunction<T> wr;

    /**
     * The function determining the cost of ancestry violations.
     */
    private WeightFunction<T> wa;

    /**
     * The function determining the cost of breaking up sibling groups.
     */
    private WeightFunction<T> ws;

    private Random rng;

    public void setNoMatchWeight(float wn) {
        this.wn = wn;
    }

    public void setRenamingWeight(float wr) {
        setRenamingWeight(matching -> wr);
    }

    public void setRenamingWeight(SimpleWeightFunction<T> wr) {
        this.wr = wr;
    }

    public void setAncestryViolationWeight(float wa) {
        setAncestryViolationWeight((matching, quantity) -> wa * quantity);
    }

    public void setAncestryViolationWeight(WeightFunction<T> wa) {
        this.wa = wa;
    }

    public void setSiblingGroupBreakupWeight(float ws) {
        setSiblingGroupBreakupWeight((matching, quantity) -> ws * quantity);
    }

    public void setSiblingGroupBreakupWeight(WeightFunction<T> ws) {
        this.ws = ws;
    }

    /**
     * Returns the exact cost of the given <code>matchings</code>. This assumes that <code>matchings</code> contains
     * for every node in the left and right tree exactly one <code>CostModelMatching</code> containing the node.
     * The exact cost computed for every <code>CostModelMatching</code> can be retrieved using
     * ({@link CostModelMatching#getExactCost()} after this call.
     *
     * @param matchings
     *         the <code>CostModelMatching</code>s to evaluate
     * @return the cost based on the set weight functions
     */
    private float cost(List<CostModelMatching<T>> matchings) {

        if (matchings.isEmpty()) {
            return 0;
        }

        CostModelMatching<T> first = matchings.get(0);
        int t1Size = first.m.findRoot().getTreeSize();
        int t2Size = first.n.findRoot().getTreeSize();

        matchings.forEach(m -> exactCost(m, matchings));

        float sumCost = matchings.stream().collect(summingDouble(CostModelMatching::getExactCost)).floatValue();

        return (1.0f / (t1Size + t2Size)) * sumCost;
    }

    /**
     * Sets the exact cost ({@link CostModelMatching#setExactCost(float)}) of the given <code>matching</code> based on
     * the given set of <code>matchings</code>.
     *
     * @param matching
     *         the <code>CostModelMatching</code> to compute the cost for
     * @param matchings
     *         the complete <code>CostModelMatching</code>s
     */
    private void exactCost(CostModelMatching<T> matching, List<CostModelMatching<T>> matchings) {

        if (matching.isNoMatch()) {
            matching.setExactCost(wn);
            return;
        }

        float cR = renamingCost(matching);
        float cA = ancestryViolationCost(matching, matchings);
        float cS = siblingGroupBreakupCost(matching, matchings);

        matching.setExactCost(cR + cA + cS);
    }

    /**
     * Returns the cost for renaming the node. The cost will be zero if the <code>Artifact</code>s match according to
     * {@link Artifact#matches(Artifact)}, otherwise it is determined by the set renaming weight function
     * {@link this#wr}.
     *
     * @param matching
     *         the <code>CostModelMatching</code> to compute the cost for
     * @return the exact renaming cost of the <code>matching</code>
     */
    private float renamingCost(CostModelMatching<T> matching) {
        if (matching.m.matches(matching.n)) {
            return 0;
        } else {
            return wr.weigh(matching);
        }
    }

    private float ancestryViolationCost(CostModelMatching<T> matching, List<CostModelMatching<T>> matchings) {
        int numM = numAncestryViolatingChildren(matching.m, matching.n, matchings);
        int numN = numAncestryViolatingChildren(matching.n, matching.m, matchings);

        return wa.weigh(matching, numM + numN);
    }

    private int numAncestryViolatingChildren(T m, T n, List<CostModelMatching<T>> matchings) {
        ArtifactList<T> mChildren = m.getChildren();
        ArtifactList<T> nChildren = n.getChildren();

        Predicate<T> filter = a -> a != null && !nChildren.contains(a);

        return (int) mChildren.stream().map(mChild -> image(mChild, matchings)).filter(filter).count();
    }

    private float siblingGroupBreakupCost(CostModelMatching<T> matching, List<CostModelMatching<T>> matchings) {
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
        return ws.weigh(matching, mCost + nCost);
    }

    private List<T> siblingInvariantSubset(T m, T n, List<CostModelMatching<T>> matchings) {
        List<T> mSiblings = siblings(m);
        List<T> nSiblings = siblings(n);

        return mSiblings.stream().filter(s -> nSiblings.contains(image(s, matchings))).collect(toList());
    }

    private List<T> siblingDivergentSubset(T m, T n, List<CostModelMatching<T>> matchings) {
        List<T> inv = siblingInvariantSubset(m, n, matchings);
        return siblings(m).stream().filter(sibling -> !inv.contains(sibling) && image(sibling, matchings) != null).collect(toList());
    }

    private Set<T> distinctSiblingFamilies(T m, List<CostModelMatching<T>> matchings) {
        Function<T, T> image = mChild -> image(mChild, matchings);
        Predicate<T> notNull = t -> t != null;
        Function<T, T> getParent = Artifact::getParent;

        return siblings(m).stream().map(image).filter(notNull).map(getParent).collect(toSet());
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
    private T image(T artifact, List<CostModelMatching<T>> matchings) {
        //TODO this is very inefficient...

        for (CostModelMatching<T> matching : matchings) {

            if (matching.contains(artifact)) {
                return matching.other(artifact);
            }
        }

        throw new NoSuchElementException("No matching containing " + artifact + " found.");
    }

    /**
     * Sets the bounds ({@link CostModelMatching#setCostBounds(Bounds)}) for the cost of the given <code>matching</code>
     * based on the given <code>currentMatchings</code>.
     *
     * @param matching
     *         the <code>CostModelMatching</code> whose costs are to be bounded
     * @param currentMatchings
     *         the current <code>CostModelMatching</code>s being considered
     */
    private void boundCost(CostModelMatching<T> matching, List<CostModelMatching<T>> currentMatchings) {

        if (matching.isNoMatch()) {
            matching.setBounds(wn, wn);
            return;
        }

        float cR = renamingCost(matching);
        Bounds cABounds = boundAncestryViolationCost(matching, currentMatchings);
        Bounds cSBounds = boundSiblingGroupBreakupCost(matching, currentMatchings);

        float lower = cR + cABounds.getLower() + cSBounds.getLower();
        float upper = cR + cABounds.getUpper() + cSBounds.getUpper();

        matching.setBounds(lower, upper);
    }

    private Bounds boundAncestryViolationCost(CostModelMatching<T> matching, List<CostModelMatching<T>> currentMatchings) {
        T m = matching.m;
        T n = matching.n;

        Stream<T> mLower = m.getChildren().stream().filter(mChild -> ancestryIndicator(mChild, n, currentMatchings, false));
        Stream<T> nLower = n.getChildren().stream().filter(nChild -> ancestryIndicator(nChild, m, currentMatchings, false));

        Stream<T> mUpper = m.getChildren().stream().filter(mChild -> ancestryIndicator(mChild, n, currentMatchings, true));
        Stream<T> nUpper = n.getChildren().stream().filter(nChild -> ancestryIndicator(nChild, m, currentMatchings, true));

        int lowerBound = (int) (mLower.count() + nLower.count());
        int upperBound = (int) (mUpper.count() + nUpper.count());

        return new Bounds(wa.weigh(matching, lowerBound), wa.weigh(matching, upperBound));
    }

    private boolean ancestryIndicator(T child, T n, List<CostModelMatching<T>> g, boolean upper) {
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

    private Bounds boundSiblingGroupBreakupCost(CostModelMatching<T> matching, List<CostModelMatching<T>> currentMatchings) {
        T m = matching.m;
        T n = matching.n;

        Bounds dMN = boundDistinctSibling(m, n, currentMatchings);
        Bounds dNM = boundDistinctSibling(n, m, currentMatchings);

        Bounds iMN = boundInvariantSiblings(m, n, currentMatchings);
        Bounds iNM = boundInvariantSiblings(n, m, currentMatchings);

        float mnLower = dMN.getLower() / (iMN.getUpper() * (dMN.getLower() + 1));
        float nmLower = dNM.getLower() / (iNM.getUpper() * (dNM.getLower() + 1));
        float lower = ws.weigh(matching, mnLower + nmLower);

        float mnUpper = dMN.getUpper() / iMN.getLower();
        float nmUpper = dNM.getUpper() / iNM.getLower();
        float upper = ws.weigh(matching, (mnUpper + nmUpper) / 2);

        return new Bounds(lower, upper);
    }

    private Bounds boundDistinctSibling(T m, T n, List<CostModelMatching<T>> currentMatchings) {
        long lower = otherSiblings(m).stream().filter(mSib -> distinctSiblingIndicator(mSib, n, currentMatchings, false)).count();
        long upper = otherSiblings(m).stream().filter(mSib -> distinctSiblingIndicator(mSib, n, currentMatchings, true)).count();

        return new Bounds(lower, upper);
    }

    private boolean distinctSiblingIndicator(T sibling, T n, List<CostModelMatching<T>> g, boolean upper) {
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

    private Bounds boundInvariantSiblings(T m, T n, List<CostModelMatching<T>> currentMatchings) {
        long lower = otherSiblings(m).stream().filter(mChild -> invariantSiblingIndicator(mChild, n, currentMatchings, false)).count();
        long upper = otherSiblings(m).stream().filter(mChild -> invariantSiblingIndicator(mChild, n, currentMatchings, true)).count();

        return new Bounds(lower + 1, upper + 1);
    }

    private boolean invariantSiblingIndicator(T child, T n, List<CostModelMatching<T>> g, boolean upper) {
        Predicate<CostModelMatching<T>> contains = match -> match.contains(child);
        Predicate<CostModelMatching<T>> indicator = match -> otherSiblings(n).contains(match.other(child));

        if (upper) {
            return g.stream().filter(contains).anyMatch(indicator);
        } else {
            return g.stream().filter(contains).allMatch(indicator);
        }
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
        setNoMatchWeight(context.wn);
        setRenamingWeight(context.wr);
        setAncestryViolationWeight(context.wa);
        setSiblingGroupBreakupWeight(context.ws);
        rng = context.seed.map(Random::new).orElse(new Random());

        LOG.finest("Matching " + left + " and " + right + " using the " + getClass().getSimpleName());

        float beta = 10; // TODO figure out good values for this (dependant on the size of the trees)

        List<CostModelMatching<T>> m = initialize(context.pAssign, left, right);
        ObjectiveValue mObjVal = objective(beta, m);

        List<CostModelMatching<T>> lowest = m;
        float lowestCost = mObjVal.matchingsCost;

        for (int i = 0; i < context.costModelIterations; i++) {
            List<CostModelMatching<T>> mHat = propose(context.pAssign, left, right, m);
            AcceptanceProbability mHatAccProb = acceptanceProb(beta, mObjVal.objValue, mHat);

            if (chance(mHatAccProb.acceptanceProbability)) {

                log(FINEST, mHat, () -> "Accepting the matchings.");

                m = mHat;
                mObjVal = mHatAccProb.mHatObjectiveValue;
            }

            if (mHatAccProb.mHatObjectiveValue.matchingsCost < lowestCost) {

                log(FINEST, mHat, () -> "New lowest cost matchings found.");

                lowest = mHat;
                lowestCost = mHatAccProb.mHatObjectiveValue.matchingsCost;
            }
        }

        LOG.log(FINEST, "Matching ended after " + context.costModelIterations + " iterations.");

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
    private Matchings<T> convert(List<CostModelMatching<T>> matchings) {
        return matchings.stream()
                        .filter(((Predicate<CostModelMatching<T>>) CostModelMatching::isNoMatch).negate())
                        .map(m -> new Matching<>(m.m, m.n, 0))
                        .collect(Matchings::new, Matchings::add, Matchings::addAll);
    }

    private List<CostModelMatching<T>> propose(float pAssign, T left, T right, List<CostModelMatching<T>> m) {
        Collections.sort(m, comparing(CostModelMatching::getExactCost));

        int j = rng.nextInt(m.size());
        List<CostModelMatching<T>> fixed = new ArrayList<>(m.subList(0, j));

        log(FINEST, m, () -> "Fixing the first " + j + " matchings from the last iteration. They are: " + fixed);

        List<CostModelMatching<T>> proposition = complete(fixed, pAssign, left, right);

        log(FINEST, proposition, () -> "Proposing matchings for the next iteration: " + proposition);

        return proposition;
    }

    private List<CostModelMatching<T>> initialize(float pAssign, T left, T right) {
        List<CostModelMatching<T>> initial = complete(new ArrayList<>(), pAssign, left, right);
        log(FINEST, initial, () -> "Initial set of matchings is: " + initial);

        return initial;
    }

    private List<CostModelMatching<T>> complete(List<CostModelMatching<T>> matchings, float pAssign, T left, T right) {
        List<CostModelMatching<T>> available = completeBipartiteGraph(left, right);
        Set<CostModelMatching<T>> fixed = new LinkedHashSet<>(matchings);

        fixed.forEach(m -> prune(m, available));

        while (!(fixed.size() == available.size())) {

            available.forEach(m -> boundCost(m, available));
            Collections.sort(available, comparing(CostModelMatching::getCostBounds, BY_LOWER_UPPER));

            CostModelMatching<T> matching;

            do {
                int i = 0;
                while (!chance(pAssign)) {
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

        return new ArrayList<>(fixed);
    }

    private void prune(CostModelMatching<T> matching, List<CostModelMatching<T>> g) {

        for (ListIterator<CostModelMatching<T>> it = g.listIterator(); it.hasNext();) {
            CostModelMatching<T> current = it.next();
            boolean neq = !matching.equals(current);

            if (neq && ((matching.m != null && matching.m == current.m) || (matching.n != null && matching.n == current.n))) {
                it.remove();
            }
        }
    }

    private boolean chance(double p) {
        return rng.nextDouble() < p;
    }

    private List<CostModelMatching<T>> completeBipartiteGraph(T left, T right) {
        List<T> leftNodes = Artifacts.bfs(left);
        List<T> rightNodes = Artifacts.bfs(right);

        // add the "No Match" node
        leftNodes.add(null);
        rightNodes.add(null);

        List<CostModelMatching<T>> bipartiteGraph = new LinkedList<>();

        for (T lNode : leftNodes) {
            for (T rNode : rightNodes) {

                if (lNode != null || rNode != null) {
                    bipartiteGraph.add(new CostModelMatching<>(lNode, rNode));
                }
            }
        }

        return bipartiteGraph;
    }

    private ObjectiveValue objective(float beta, List<CostModelMatching<T>> matchings) {
        float cost = cost(matchings);
        double objVal = Math.exp(-(beta * cost));

        log(FINEST, matchings, () -> "Cost of matchings is " + cost);
        log(FINEST, matchings, () -> "Objective function value is " + objVal);

        return new ObjectiveValue(objVal, cost);
    }

    private AcceptanceProbability acceptanceProb(float beta, double mObjectiveValue, List<CostModelMatching<T>> mHat) {
        ObjectiveValue mHatObjectiveValue = objective(beta, mHat);
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
    private void log(Level level, List<CostModelMatching<T>> matchings, Supplier<String> msg) {
        LOG.log(level, () -> String.format("%-10s%s", id(matchings), msg.get()));
    }
}
