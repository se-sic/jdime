package de.fosd.jdime.strategy;

import java.util.Arrays;
import java.util.Collections;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.function.Predicate;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for <code>StatisticsInterface</code>.
 */
public class StatisticsInterfaceTest {

    @Test
    public void testSegmentStatistics() throws Exception {
        List<Boolean> seg1 = Arrays.asList(true, true, true, false, true, true, false, true, false, true);
        Predicate<Boolean> p = b -> b;

        IntSummaryStatistics stats = StatisticsInterface.segmentStatistics(seg1, p);

        assertEquals(stats.getCount(), 4);
        assertEquals(stats.getAverage(), (3 + 2 + 1 + 1) / 4.0, 0.0001);
        assertEquals(stats.getMax(), 3);
        assertEquals(stats.getMin(), 1);
        assertEquals(stats.getSum(), 7);

        List<Boolean> seg2 = Collections.emptyList();

        stats = StatisticsInterface.segmentStatistics(seg2, p);
        assertEquals(stats.getCount(), 0);

        List<Boolean> seg3 = Arrays.asList(true, true, true);

        stats = StatisticsInterface.segmentStatistics(seg3, p);
        assertEquals(stats.getCount(), 1);
        assertEquals(stats.getAverage(), 3, 0.0001);
        assertEquals(stats.getMax(), 3);
        assertEquals(stats.getMin(), 3);
        assertEquals(stats.getSum(), 3);

        List<Boolean> seg4 = Arrays.asList(false, false, false);

        stats = StatisticsInterface.segmentStatistics(seg4, p);
        assertEquals(stats.getCount(), 0);
    }
}