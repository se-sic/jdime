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
package de.fosd.jdime.strategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.artifact.file.FileArtifact;
import de.fosd.jdime.merge.MergeInterface;

/**
 * A <code>MergeStrategy</code> implements an algorithm to merge a certain type of <code>Artifacts</code>.
 * <p>
 * The respective <code>MergeStrategies</code> can be optimized regarding different objectives, e.g., generalization,
 * performance, precision, or also a trade-off of concerns.
 *
 * @param <T>
 *         type of artifact
 * @author Olaf Lessenich
 */
public abstract class MergeStrategy<T extends Artifact<T>> implements MergeInterface<T> {

    public static final String LINEBASED = "linebased";
    public static final String SEMISTRUCTURED = "semistructured";
    public static final String STRUCTURED = "structured";
    public static final String NWAY = "nway";

    /**
     * Returns an unmodifiable <code>List</code> containing the names of available strategies.
     *
     * @return names of available strategies
     */
    public static List<String> listStrategies() {
        return Arrays.asList(LINEBASED, SEMISTRUCTURED, STRUCTURED, NWAY);
    }

    /**
     * Returns a {@link CombinedStrategy} for the given {@link MergeStrategy MergeStrategies}.
     *
     * @param firstName the first strategy name
     * @param secondName the second strategy name
     * @param otherNames the other strategy names
     * @return a {@link CombinedStrategy} for the given names
     */
    public static Optional<MergeStrategy<FileArtifact>> parse(String firstName, String secondName, String... otherNames) {
        List<String> names = new ArrayList<>(2 + (otherNames != null ? otherNames.length : 0));

        names.add(firstName);
        names.add(secondName);

        if (otherNames != null) {
            Collections.addAll(names, otherNames);
        }

        return parse(String.join(",", names));
    }

    /**
     * Returns a <code>MergeStrategy</code> for the given <code>name</code>. <code>name</code> (ignoring case and
     * leading/trailing whitespaces) must be one of the strings returned by {@link #listStrategies()}. If no
     * <code>MergeStrategy</code> for the given <code>name</code> is found, an empty {@link Optional} is returned.
     *
     * @param name
     *         the name to return a <code>MergeStrategy</code> for; <code>name</code> may not be <code>null</code>
     * @return optionally the <code>MergeStrategy</code>
     * @throws NullPointerException
     *         if <code>name</code> is <code>null</code>
     */
    public static Optional<MergeStrategy<FileArtifact>> parse(String name) {
        Objects.requireNonNull(name, "name may not be null!");

        name = name.trim().toLowerCase();
        MergeStrategy<FileArtifact> strategy = null;

        switch (name) {
            case LINEBASED:
                strategy = new LinebasedStrategy();
                break;
            case SEMISTRUCTURED:
                strategy = new SemiStructuredStrategy();
                break;
            case STRUCTURED:
                strategy = new StructuredStrategy();
                break;
            case NWAY:
                strategy = new NWayStrategy();
                break;
            default:
                if (name.indexOf(',') != -1) {
                    String[] names = name.split(",");

                    if (names.length > 0) {
                        List<MergeStrategy<FileArtifact>> strategies = new ArrayList<>(names.length);

                        for (String subName : names) {
                            Optional<MergeStrategy<FileArtifact>> optStrategy = parse(subName);

                            if (optStrategy.isPresent()) {
                                strategies.add(optStrategy.get());
                            } else {
                                return Optional.empty();
                            }
                        }

                        strategy = new CombinedStrategy(strategies);
                    }
                }
        }

        return Optional.ofNullable(strategy);
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName();
    }
}