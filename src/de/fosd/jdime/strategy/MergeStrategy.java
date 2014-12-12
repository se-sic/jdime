/*******************************************************************************
 * Copyright (C) 2013, 2014 Olaf Lessenich.
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
 *     Olaf Lessenich - initial API and implementation
 *******************************************************************************/
package de.fosd.jdime.strategy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.merge.MergeInterface;

/**
 * @author Olaf Lessenich
 *
 * @param <T>
 *            type of artifact
 */
public abstract class MergeStrategy<T extends Artifact<T>> implements
		MergeInterface<T>, StatsInterface<T>, DumpInterface<T> {

	private static final Map<String, MergeStrategy<?>> strategyMap;

	static {
		Map<String, MergeStrategy<?>> entries = new HashMap<>();
		LinebasedStrategy lineBased = new LinebasedStrategy();
		StructuredStrategy structured = new StructuredStrategy();
		CombinedStrategy combined = new CombinedStrategy();

		entries.put("linebased", lineBased);
		entries.put("unstructured", lineBased);
		entries.put("structured", structured);
		entries.put("combined", combined);
		entries.put("autotuning", combined);
		
		strategyMap = Collections.unmodifiableMap(entries);
	}

	/**
	 * Returns a set containing the names of available strategies.
	 *
	 * @return names of available strategies
	 */
	public static Set<String> listStrategies() {
		return strategyMap.keySet();
	}

	/**
	 * Parses a String and returns a strategy. Null is returned if no
	 * appropriate Tool is found.
	 *
	 * @param str
	 *            name of the merge tool
	 * @return MergeStrategy merge strategy
	 */
	public static MergeStrategy<?> parse(final String str) {
		assert str != null : "Merge strategy may not be null!";

		String input = str.toLowerCase();

		if (strategyMap.containsKey(input)) {
			return strategyMap.get(input);
		} else {
			throw new StrategyNotFoundException("Strategy not found: " + str);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public abstract String toString();
}
