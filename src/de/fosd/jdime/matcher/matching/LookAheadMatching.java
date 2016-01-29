package de.fosd.jdime.matcher.matching;

import java.util.Objects;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.UnorderedTuple;

public class LookAheadMatching<T extends Artifact<T>> extends Matching<T> {

    private UnorderedTuple<T, T> lookAheadFrom;

    public LookAheadMatching(Matching<T> realMatching, T lookAheadLeft, T lookAheadRight) {
        super(realMatching.getLeft(), realMatching.getRight(), realMatching.getScore());
        this.lookAheadFrom = UnorderedTuple.of(lookAheadLeft, lookAheadRight);
    }

    @Override
    public UnorderedTuple<T, T> getMatchedArtifacts() {
        return lookAheadFrom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LookAheadMatching<?> that = (LookAheadMatching<?>) o;
        return Objects.equals(lookAheadFrom, that.lookAheadFrom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lookAheadFrom);
    }
}
