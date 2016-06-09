package de.fosd.jdime.matcher.cost_model;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Stream;

import de.fosd.jdime.common.Artifact;
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

        float weigh(CostModelMatching<T>  matching, int quantity);
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

        public CostModelMatching(T m) {
            this(m, null);
        }

        public CostModelMatching(T m, T n) {
            Objects.requireNonNull(m);

            this.m = m;
            this.n = n;
        }

        private boolean isNoMatch() {
            return n == null;
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

    private float weighRenaming(CostModelMatching<T> matching) {
        if (matching.isNoMatch() || matching.m.matches(matching.n)) {
            return 0;
        } else {
            return wr.weigh(matching);
        }
    }

    private Bounds boundAncestryViolationWeight(CostModelMatching<T> matching, Collection<CostModelMatching<T>> currentMatchings) {
        Function<Predicate<CostModelMatching<T>>, Boolean> upper = currentMatchings.stream()::anyMatch;
        Function<Predicate<CostModelMatching<T>>, Boolean> lower = currentMatchings.stream()::noneMatch;
        T m = matching.m;
        T n = matching.n;

        Stream<T> mUpper = m.getChildren().stream().filter(mAp -> ancestryIndicator(mAp, n, currentMatchings, true));
        Stream<T> nUpper = n.getChildren().stream().filter(nAp -> ancestryIndicator(nAp, m, currentMatchings, true));

        Stream<T> mLower = m.getChildren().stream().filter(mAp -> ancestryIndicator(mAp, n, currentMatchings, false));
        Stream<T> nLower = n.getChildren().stream().filter(nAp -> ancestryIndicator(nAp, m, currentMatchings, false));

        int upperBound = (int) (mUpper.count() + nUpper.count());
        int lowerBound = (int) (mLower.count() + nLower.count());

        return new Bounds(wa.weigh(matching, upperBound), wa.weigh(matching, lowerBound));
    }

    private boolean ancestryIndicator(T mAp, T n, Collection<CostModelMatching<T>> g, boolean upper) {

        if (upper) {
            return g.stream().anyMatch(match -> match.m == mAp && !(match.n == null || n.getChildren().contains(match.n)));
        } else {
            return g.stream().noneMatch(match -> match.m == mAp && (match.n == null || n.getChildren().contains(match.n)));
        }
    }

    @Override
    public Matchings<T> match(MergeContext context, T left, T right) {
        setNoMatchWeight(context.wn);
        setRenamingWeight(context.wr);
        setAncestryViolationWeight(context.wa);
        setSiblingGroupBreakupWeight(context.ws);

        return new Matchings<>();
    }
}
