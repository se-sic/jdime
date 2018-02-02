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
package de.fosd.jdime.matcher.cost_model;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.util.Tuple;
import org.apache.commons.math3.distribution.IntegerDistribution;
import org.apache.commons.math3.distribution.PascalDistribution;
import org.apache.commons.math3.random.RandomAdaptor;
import org.apache.commons.math3.random.Well19937c;

import static de.fosd.jdime.stats.KeyEnums.Type.CLASS;
import static de.fosd.jdime.stats.KeyEnums.Type.METHOD;

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
     * Caches the <code>CMMatching</code>s containing an artifact.
     */
    ConcurrentMap<T, CMMatching<T>> exactContainsCache;

    /*
     * Caches valid during one run of the CostModelMatcher#boundCost(CMMatchings, CMParameters) function.
     */

    /**
     * Caches lists of <code>CMMatching</code>s containing an artifact.
     */
    ConcurrentMap<T, List<CMMatching<T>>> boundContainsCache;

    /**
     * Constructs a new <code>CMParameters</code> configured from the given <code>MergeContext</code>.
     *
     * @param context
     *         the <code>MergeContext</code> to use
     */
    public CMParameters(MergeContext context) {
        setNoMatchWeight(context.getWn());
        setRenamingWeight(context.getWr());
        setAncestryViolationWeight(context.getWa());
        setSiblingGroupBreakupWeight(context.getWs());
        setOrderingWeight(context.getWo());
        rng = new RandomAdaptor(context.getSeed().map(Well19937c::new).orElse(new Well19937c()));
        assignDist = new PascalDistribution(rng, 1, context.getpAssign());
        setPAssign(context.getpAssign());
        setFixLower(context.getFixLower());
        setFixUpper(context.getFixUpper());
        setBeta(30);
        setParallel(context.isCmMatcherParallel());
        setFixRandomPercentage(context.isCmMatcherFixRandomPercentage());
        lcaCache = new ConcurrentHashMap<>();
        siblingCache = new ConcurrentHashMap<>();
        otherSiblingsCache = new ConcurrentHashMap<>();
        exactContainsCache = new ConcurrentHashMap<>();
        boundContainsCache = new ConcurrentHashMap<>();
    }

    /**
     * Sets the no-match weighting function to return the given <code>wn</code>.
     *
     * @param wn
     *         the no-match weight
     */
    public void setNoMatchWeight(float wn) {
        this.wn = wn;
    }

    /**
     * Sets the renaming weighting function to return the given <code>wr</code>.
     *
     * @param wr
     *         the renaming weight
     */
    public void setRenamingWeight(float wr) {
        setRenamingWeight(matching -> {
            float ease = 0.1f;

            if (matching.m.getType() == METHOD && matching.n.getType() == METHOD) {
                return ease * wr;
            }

            if (matching.m.getType() == CLASS && matching.n.getType() == CLASS) {
                return ease * wr;
            }

            return wr;
        });
    }

    /**
     * Sets the renaming weighting function.
     *
     * @param wr
     *         the new renaming weighting function
     */
    public void setRenamingWeight(CostModelMatcher.SimpleWeightFunction<T> wr) {
        this.wr = wr;
    }

    /**
     * Sets the ancestry violation weighting function to multiply the cost with <code>wn</code>.
     *
     * @param wa
     *         the new ancestry violation weight
     */
    public void setAncestryViolationWeight(float wa) {
        setAncestryViolationWeight((matching, quantity) -> wa * quantity);
    }

    /**
     * Sets the ancestry violation weighting function.
     *
     * @param wa
     *         the new ancestry violation weighting function
     */
    public void setAncestryViolationWeight(CostModelMatcher.WeightFunction<T> wa) {
        this.wa = wa;
    }

    /**
     * Sets the sibling group breakup weighting function to multiply the cost with <code>ws</code>.
     *
     * @param ws
     *         the new sibling group breakup weight
     */
    public void setSiblingGroupBreakupWeight(float ws) {
        setSiblingGroupBreakupWeight((matching, quantity) -> ws * quantity);
    }

    /**
     * Sets the sibling group breakup weighting function.
     *
     * @param ws
     *         the new sibling group breakup weighting function
     */
    public void setSiblingGroupBreakupWeight(CostModelMatcher.WeightFunction<T> ws) {
        this.ws = ws;
    }

    /**
     * Sets the ordering violation weighting function to return <code>wo</code>.
     *
     * @param wo
     *         the new ordering violation weight
     */
    public void setOrderingWeight(float wo) {
        setOrderingWeight(matching -> wo);
    }

    /**
     * Sets the ordering violation weighting function.
     *
     * @param wo
     *         the new ordering violation weighting function
     */
    public void setOrderingWeight(CostModelMatcher.SimpleWeightFunction<T> wo) {
        this.wo = wo;
    }

    /**
     * Sets the fixing probability used in {@link CostModelMatcher#complete(CMMatchings, CMParameters)}.
     *
     * @param pAssign
     *         the new fixing probability
     */
    public void setPAssign(float pAssign) {
        this.pAssign = pAssign;
    }

    /**
     * Sets the lower bound for the number of matchings being fixed for the next iteration.
     *
     * @param fixLower
     *         the lower bound, must be from [0, 1]
     */
    public void setFixLower(float fixLower) {
        checkInRange(0, 1, fixLower);
        this.fixLower = fixLower;
    }

    /**
     * Sets the upper bound for the number of matchings being fixed for the next iteration.
     *
     * @param fixUpper
     *         the upper bound, must be from [0, 1]
     */
    public void setFixUpper(float fixUpper) {
        checkInRange(0, 1, fixUpper);
        this.fixUpper = fixUpper;
    }

    /**
     * Checks whether <code>val</code> is in the range [<code>lower</code>, <code>upper</code>].
     *
     * @param lower
     *         the lower bound
     * @param upper
     *         the upper bound
     * @param val
     *         the value to check
     * @throws IllegalArgumentException
     *         if <code>val</code> is not from [<code>lower</code>, <code>upper</code>]
     */
    private void checkInRange(float lower, float upper, float val) {
        if (!(lower <= val && val <= upper)) {
            throw new IllegalArgumentException(String.format("%s is not in the range [%s, %s]", val, lower, upper));
        }
    }

    /**
     * Sets the cost scaling factor beta used in the objective function.
     *
     * @param beta
     *         the new beta
     */
    public void setBeta(float beta) {
        this.beta = beta;
    }

    /**
     * Sets whether cost calculations should be performed in parallel for all matchings.
     *
     * @param parallel
     *         whether cost calculations should be parallel
     */
    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    /**
     * Sets whether to fix a random percentage of matchings instead of the first (randomly many) matchings.
     *
     * @param fixRandomPercentage
     *         whether to fix a random percentage of matchings
     * @see #setFixLower(float)
     * @see #setFixUpper(float)
     */
    public void setFixRandomPercentage(boolean fixRandomPercentage) {
        this.fixRandomPercentage = fixRandomPercentage;
    }

    /**
     * Clears the caches that are only valid for one exact cost calculation.
     */
    public void clearExactCaches() {
        exactContainsCache.clear();
    }

    /**
     * Clears the caches that are only valid for one bounded cost calculation.
     */
    public void clearBoundCaches() {
        boundContainsCache.clear();
    }
}
