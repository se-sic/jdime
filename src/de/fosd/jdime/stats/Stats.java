/*******************************************************************************
 * Copyright (C) 2013-2015 Olaf Lessenich.
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
 *******************************************************************************/
package de.fosd.jdime.stats;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.fosd.jdime.common.operations.Operation;

/**
 * @author Olaf Lessenich
 *
 */
public class Stats {

	private List<MergeTripleStats> scenariostats;

	/**
	 * Number of conflicts.
	 */
	private int conflicts = 0;

	/**
	 * Runtime.
	 */
	private long runtime = 0;

	/**
	 * Map of elements.
	 */
	private HashMap<String, StatsElement> elements;

	/**
	 * Map of operations.
	 */
	private HashMap<String, Integer> operations;

	private ASTStats astStats;
	private ASTStats leftStats;
	private ASTStats rightStats;

	/**
	 * Creates a new Stats instance from a list of keys.
	 *
	 * @param scenariostats
	 *            list of scenario stats
	 * @param keys
	 *            List of keys
	 */
	public Stats(final List<MergeTripleStats> scenariostats,
			final List<String> keys) {
		assert (keys != null);
		assert (!keys.isEmpty());

		// If necessary, initialize lists and maps
		if (this.scenariostats == null) {
			this.scenariostats = new LinkedList<>();
		}

		if (scenariostats != null) {
			this.scenariostats.addAll(scenariostats);
		}

		if (elements == null) {
			elements = new HashMap<>();
		}

		if (operations == null) {
			operations = new HashMap<>();
		}

		for (String key : keys) {
			elements.put(key, new StatsElement());
		}
	}

	/**
	 * Creates a new Stats instance from an array of keys.
	 *
	 * @param keys
	 *            array of keys
	 */
	public Stats(final String[] keys) {
		this(null, Arrays.asList(keys));
	}

	/**
	 * Returns the number of conflicts.
	 *
	 * @return number of conflicts
	 */
	public final int getConflicts() {
		return conflicts;
	}

	/**
	 * Sets the number of conflicts.
	 *
	 * @param conflicts
	 *            number of conflicts to set
	 */
	public final void setConflicts(final int conflicts) {
		this.conflicts = conflicts;
	}

	/**
	 * Returns the runtime.
	 *
	 * @return runtime
	 */
	public final long getRuntime() {
		return runtime;
	}

	/**
	 * Sets the runtime.
	 *
	 * @param runtime
	 *            runtime
	 */
	public final void setRuntime(final long runtime) {
		this.runtime = runtime;
	}

	/**
	 * Increases the number of conflicts.
	 *
	 * @param conflicts
	 *            number of conflicts to add
	 */
	public final void addConflicts(final int conflicts) {
		this.conflicts += conflicts;
	}

	/**
	 * Increase the runtime statistics.
	 *
	 * @param runtime
	 *            runtime in ms
	 */
	public final void increaseRuntime(final long runtime) {
		this.runtime += runtime;
	}

	/**
	 * Adds the statistical data from another stats object.
	 *
	 * @param other
	 *            stats data to add
	 */
	public final void add(final Stats other) {
		assert (other != null);
		assert (other.elements.size() > 0);
		for (String otherKey : other.getKeys()) {
			StatsElement otherElement = other.getElement(otherKey);
			if (elements.containsKey(otherKey)) {
				StatsElement updated = elements.get(otherKey);
				updated.addStatsElement(otherElement);
				elements.put(otherKey, updated);
			} else {
				elements.put(otherKey, otherElement);
			}
		}

		this.conflicts += other.conflicts;
		this.runtime += other.runtime;
	}

	/**
	 * Returns a set of keys.
	 *
	 * @return set of keys
	 */
	public final Set<String> getKeys() {
		assert (elements != null);
		return elements.keySet();
	}

	/**
	 * Returns a StatsElement.
	 *
	 * @param key
	 *            element that should be returned
	 * @return element
	 */
	public final StatsElement getElement(final String key) {
		assert (elements != null);
		assert (elements.containsKey(key));
		return elements.get(key);
	}

	/**
	 * Returns a set of operations.
	 *
	 * @return set of operations
	 */
	public final Set<String> getOperations() {
		assert (operations != null);
		return operations.keySet();
	}

	/**
	 * Increases the counter of an operation.
	 *
	 * @param op
	 *            operation
	 */
	public final void incrementOperation(final Operation<?> op) {
		assert (operations != null);
		String opName = op.getName();

		if (!operations.containsKey(opName)) {
			operations.put(opName, 0);
		}

		assert (operations.containsKey(opName));

		operations.put(opName, operations.get(opName) + 1);
	}

	/**
	 * Returns how many times an operation was applied..
	 *
	 * @param opName
	 *            name of the operation
	 * @return number of times an operation was applied
	 */
	public final int getOperation(final String opName) {
		assert (operations != null);
		Integer op = operations.get(opName);
		return op == null ? 0 : op;
	}

	/**
	 * @return the scenariostats
	 */
	public final List<MergeTripleStats> getScenariostats() {
		return scenariostats;
	}

	/**
	 * Add a triple statistic.
	 *
	 * @param tripleStats
	 *            triple statistics
	 */
	public final void addScenarioStats(final MergeTripleStats tripleStats) {
		this.scenariostats.add(tripleStats);
	}

	/**
	 * Reset the triple statistics.
	 */
	public final void resetScenarioStats() {
		scenariostats = new LinkedList<>();
	}

	/**
	 * @return the astStats
	 */
	public final ASTStats getAstStats() {
		return astStats;
	}
	
	public final ASTStats getLeftStats() {
		return leftStats;
	}
	
	public final ASTStats getRightStats() {
		return rightStats;
	}

	public final void addASTStats(ASTStats other) {
		if (astStats == null) {
			astStats = other;
		} else {
			astStats.add(other);
		}
	}
	
	public final void addLeftStats(ASTStats other) {
		if (leftStats == null) {
			leftStats = other;
		} else {
			leftStats.add(other);
		}
	}
	
	public final void addRightStats(ASTStats other) {
		if (rightStats == null) {
			rightStats = other;
		} else {
			rightStats.add(other);
		}
	}
}
