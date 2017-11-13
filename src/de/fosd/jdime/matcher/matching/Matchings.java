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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.util.UnorderedTuple;

/**
 * A <code>Set</code> of <code>Matching</code>s. Adds methods to retrieve specific elements of the <code>Set</code>
 * by their matched <code>Artifact</code>s.
 *
 * @param <T>
 *         the type of the <code>Artifact</code>s
 */
public class Matchings<T extends Artifact<T>> extends HashSet<Matching<T>> {

    private static final long serialVersionUID = 1L;

    private final UnorderedTuple<T, T> tuple = UnorderedTuple.of(null, null);

    /**
     * Creates a new <code>Matchings</code> instance containing a single <code>Matching</code> that matches
     * <code>left</code> and <code>right</code> with the given <code>score</code>.
     *
     * @param left
     *         the left <code>Artifact</code>
     * @param right
     *         the right <code>Artifact</code>
     * @param score
     *         the score of the matching
     * @param <T>
     *         the type of the <code>Artifact</code>s
     * @return the new <code>Matchings</code> instance
     */
    public static <T extends Artifact<T>> Matchings<T> of(T left, T right, int score) {
        Matchings<T> result = new Matchings<>();
        result.add(new Matching<>(left, right, score));

        return result;
    }

    /**
     * Optionally returns the <code>Matching</code> matching the given <code>Artifact</code>s if there is such a
     * <code>Matching</code> in the <code>Set</code>. If <code>left</code> is a choice node then the first
     * matching of a variant of <code>left</code> and <code>right</code> is returned.
     *
     * @param artifacts
     *         the <code>Artifact</code>s whose <code>Matching</code> is to be returned
     * @return optionally the <code>Matching</code> matching the given <code>artifacts</code>
     */
    public Optional<Matching<T>> get(UnorderedTuple<T, T> artifacts) {
        T left = artifacts.getX();
        T right = artifacts.getY();

        if (left.isChoice()) {

            for (T variant : left.getVariants().values()) {
                Optional<Matching<T>> m = get(variant, right);

                if (m.isPresent()) {
                    Matching<T> matching = m.get();
                    Matching<T> variantMatching = new Matching<>(left, right, matching.getScore());
                    variantMatching.setAlgorithm(matching.getAlgorithm());

                    return Optional.of(variantMatching);
                }
            }

            return Optional.empty();
        }

        return stream().filter(matching -> matching.getMatchedArtifacts().equals(artifacts)).findFirst();
    }

    /**
     * Optionally returns the <code>Matching</code> matching the given <code>Artifact</code>s if there is such a
     * <code>Matching</code> in the <code>Set</code>. If <code>left</code> is a choice node then the first
     * matching of a variant of <code>left</code> and <code>right</code> is returned.
     *
     * @param left
     *         the left <code>Artifact</code> of the <code>Matching</code>
     * @param right
     *         the right <code>Artifact</code> of the <code>Matching</code>
     * @return optionally the <code>Matching</code> matching the given <code>artifacts</code>
     */
    public Optional<Matching<T>> get(T left, T right) {
        tuple.setX(left);
        tuple.setY(right);

        Optional<Matching<T>> matching = get(tuple);

        tuple.setX(null);
        tuple.setY(null);

        return matching;
    }

    /**
     * Returns the first matching having the given <code>artifact</code> as its left component,
     * if no such matching exists, returns the first having <code>artifact</code> as its right
     * component. If neither exits returns an empty <code>Optional</code>.
     *
     * @param artifact
     *         the <code>Artifact</code> to search for
     * @return optionally the first <code>Matching</code> containing <code>artifact</code>
     */
    public Optional<Matching<T>> getAny(T artifact) {
        Optional<Matching<T>> left = getLeft(artifact);

        if (left.isPresent()) {
            return left;
        } else {
            return getRight(artifact);
        }
    }

    /**
     * Returns the first matching having the given <code>artifact</code> as its left component. If no such matching
     * exits returns an empty <code>Optional</code>.
     *
     * @param artifact
     *         the <code>Artifact</code> to search for
     * @return optionally the first <code>Matching</code> containing <code>artifact</code>
     */
    public Optional<Matching<T>> getLeft(T artifact) {
        return get(artifact, Matching::getLeft);
    }

    /**
     * Returns the first matching having the given <code>artifact</code> as its right component. If no such matching
     * exits returns an empty <code>Optional</code>.
     *
     * @param artifact
     *         the <code>Artifact</code> to search for
     * @return optionally the first <code>Matching</code> containing <code>artifact</code>
     */
    public Optional<Matching<T>> getRight(T artifact) {
        return get(artifact, Matching::getRight);
    }

    /**
     * Returns the first matching whose result of the application of <code>getArtifact</code> is equal to
     * <code>artifact</code>.
     *
     * @param artifact
     *         the <code>Artifact</code> to search for
     * @return optionally the first <code>Matching</code> containing <code>artifact</code>
     */
    private Optional<Matching<T>> get(T artifact, Function<Matching<T>, T> getArtifact) {
        return stream().filter(m -> getArtifact.apply(m) == artifact).findFirst();
    }

    /**
     * Optionally returns the score of the <code>Matching</code> matching the given <code>Artifact</code>s if there
     * is such a <code>Matching</code> in the <code>Set</code>.
     *
     * @param artifacts
     *         the <code>Artifact</code>s whose <code>Matching</code>s score is to be returned
     * @return optionally the matching score for the given <code>artifacts</code>
     */
    public Optional<Integer> getScore(UnorderedTuple<T, T> artifacts) {
        Optional<Matching<T>> matching = get(artifacts);

        if (matching.isPresent()) {
            return Optional.of(matching.get().getScore());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Optionally returns the score of the <code>Matching</code> matching the given <code>Artifact</code>s if there
     * is such a <code>Matching</code> in the <code>Set</code>.
     *
     * @param left
     *         the left <code>Artifact</code> of the <code>Matching</code>
     * @param right
     *         the right <code>Artifact</code> of the <code>Matching</code>
     * @return optionally the matching score for the given <code>artifacts</code>
     */
    public Optional<Integer> getScore(T left, T right) {
        tuple.setX(left);
        tuple.setY(right);

        Optional<Integer> matching = getScore(tuple);

        tuple.setX(null);
        tuple.setY(null);

        return matching;
    }

    /**
     * Adds all <code>Matchings</code> contained in the given collection.
     *
     * @param matchings
     *         the <code>Matchings</code> to add
     */
    public void addAllMatchings(Collection<? extends Matchings<T>> matchings) {
        for (Matchings<T> matching : matchings) {
            addAll(matching);
        }
    }

    /**
     * Returns a <code>Matchings</code> instance containing for every matched Artifact in this <code>Matchings</code>
     * the <code>Matching</code> containing it that has the highest score.
     *
     * @return a new <code>Matchings</code> instance
     */
    public Matchings<T> optimized() {
        Map<Artifact<T>, List<Matching<T>>> matchings = new HashMap<>();

        forEach(matching -> {
            UnorderedTuple<T, T> artifacts = matching.getMatchedArtifacts();
            BiFunction<Artifact<T>, List<Matching<T>>, List<Matching<T>>> adder = (artifact, list) -> {

                if (list == null) {
                    list = new ArrayList<>();
                }

                list.add(matching);

                return list;
            };

            matchings.compute(artifacts.getX(), adder);
            matchings.compute(artifacts.getY(), adder);
        });

        Set<Matching<T>> filtered = new HashSet<>();
        Set<Artifact<T>> computed = new HashSet<>();
        Comparator<Matching<T>> comp = (o1, o2) -> Float.compare(o1.getPercentage(), o2.getPercentage());

        for (Map.Entry<Artifact<T>, List<Matching<T>>> entry : matchings.entrySet()) {
            (entry.getValue()).sort(comp.reversed());

            for (Matching<T> max : entry.getValue()) {

                if (max.getScore() == 0) {
                    break;
                }

                T left = max.getLeft();
                T right = max.getRight();

                if (!computed.contains(left) && !computed.contains(right)) {
                    filtered.add(max);
                    computed.add(left);
                    computed.add(right);
                    break;
                }
            }
        }

        Matchings<T> res = new Matchings<>();
        res.addAll(filtered);

        return res;
    }
}
