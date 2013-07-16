/**
 * 
 */
package de.fosd.jdime.stats;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * @author Olaf Lessenich
 *
 */
public class Stats {
	
	/**
	 * Map of elements.
	 */
	private HashMap<String, StatsElement> elements;
	
	/**
	 * Creates a new Stats instance from a list of keys.
	 * @param keys List of keys
	 */
	public Stats(final List<String> keys) {
		assert (keys != null);
		assert (!keys.isEmpty());
		for (String key : keys) {
			elements.put(key, new StatsElement());
		}
	}
	
	/**
	 * Creates a new Stats instance from an array of keys.
	 * @param keys array of keys
	 */
	public Stats(final String[] keys) {
		this(Arrays.asList(keys));
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
	}
	
	/**
	 * Returns a set of keys.
	 * @return set of keys
	 */
	public final Set<String> getKeys() {
		return elements.keySet();
	}
	
	/**
	 * Returns a StatsElement.
	 * @param key element that should be returned
	 * @return element
	 */
	public final StatsElement getElement(final String key) {
		assert (elements.containsKey(key));
		return elements.get(key);
	}
	
}
