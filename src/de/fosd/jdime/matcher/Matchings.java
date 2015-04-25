package de.fosd.jdime.matcher;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.UnorderedTuple;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Matchings<T extends Artifact<T>> {

	private Map<UnorderedTuple<T, T>, NewMatching<T>> matchings;

	public void add(NewMatching<T> matching) {
		getMatchings().put(matching.getMatchedArtifacts(), matching);
	}

	public void addAll(Collection<? extends NewMatching<T>> matchings) {
		for (NewMatching<T> matching : matchings) {
			add(matching);
		}
	}

	public NewMatching<T> get(UnorderedTuple<T, T> artifacts) {
		return getMatchings().get(artifacts);
	}

	public NewMatching<T> get(T left, T rigt) {
		return getMatchings().get(UnorderedTuple.of(left, rigt));
	}

	public int getScore(UnorderedTuple<T, T> artifacts) {
		if (!contains(artifacts)) {
			return -1;
		} else {
			return get(artifacts).getScore();
		}
	}

	public int getScore(T left, T right) {
		return getScore(UnorderedTuple.of(left, right));
	}

	public boolean contains(UnorderedTuple<T, T> artifacts) {
		return getMatchings().containsKey(artifacts);
	}

	public boolean contains(T left, T right) {
		return getMatchings().containsKey(UnorderedTuple.of(left, right));
	}

	public Map<UnorderedTuple<T, T>, NewMatching<T>> getMatchings() {

		if (matchings == null) {
			matchings = new HashMap<>();
		}

		return matchings;
	}
}
