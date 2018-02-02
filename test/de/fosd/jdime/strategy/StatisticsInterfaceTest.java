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

import java.util.Arrays;
import java.util.Collections;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.function.Predicate;

import de.fosd.jdime.stats.StatisticsInterface;
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