package de.fosd.jdime.matcher.cost_model;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.matcher.MatcherInterface;
import de.fosd.jdime.matcher.matching.Matchings;

public class CostModelMatcher<T extends Artifact<T>> implements MatcherInterface<T> {

    @Override
    public Matchings<T> match(MergeContext context, T left, T right) {
        return new Matchings<>();
    }
}
