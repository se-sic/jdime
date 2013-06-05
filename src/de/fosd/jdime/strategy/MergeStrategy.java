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
package de.fosd.jdime.strategy;

import java.util.HashMap;
import java.util.Set;

import de.fosd.jdime.common.Artifact;

/**
 * @author Olaf Lessenich
 * 
 * @param <T> type of artifact
 */
public abstract class MergeStrategy<T extends Artifact<T>>
							implements MergeInterface<T> {
	
	/**
	 * Map holding all strategies.
	 */
	private static HashMap<String, MergeStrategy<?>> strategyMap 
			= null;
	
	/**
	 * Initializes the strategy map.
	 */
	private static void initialize() {
		strategyMap = new HashMap<String, MergeStrategy<?>>();
		strategyMap.put("linebased", new LinebasedStrategy());
		strategyMap.put("structured", new StructuredStrategy());
		strategyMap.put("combined", new CombinedStrategy());
	}
	
	/**
	 * Returns a set containing the names of available strategies.
	 * @return names of available strategies
	 */
	public static Set<String> listStrategies() {
		if (strategyMap == null) {
			initialize();
		}
		
		assert (strategyMap != null);
				
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

		if (strategyMap == null) {
			initialize();
		}
		
		assert (strategyMap != null);
		
		if (strategyMap.containsKey(input)) {
			return strategyMap.get(input);
		} else {
			throw new StrategyNotFoundException("Strategy not found: " + str);
		}
		
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public abstract String toString();

}
