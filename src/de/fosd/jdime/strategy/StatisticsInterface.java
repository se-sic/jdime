/*
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2015 University of Passau, Germany
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
 */
package de.fosd.jdime.strategy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.Revision;
import de.fosd.jdime.stats.ElementStatistics;
import de.fosd.jdime.stats.KeyEnums;
import de.fosd.jdime.stats.MergeScenarioStatistics;
import de.fosd.jdime.stats.MergeStatistics;

public interface StatisticsInterface<T extends Artifact<T>> {

    /**
     * Returns the <code>KeyEnums.Type</code> matching this <code>Artifact</code>.
     *
     * @return the type of this <code>Artifact</code>
     */
    KeyEnums.Type getType();

    /**
     * Returns the <code>KeyEnums.Level</code> matching this <code>Artifact</code>.
     *
     * @return the level of this <code>Artifact</code>
     */
    KeyEnums.Level getLevel();

    /**
     * Called by the <code>AddOperation</code> on the <code>Artifact</code> being added. The default implementation
     * does nothing and is intended to be overridden by <code>Artifact</code> implementations wishing to record custom
     * statistics after being added.
     *
     * @param mScenarioStatistics
     *         the <code>MergeScenarioStatistics</code> for the current
     *         <code>MergeScenarioStatistics</code>
     * @param mergeContext
     *         the <code>MergeContext</code> of the current run
     */
    default void addOpStatistics(MergeScenarioStatistics mScenarioStatistics, MergeContext mergeContext) {

    }

    /**
     * Called by the <code>DeleteOperation</code> on the <code>Artifact</code> being deleted. The default implementation
     * does nothing and is intended to be overridden by <code>Artifact</code> implementations wishing to record custom
     * statistics after being deleted.
     *
     * @param mScenarioStatistics
     *         the <code>MergeScenarioStatistics</code> for the current
     *         <code>MergeScenarioStatistics</code>
     * @param mergeContext
     *         the <code>MergeContext</code> of the current run
     */
    default void deleteOpStatistics(MergeScenarioStatistics mScenarioStatistics, MergeContext mergeContext) {

    }

    /**
     * Called by the <code>MergeOperation</code> on the first <code>Artifact</code> that was added to the
     * <code>MergeScenario</code> being merged. The default implementation does nothing and is intended to be
     * overridden by <code>Artifact</code> implementations wishing to record custom statistics after being merged.
     *
     * @param mScenarioStatistics
     *         the <code>MergeScenarioStatistics</code> for the current
     *         <code>MergeScenarioStatistics</code>
     * @param mergeContext
     *         the <code>MergeContext</code> of the current run
     */
    default void mergeOpStatistics(MergeScenarioStatistics mScenarioStatistics, MergeContext mergeContext) {

    }

    static MergeScenarioStatistics getASTStatistics(Artifact<?> artifact, Revision otherRev) {
        MergeScenarioStatistics statistics = new MergeScenarioStatistics(null);
        List<ElementStatistics> elementStats = new ArrayList<>();
        List<Artifact<?>> preOrder = new ArrayList<>();

        {
            Deque<Artifact<?>> q = new ArrayDeque<>(Collections.singleton(artifact));

            while (!q.isEmpty()) {
                Artifact<?> curr = q.removeFirst();

                preOrder.add(curr);
                curr.getChildren().forEach(q::addFirst);
            }
        }

        for (Artifact<?> current : preOrder) {
            elementStats.clear();

            KeyEnums.Type type = current.getType();
            KeyEnums.Level level = current.getLevel();

            elementStats.add(statistics.getTypeStatistics(current.getRevision(), type));

            if (level != KeyEnums.Level.NONE) {
                elementStats.add(statistics.getLevelStatistics(current.getRevision(), level));
            }

            elementStats.forEach(ElementStatistics::incrementTotal);

            if (current.isConflict()) {
                elementStats.forEach(ElementStatistics::incrementNumChanged);
                elementStats.forEach(ElementStatistics::incrementNumOccurInConflic);
            } else if ((otherRev == null && current.hasMatches()) || current.hasMatching(otherRev)) {
                elementStats.forEach(ElementStatistics::incrementNumMatched);
            } else {
                elementStats.forEach(ElementStatistics::incrementNumChanged);

                // added or deleted?
                if (current.hasMatches()) {
                    elementStats.forEach(ElementStatistics::incrementNumDeleted);
                } else {
                    elementStats.forEach(ElementStatistics::incrementNumAdded);
                }
            }
        }

        MergeStatistics mergeStatistics = statistics.getMergeStatistics(artifact.getRevision());

        Optional<Artifact<?>> max = preOrder.stream().max((o1, o2) -> Integer.compare(o1.getNumChildren(), o2.getNumChildren()));

        max.ifPresent(a -> mergeStatistics.setMaxNumChildren(a.getNumChildren()));
        mergeStatistics.setMaxASTDepth(artifact.getMaxDepth());

        Predicate<Artifact<?>> p = a -> a.isConflict() || !((otherRev == null && a.hasMatches()) || a.hasMatching(otherRev));
        IntSummaryStatistics summary = segmentStatistics(preOrder, p);

        mergeStatistics.setNumChunks((int) summary.getCount());
        mergeStatistics.setAvgChunkSize((float) summary.getAverage());

        return statistics;
    }

    /**
     * Given a collection of items and a <code>Predicate</code> to be fulfilled returns an
     * <code>IntSummaryStatistics</code> over a list of the lengths of segments of successive (as determined by the
     * order they are returned by the iterator of the collection) items that fulfill the predicate.
     *
     * @param coll
     *         the collection of items
     * @param test
     *         the predicate to fulfill
     * @param <T>
     *         the type of the items
     * @return an <code>IntSummaryStatistics</code> over the list of segment lengths
     */
    static <T> IntSummaryStatistics segmentStatistics(Collection<T> coll, Predicate<T> test) {
        List<Integer> chunkSizes = new ArrayList<>();
        int currentSize = 0;
        boolean inChunk = false;

        for (T item : coll) {
            boolean p = test.test(item);

            if (p) {
                if (!inChunk) {
                    inChunk = true;
                }

                currentSize++;
            } else {
                if (inChunk) {
                    chunkSizes.add(currentSize);
                    currentSize = 0;
                    inChunk = false;
                }
            }
        }

        if (inChunk) {
            chunkSizes.add(currentSize);
        }

        return chunkSizes.stream().collect(Collectors.summarizingInt(i -> i));
    }
}
