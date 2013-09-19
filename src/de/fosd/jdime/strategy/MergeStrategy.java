/* 
 * Copyright (C) 2013 Olaf Lessenich.
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
 */
package de.fosd.jdime.strategy;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.merge.MergeInterface;
import java.util.HashMap;
import java.util.Set;

/**
 * @author Olaf Lessenich
 *
 * @param <T> type of artifact
 */
public abstract class MergeStrategy<T extends Artifact<T>> implements
        MergeInterface<T>, StatsInterface<T>, DumpInterface<T> {

    /**
     * Map holding all strategies.
     */
    private static HashMap<String, MergeStrategy<?>> strategyMap = null;

    /**
     * Initializes the strategy map.
     */
    private static void initialize() {
        strategyMap = new HashMap<>();
        LinebasedStrategy linebased = new LinebasedStrategy();
        StructuredStrategy structured = new StructuredStrategy();
        CombinedStrategy combined = new CombinedStrategy();
        
        strategyMap.put("linebased", linebased);
        strategyMap.put("unstructured", linebased);
        
        strategyMap.put("structured", structured);
        
        strategyMap.put("combined", combined);
        strategyMap.put("autotuning", combined);
    }

    /**
     * Returns a set containing the names of available strategies.
     *
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
     * @param str name of the merge tool
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

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public abstract String toString();
}
