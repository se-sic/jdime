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
package de.fosd.jdime;

import java.util.List;

import de.fosd.jdime.config.merge.MergeType;
import de.fosd.jdime.strategy.MergeStrategy;

/**
 * An array of {@link MergeTestCase} instances will be deserialized from a file called
 * {@value MergeTest#TEST_CASES_FILE} and used to execute merge tests using {@link MergeTest}
 */
public class MergeTestCase {

    private static final String UNNAMED = "UNNAMED";

    private final String name;

    public final List<String> strategies;

    public final MergeType type;
    public final String path;

    /**
     * Constructs a new {@link MergeTestCase}.
     *
     * @param name
     *         the descriptive name of the {@link MergeTestCase}
     * @param strategies
     *         the merge strategies to be applied to the files defined using {@code type} and {@code path},
     *         must be parseable by {@link MergeStrategy#parse(String)}
     * @param type
     *         the type of merge to perform, must be one of {@link MergeType#TWOWAY}, {@link MergeType#THREEWAY}
     * @param path
     *         the relative path under the {@code /left}, {@code /base}, and {@code /right} directories as well as
     *         the appropriate directories under the {@code /results} directory in the {@code jdime-testfiles}
     *         repository
     */
    public MergeTestCase(String name, List<String> strategies, MergeType type, String path) {
        this.name = name;
        this.strategies = strategies;
        this.type = type;
        this.path = path;
    }

    /**
     * Tests whether all required properties were deserialized from the JSON. This method does not test whether valid
     * (existing paths, supported {@link MergeType MergeTypes}) were deserialized.
     *
     * @return true iff values for alle required fields were deserialized
     */
    public boolean valid() {

        if (strategies == null || strategies.isEmpty()) {
            return false;
        }

        if (type == null) {
            return false;
        }

        if (path == null) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return name != null ? name : UNNAMED;
    }
}
