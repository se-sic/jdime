package de.fosd.jdime.matcher.cost_model;

import java.util.List;
import java.util.Random;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;

/**
 * A container class for the parameters of the <code>CostModelMatcher</code>. Certain caches for speeding up successive
 * calls to {@link CostModelMatcher#cost(List, Artifact, Artifact, CostModelParameters)} are also managed by this class.
 */
public final class CostModelParameters<T extends Artifact<T>> {

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
    Random rng;

    /**
     * The chance that an edge is chosen when traversing the available edges in
     * {@link CostModelMatcher#complete(List, Artifact, Artifact, CostModelParameters)}.
     */
    float pAssign;

    /**
     * Scaling factor for the difference in cost of two proposed sets of matchings. A higher value makes it less likely
     * that a set of matchings is accepted despite having a higher cost than the previous reference set.
     */
    float beta;

    public CostModelParameters(MergeContext context) {
        setNoMatchWeight(context.wn);
        setRenamingWeight(context.wr);
        setAncestryViolationWeight(context.wa);
        setSiblingGroupBreakupWeight(context.ws);
        rng = context.seed.map(Random::new).orElse(new Random());
        setPAssign(context.pAssign);
        setBeta(30); // TODO figure out good values for this (dependant on the size of the trees)
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

    public void setBeta(float beta) {
        this.beta = beta;
    }

    /**
     * Returns <code>true</code> with a probability of <code>p</code>.
     *
     * @param p
     *         a number between 0.0 and 1.0
     * @return true or false depending on the next double returned by the PRNG
     */
    boolean chance(double p) {
        return rng.nextDouble() < p;
    }
}
