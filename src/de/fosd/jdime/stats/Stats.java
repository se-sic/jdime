/*******************************************************************************
 * Copyright (c) 2013 Olaf Lessenich.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Olaf Lessenich - initial API and implementation
 ******************************************************************************/
/**
 * 
 */
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
/**
 * @author lessenic
 *
 */
public class Stats {
	
	/**
	 * 
	 */
	private List<MergeTripleStats> scenariostats; 
	
	/**
	 * Number of conflicts. 
	 */
	private int conflicts = 0;
	
	/**
	 * Returns the number of conflicts.
	 * @return number of conflicts
	 */
	public final int getConflicts() {
		return conflicts;
	}

	/**
	 * Sets the number of conflicts.
	 * @param conflicts number of conflicts to set
	 */
	public final void setConflicts(final int conflicts) {
		this.conflicts = conflicts;
	}
	
	/**
	 * Runtime.
	 */
	private long runtime = 0;
	
	/**
	 * Returns the runtime.
	 * @return runtime
	 */
	public final long getRuntime() {
		return runtime;
	}
	
	/**
	 * Sets the runtime.
	 * 
	 * @param runtime runtime
	 */
	public final void setRuntime(final long runtime) {
		this.runtime = runtime;
	}
	
	/**
	 * Increases the number of conflicts.
	 * @param conflicts number of conflicts to add
	 */
	public final void addConflicts(final int conflicts) {
		this.conflicts += conflicts;
	}
	
	/**
	 * Increase the runtime statistics.
	 * @param runtime 
	 */
	public final void increaseRuntime(final long runtime) {
		this.runtime += runtime;
	}

	/**
	 * Map of elements.
	 */
	private HashMap<String, StatsElement> elements;
	
	/**
	 * Map of operations.
	 */
	private HashMap<String, Integer> operations;
	
	/**
	 * Creates a new Stats instance from a list of keys.
	 * @param scenariostats list of scenario stats
	 * @param keys List of keys
	 */
	public Stats(final List<MergeTripleStats> scenariostats, 
			final List<String> keys) {
		assert (keys != null);
		assert (!keys.isEmpty());
		
		// If necessary, initialize lists and maps
		if (this.scenariostats == null) {
			this.scenariostats =  new LinkedList<>();
		}
		
		if (scenariostats != null) {
			this.scenariostats.addAll(scenariostats);
		}
		
		if (elements == null) {
			elements = new HashMap<String, StatsElement>();
		}
		
		if (operations == null) {
			operations = new HashMap<String, Integer>();
		}
		
		for (String key : keys) {
			elements.put(key, new StatsElement());
		}
	}
	
	/**
	 * Creates a new Stats instance from an array of keys.
	 * @param keys array of keys
	 */
	public Stats(final String[] keys) {
		this(null, Arrays.asList(keys));
	}
	
	/**
	 * Adds the statistical data from another stats object.
	 * 
	 * @param other stats data to add
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
	 * @return set of keys
	 */
	public final Set<String> getKeys() {
		assert (elements != null);
		return elements.keySet();
	}
	
	/**
	 * Returns a StatsElement.
	 * @param key element that should be returned
	 * @return element
	 */
	public final StatsElement getElement(final String key) {
		assert (elements != null);
		assert (elements.containsKey(key));
		return elements.get(key);
	}
	
	/**
	 * Returns a set of operations.
	 * @return set of operations
	 */
	public final Set<String> getOperations() {
		assert (operations != null);
		return operations.keySet();
	}
	
	/**
	 * Increases the counter of an operation.
	 * @param op operation
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
	 * @param opName name of the operation
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
	 * @param tripleStats triple statistics
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
	
}
