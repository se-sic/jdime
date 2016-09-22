package de.fosd.jdime.matcher.cost_model;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.matcher.Matcher;
import de.fosd.jdime.matcher.matching.Color;

/**
 * The possible modes determining how the <code>CostModelMatcher</code> is used in the matching process.
 */
public enum CMMode {
    /**
     * The <code>CostModelMatcher</code> is not used.
     */
    OFF,

    /**
     * The <code>CostModelMatcher</code> will be the only matcher used to produce matchings in
     * {@link Matcher#match(MergeContext, Artifact, Artifact, Color)}.
     */
    REPLACEMENT,

    /**
     * After the matchings were calculated in {@link Matcher#match(MergeContext, Artifact, Artifact, Color)} they are
     * fixed and a matching between the unmatched artifacts is calculated using the <code>CostModelMatcher</code>.
     */
    POST_PROCESSOR,

    /**
     * Whenever a subtree could not be matched fully during the execution of
     * {@link Matcher#match(MergeContext, Artifact, Artifact)}, the <code>CostModelMatcher</code> is used to attempt
     * to refine the matchings.
     */
    INTEGRATED
}
