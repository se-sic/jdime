package de.fosd.jdime.matcher.cost_model;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.Tuple;
import org.apache.commons.math3.distribution.IntegerDistribution;
import org.apache.commons.math3.distribution.PascalDistribution;
import org.apache.commons.math3.random.RandomAdaptor;
import org.apache.commons.math3.random.Well19937c;

/**
 * A container class for the parameters of the <code>CostModelMatcher</code>. Certain caches for speeding up successive
 * calls to {@link CostModelMatcher#cost(CMMatchings, CMParameters)} are also managed by this class.
 */
public final class CMParameters<T extends Artifact<T>> {

    /**
     * The cost of not matching an artifact.
     */
    float wn;

    /**
     * The function determining the cost of renaming an artifact. This cost is 0 if the artifacts match according to
     * {@link Artifact#matches}.
     */
    CostModelMatcher.SimpleWeightFunction<T> wr;

    /**
     * The function determining the cost of ancestry violations.
     */
    CostModelMatcher.WeightFunction<T> wa;

    /**
     * The function determining the cost of breaking up sibling groups.
     */
    CostModelMatcher.WeightFunction<T> ws;

    /**
     * The function determining the cost of an edge that violates ordering.
     */
    CostModelMatcher.SimpleWeightFunction<T> wo;

    /**
     * The PRNG used during the execution of the {@link CostModelMatcher#match(MergeContext, Artifact, Artifact)}
     * function.
     */
    RandomAdaptor rng;

    /**
     * A {@link PascalDistribution} from which indices into the list of available edges may be sampled. The probability
     * distribution is dictated by {@link this#pAssign}.
     */
    IntegerDistribution assignDist;

    /**
     * The chance that an edge is chosen when traversing the available edges in
     * {@link CostModelMatcher#complete(CMMatchings, CMParameters)}.
     */
    float pAssign;

    /**
     * Percentages (numbers from [0, 1]) indicating how many matchings from the previous iteration should be fixed
     * when proposing a new set of matchings.
     */
    float fixLower;
    float fixUpper;

    /**
     * Scaling factor for the difference in cost of two proposed sets of matchings. A higher value makes it less likely
     * that a set of matchings is accepted despite having a higher cost than the previous reference set.
     */
    float beta;

    /**
     * Whether the cost calculations (both exact and bounded) are executed for all edges in parallel.
     */
    boolean parallel;

    boolean fixRandomPercentage;

    /*
     * Caches valid for the entirety of the CostModelMatcher#match(MergeContext, Artifact, Artifact) function.
     */

    ConcurrentMap<Tuple<T, T>, Tuple<T, T>> lcaCache;
    ConcurrentMap<T, List<T>> siblingCache;
    ConcurrentMap<T, List<T>> otherSiblingsCache;

    /*
     * Caches valid during one run of the CostModelMatcher#cost(CMMatchings, CMParameters) function.
     */

    /**
     * Caches the <code>CostModelMatching</code>s containing an artifact.
     */
    ConcurrentMap<T, CostModelMatching<T>> exactContainsCache;

    /*
     * Caches valid during one run of the CostModelMatcher#boundCost(CMMatchings, CMParameters) function.
     */

    /**
     * Caches lists of <code>CostModelMatching</code>s containing an artifact.
     */
    ConcurrentMap<T, List<CostModelMatching<T>>> boundContainsCache;

    public CMParameters(MergeContext context) {
        setNoMatchWeight(context.wn);
        setRenamingWeight(context.wr);
        setAncestryViolationWeight(context.wa);
        setSiblingGroupBreakupWeight(context.ws);
        setOrderingWeight(context.wo);
        rng = new RandomAdaptor(context.seed.map(Well19937c::new).orElse(new Well19937c()));
        assignDist = new PascalDistribution(rng, 1, pAssign);
        setPAssign(context.pAssign);
        setFixLower(context.fixLower);
        setFixUpper(context.fixUpper);
        setBeta(30); // TODO figure out good values for this (dependant on the size of the trees)
        setParallel(context.cmMatcherParallel);
        setFixRandomPercentage(context.cmMatcherFixRandomPercentage);
        lcaCache = new ConcurrentHashMap<>();
        siblingCache = new ConcurrentHashMap<>();
        otherSiblingsCache = new ConcurrentHashMap<>();
        exactContainsCache = new ConcurrentHashMap<>();
        boundContainsCache = new ConcurrentHashMap<>();
    }

    public void setNoMatchWeight(float wn) {
        this.wn = wn;
    }

    public void setRenamingWeight(float wr) {
        setRenamingWeight(matching -> wr);
    }

    public void setRenamingWeight(CostModelMatcher.SimpleWeightFunction<T> wr) {
        this.wr = wr;
    }

    public void setAncestryViolationWeight(float wa) {
        setAncestryViolationWeight((matching, quantity) -> wa * quantity);
    }

    public void setAncestryViolationWeight(CostModelMatcher.WeightFunction<T> wa) {
        this.wa = wa;
    }

    public void setSiblingGroupBreakupWeight(float ws) {
        setSiblingGroupBreakupWeight((matching, quantity) -> ws * quantity);
    }

    public void setSiblingGroupBreakupWeight(CostModelMatcher.WeightFunction<T> ws) {
        this.ws = ws;
    }

    public void setOrderingWeight(float wo) {
        setOrderingWeight(matching -> wo);
    }

    public void setOrderingWeight(CostModelMatcher.SimpleWeightFunction<T> wo) {
        this.wo = wo;
    }

    public void setPAssign(float pAssign) {
        this.pAssign = pAssign;
    }

    public void setFixLower(float fixLower) {
        checkInRange(0, 1, fixLower);
        this.fixLower = fixLower;
    }

    public void setFixUpper(float fixUpper) {
        checkInRange(0, 1, fixUpper);
        this.fixUpper = fixUpper;
    }

    private void checkInRange(float lower, float upper, float val) {
        if (!(lower <= val && val <= upper)) {
            throw new IllegalArgumentException(String.format("%s is not in the range [%s, %s]", val, lower, upper));
        }
    }

    public void setBeta(float beta) {
        this.beta = beta;
    }

    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    public void setFixRandomPercentage(boolean fixRandomPercentage) {
        this.fixRandomPercentage = fixRandomPercentage;
    }

    public void clearExactCaches() {
        exactContainsCache.clear();
    }

    public void clearBoundCaches() {
        boundContainsCache.clear();
    }
}
