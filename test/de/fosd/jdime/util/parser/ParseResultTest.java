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
package de.fosd.jdime.util.parser;

import de.fosd.jdime.JDimeTest;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for <code>ParseResult</code>
 */
public class ParseResultTest extends JDimeTest {

    private ParseResult res;

    @Before
    public void setUp() throws Exception {
        res = new ParseResult();

        assertTrue(res.size() == 0);
        assertEquals("", res.toString());
    }

    @Test
    public void testAddMergedLine() throws Exception {
        String[] lines = {"L0", "L1", "L2", "L3", "L4"};
        String concat = String.join(System.lineSeparator(), lines);

        for (String line : lines) {
            res.addMergedLine(line);
        }

        assertEquals(1, res.size());
        assertThat(res.get(0), is(instanceOf(Content.Merged.class)));
        assertEquals(concat, res.toString());
        assertEquals(concat, res.toString("ID1", "ID2")); // IDs are ignored if there are not conflicts
    }

    @Test
    public void testAddConflictingLine() throws Exception {
        String[] lines = {"L0", "L1", "L2", "L3", "L4"};
        String concat = String.format("%s%n" +
                                      "<<<<<<< ID1%n" +
                                      "%s%n" +
                                      "%s%n" +
                                      "=======%n" +
                                      "%s%n" +
                                      ">>>>>>> ID2%n" +
                                      "%s", (Object[]) lines);

        res.addMergedLine(lines[0]);
        res.addConflictingLine(lines[1], true);
        res.addConflictingLine(lines[2], true);
        res.addConflictingLine(lines[3], false);
        res.addMergedLine(lines[4]);

        assertEquals(3, res.size());
        assertThat(res.get(0), is(instanceOf(Content.Merged.class)));
        assertThat(res.get(1), is(instanceOf(Content.Conflict.class)));
        assertThat(res.get(2), is(instanceOf(Content.Merged.class)));
        assertEquals(normalize(concat), res.toString());
        assertEquals(concat, res.toString("ID1", "ID2"));
    }
}