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

        assertEquals(4, stats.getCount());
        assertEquals((3 + 2 + 1 + 1) / 4.0, 0.0001, stats.getAverage());
        assertEquals(3, stats.getMax());
        assertEquals(1, stats.getMin());
        assertEquals(7, stats.getSum());

        List<Boolean> seg2 = Collections.emptyList();

        stats = StatisticsInterface.segmentStatistics(seg2, p);
        assertEquals(0, stats.getCount());

        List<Boolean> seg3 = Arrays.asList(true, true, true);

        stats = StatisticsInterface.segmentStatistics(seg3, p);
        assertEquals(1, stats.getCount());
        assertEquals(3, stats.getAverage(), 0.0001);
        assertEquals(3, stats.getMax());
        assertEquals(3, stats.getMin());
        assertEquals(3, stats.getSum());

        List<Boolean> seg4 = Arrays.asList(false, false, false);

        stats = StatisticsInterface.segmentStatistics(seg4, p);
        assertEquals(0, stats.getCount());
    }
}