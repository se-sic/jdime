package de.fosd.jdime.matcher;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.UnorderedTuple;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

public class Matchings<T extends Artifact<T>> extends HashSet<NewMatching<T>> {

	public static <T extends Artifact<T>> Matchings<T> of(T left, T right, int score) {
		Matchings<T> result = new Matchings<>();
		result.add(new NewMatching<>(left, right, score));

		return result;
	}

	public Optional<NewMatching<T>> get(UnorderedTuple<T, T> artifacts) {
		return stream().filter(matching -> matching.getMatchedArtifacts().equals(artifacts)).findFirst();
	}

	public Optional<NewMatching<T>> get(T left, T rigt) {
		return get(UnorderedTuple.of(left, rigt));
	}

	public Optional<Integer> getScore(UnorderedTuple<T, T> artifacts) {
		Optional<NewMatching<T>> matching = get(artifacts);

		if (matching.isPresent()) {
			return Optional.of(matching.get().getScore());
		} else {
			return Optional.empty();
		}
	}

	public Optional<Integer> getScore(T left, T right) {
		return getScore(UnorderedTuple.of(left, right));
	}

	public void addAllMatchings(Collection<? extends Matchings<T>> matchings) {
		for (Matchings<T> matching : matchings) {
			addAll(matching);
		}
	}
}
