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
package de.fosd.jdime.stats;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.config.merge.MergeScenario;
import de.fosd.jdime.config.merge.Revision;
import de.fosd.jdime.matcher.matching.Matching;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface StatisticsInterface {

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

    /**
     * Collects statistics about the given <code>artifact</code> tree. The <code>otherRev</code> is used when counting
     * added, deleted and matched elements elements. <code>otherRev</code> should be the 'opposite'
     * <code>Revision</code> e.g. when <code>artifact</code> represents the tree of the 'left' revision,
     * <code>otherRev</code> should be the 'right' <code>Revision</code>. The resulting
     * <code>MergeScenarioStatistics</code> contains <code>null</code> as its <code>MergeScenario</code> and is intended
     * to be added to an existing <code>MergeScenarioStatistics</code> instance.
     *
     * @param artifact
     *         the <code>Artifact</code> tree to collect statistics for
     * @param otherRev
     *         the 'opposite' <code>Revision</code> or <code>null</code> (when collecting statistics for the target
     *         <code>Artifact</code>)
     * @return the resulting <code>MergeScenarioStatistics</code>
     */
    static MergeScenarioStatistics getASTStatistics(Artifact<?> artifact, Revision otherRev) {
        MergeScenarioStatistics statistics = new MergeScenarioStatistics((MergeScenario<?>) null);
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

        Predicate<Artifact<?>> otherMatches = a -> ((otherRev == null && a.hasMatches()) || a.hasMatching(otherRev));
        Predicate<Artifact<?>> isConflict = Artifact::isConflict;

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
                elementStats.forEach(ElementStatistics::incrementNumOccurInConflict);
            } else if (otherMatches.negate().test(current)) {

                // added or deleted?
                if (current.hasMatches()) {
                    elementStats.forEach(ElementStatistics::incrementNumDeleted);
                } else {
                    elementStats.forEach(ElementStatistics::incrementNumAdded);
                }
            }

            if (otherRev != null) {
                Matching<?> matching = current.getMatching(otherRev);

                if (matching != null) {
                    statistics.addMatching(matching);
                }
            }
        }

        MergeStatistics mergeStatistics = statistics.getMergeStatistics(artifact.getRevision());

        Optional<Artifact<?>> max = preOrder.stream().max(Comparator.comparingInt(Artifact::getNumChildren));

        max.ifPresent(a -> mergeStatistics.setMaxNumChildren(a.getNumChildren()));
        mergeStatistics.setMaxASTDepth(artifact.getMaxDepth());

        IntSummaryStatistics summary = segmentStatistics(preOrder, isConflict.or(otherMatches.negate()));

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

        for (T item : coll) {
            boolean p = test.test(item);

            if (p) {
                currentSize++;
            } else {
                if (currentSize != 0) {
                    chunkSizes.add(currentSize);
                    currentSize = 0;
                }
            }
        }

        if (currentSize != 0) {
            chunkSizes.add(currentSize);
        }

        return chunkSizes.stream().collect(Collectors.summarizingInt(i -> i));
    }
}
