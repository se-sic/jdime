package de.fosd.jdime.matcher.cost_model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.ArtifactList;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.matcher.MatcherInterface;
import de.fosd.jdime.matcher.matching.Matchings;

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

    private static final class Bounds {
        private float lower;
        private float upper;

        public Bounds(float lower, float upper) {
            this.lower = lower;
            this.upper = upper;
        }
    }

    private static final class CostModelMatching<T extends Artifact<T>> {
        public final T m;
        public final T n;

        public CostModelMatching(T m, T n) {
            Objects.requireNonNull(m);

            this.m = m;
            this.n = n;
        }

        public boolean isNoMatch() {
            return m == null || n == null;
        }

        public boolean contains(T t) {
            return m == t || n == t;
        }

        public T other(T t) {
            if (m == t) {
                return n;
            } else if (n == t) {
                return m;
            } else {
                throw new IllegalArgumentException(t + " is not part of " + this);
            }
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

    public float cost(Matchings<T> matchings) {
        return cost(matchings.stream().map(m -> new CostModelMatching<>(m.getLeft(), m.getRight())).collect(Collectors.toList()));
    }

    private float cost(List<CostModelMatching<T>> matchings) {

        if (matchings.isEmpty()) {
            return 0;
        }

        CostModelMatching<T> first = matchings.get(0);
        int t1Size = first.m.findRoot().getTreeSize();
        int t2Size = first.n.findRoot().getTreeSize();

        return (1.0f / (t1Size + t2Size)) * matchings.stream().collect(Collectors.summingDouble(m -> exactCost(m, matchings))).floatValue();
    }

    private float exactCost(CostModelMatching<T> matching, List<CostModelMatching<T>> matchings) {

        if (matching.isNoMatch()) {
            return wn;
        }

        return renamingCost(matching) + ancestryViolationCost(matching, matchings) + siblingGroupBreakupCost(matching, matchings);
    }

    private float ancestryViolationCost(CostModelMatching<T> matching, List<CostModelMatching<T>> matchings) {
        return wa.weigh(matching, numAncestryViolatingChildren(matching, matchings) + numAncestryViolatingChildren(matching, matchings));
    }

    private int numAncestryViolatingChildren(CostModelMatching<T> matching, List<CostModelMatching<T>> matchings) {
        ArtifactList<T> mChildren = matching.m.getChildren();
        ArtifactList<T> nChildren = matching.n.getChildren();

        return (int) mChildren.stream().map(mAp -> image(mAp, matchings)).filter(nChildren::contains).count();
    }

    private float siblingGroupBreakupCost(CostModelMatching<T> matching, List<CostModelMatching<T>> matchings) {
        List<T> dMm, iMm, fMm;
        List<T> dMn, iMn, fMn;

        dMm = siblingDivergentSubset(matching.m, matching.n, matchings);
        iMm = siblingInvariantSubset(matching.m, matching.n, matchings);
        fMm = distinctSiblingFamilies(matching.m, matchings);

        dMn = siblingDivergentSubset(matching.n, matching.m, matchings);
        iMn = siblingInvariantSubset(matching.n, matching.m, matchings);
        fMn = distinctSiblingFamilies(matching.n, matchings);

        float mCost = (float) dMm.size() / (iMm.size() + fMm.size());
        float nCost = (float) dMn.size() / (iMn.size() + fMn.size());
        return ws.weigh(matching, mCost + nCost);
    }

    private List<T> siblingInvariantSubset(T m, T n, List<CostModelMatching<T>> matchings) {
        return siblings(m).stream().filter(mAp -> siblings(n).contains(image(mAp, matchings))).collect(Collectors.toList());
    }

    private List<T> siblingDivergentSubset(T m, T n, List<CostModelMatching<T>> matchings) {
        List<T> inv = siblingInvariantSubset(m, n, matchings);
        return siblings(m).stream().filter(mAp -> !inv.contains(mAp) && image(mAp, matchings) != null).collect(Collectors.toList());
    }

    private List<T> distinctSiblingFamilies(T m, List<CostModelMatching<T>> matchings) {
        return siblings(m).stream().map(mAp -> image(mAp, matchings).getParent()).collect(Collectors.toList());
    }

    private T image(T m, List<CostModelMatching<T>> matchings) {
        //TODO this is very inefficient...
        return matchings.stream().filter(ma -> ma.contains(m)).map(ma -> ma.other(m)).findFirst().get();
    }

    private Bounds boundCost(CostModelMatching<T> matching, List<CostModelMatching<T>> currentMatchings) {
        float cR = renamingCost(matching);
        Bounds cABounds = boundAncestryViolationCost(matching, currentMatchings);
        Bounds cSBounds = boundSiblingGroupBreakupCost(matching, currentMatchings);

        return new Bounds(cR + cABounds.lower + cSBounds.lower, cR + cABounds.upper + cSBounds.upper);
    }

    private float renamingCost(CostModelMatching<T> matching) {
        if (matching.isNoMatch() || matching.m.matches(matching.n)) {
            return 0;
        } else {
            return wr.weigh(matching);
        }
    }

    private Bounds boundAncestryViolationCost(CostModelMatching<T> matching, List<CostModelMatching<T>> currentMatchings) {
        T m = matching.m;
        T n = matching.n;

        Stream<T> mLower = m.getChildren().stream().filter(mAp -> ancestryIndicator(mAp, n, currentMatchings, false));
        Stream<T> nLower = n.getChildren().stream().filter(nAp -> ancestryIndicator(nAp, m, currentMatchings, false));

        Stream<T> mUpper = m.getChildren().stream().filter(mAp -> ancestryIndicator(mAp, n, currentMatchings, true));
        Stream<T> nUpper = n.getChildren().stream().filter(nAp -> ancestryIndicator(nAp, m, currentMatchings, true));

        int lowerBound = (int) (mLower.count() + nLower.count());
        int upperBound = (int) (mUpper.count() + nUpper.count());

        return new Bounds(wa.weigh(matching, lowerBound), wa.weigh(matching, upperBound));
    }

    private boolean ancestryIndicator(T mAp, T n, List<CostModelMatching<T>> g, boolean upper) {

        if (upper) {
            return g.stream().anyMatch(match -> match.m == mAp && !(match.n == null || n.getChildren().contains(match.n)));
        } else {
            return g.stream().noneMatch(match -> match.m == mAp && (match.n == null || n.getChildren().contains(match.n)));
        }
    }

    private Bounds boundSiblingGroupBreakupCost(CostModelMatching<T> matching, List<CostModelMatching<T>> currentMatchings) {
        T m = matching.m;
        T n = matching.n;

        Bounds dMN = boundDistinctSiblingGroups(m, n, currentMatchings);
        Bounds dNM = boundDistinctSiblingGroups(n, m, currentMatchings);

        Bounds iMN = boundInvariantSiblings(m, n, currentMatchings);
        Bounds iNM = boundInvariantSiblings(n, m, currentMatchings);

        float lower = ws.weigh(matching, ((dMN.lower / (iMN.upper * (dMN.lower + 1))) + (dNM.lower / (iNM.upper * (dNM.lower + 1)))));
        float upper = ws.weigh(matching, (dMN.upper / iMN.lower) + (dNM.upper / iNM.lower)) / 2;

        return new Bounds(lower, upper);
    }

    private Bounds boundDistinctSiblingGroups(T m, T n, List<CostModelMatching<T>> currentMatchings) {
        long lower = m.getChildren().stream().filter(mAp -> distinctSiblingIndicator(mAp, n, currentMatchings, false)).count();
        long upper = m.getChildren().stream().filter(mAp -> distinctSiblingIndicator(mAp, n, currentMatchings, true)).count();

        return new Bounds(lower, upper);
    }

    private boolean distinctSiblingIndicator(T mAp, T n, List<CostModelMatching<T>> g, boolean upper) {

        if (upper) {
            return g.stream().anyMatch(match -> match.m == mAp && !(match.n == null || otherSiblings(n).contains(match.n)));
        } else {
            return g.stream().noneMatch(match -> match.m == mAp && (match.n == null || otherSiblings(n).contains(match.n)));
        }
    }

    private Bounds boundInvariantSiblings(T m, T n, List<CostModelMatching<T>> currentMatchings) {
        long lower = m.getChildren().stream().filter(mAp -> invariantSiblingIndicator(mAp, n, currentMatchings, false)).count();
        long upper = m.getChildren().stream().filter(mAp -> invariantSiblingIndicator(mAp, n, currentMatchings, true)).count();

        return new Bounds(lower + 1, upper + 1);
    }

    private boolean invariantSiblingIndicator(T mAp, T n, List<CostModelMatching<T>> g, boolean upper) {

        if (upper) {
            return g.stream().anyMatch(match -> match.m == mAp && otherSiblings(n).contains(match.n));
        } else {
            return g.stream().allMatch(match -> (match.m != mAp) || otherSiblings(n).contains(match.n));
        }
    }

    private List<T> siblings(T artifact) {
        T parent = artifact.getParent();

        if (parent == null) {
            return Collections.emptyList();
        } else {
            List<T> res = new ArrayList<T>(parent.getChildren());
            res.remove(artifact);

            return res;
        }
    }

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

        return new Matchings<>();
    }

    private float objective(float beta, List<CostModelMatching<T>> matchings) {
        return (float) Math.exp((- beta) * cost(matchings));
    }

    private float acceptanceProb(float beta, List<CostModelMatching<T>> mOld, List<CostModelMatching<T>> mNew) {
        return Math.min(1, objective(beta, mNew) / objective(beta, mOld));
    }
}
