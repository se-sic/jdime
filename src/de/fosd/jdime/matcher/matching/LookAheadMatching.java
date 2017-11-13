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
package de.fosd.jdime.matcher.matching;

import java.util.Objects;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.matcher.Matcher;
import de.fosd.jdime.util.UnorderedTuple;

/**
 * This <code>Matching</code> is used by the <code>Matcher</code> if lookahead is enabled. The concrete
 * <code>Matcher</code>s (like the <code>SimpleTreeMatcher</code>) always expect a <code>Matching</code> for the pair
 * of <code>Artifact</code>s that they passed to the {@link Matcher#match(MergeContext, Artifact, Artifact)}. However
 * when lookahead is enabled, we may instead find a <code>Matching</code> for children of the <code>Artifact</code>s that
 * were passed in. To ensure that the concrete <code>Matcher</code>s work correctly and that the <code>Matching</code>
 * between the children is added in {@link Matcher#storeMatchings(MergeContext, Matchings, Color)} this class overloads
 * some methods of the <code>Matching</code> base class.
 *
 * @param <T>
 *         the type of <code>Artifact</code>
 */
public class LookAheadMatching<T extends Artifact<T>> extends Matching<T> {

    private UnorderedTuple<T, T> lookAheadFrom;

    /**
     * Constructs a new <code>LookAheadMatching</code>. For the sake of the concrete <code>Matcher</code>s this class
     * 'pretends' to match the artifacts <code>lookAheadLeft</code> and <code>lookAheadRight</code>. These
     * <code>Artifact</code>s will be used for {@link #getMatchedArtifacts()}, {@link #equals(Object)} and
     * {@link #hashCode()}. The {@link #getLeft()} and {@link #getRight()} methods however will return the
     * <code>Artifact</code>s that were really matched.
     *
     * @param realMatching
     *         the <code>Artifact</code>s that were matched
     * @param lookAheadLeft
     *         the left <code>Artifact</code> for which lookahead matching was used
     * @param lookAheadRight
     *         the right <code>Artifact</code> for which lookahead matching was used
     */
    public LookAheadMatching(Matching<T> realMatching, T lookAheadLeft, T lookAheadRight) {
        super(realMatching.getLeft(), realMatching.getRight(), realMatching.getScore());
        this.lookAheadFrom = UnorderedTuple.of(lookAheadLeft, lookAheadRight);
        this.setAlgorithm(realMatching.getAlgorithm());
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
