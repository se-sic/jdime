/**
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2018 University of Passau, Germany
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

import java.io.File;

import de.fosd.jdime.JDimeTest;
import de.fosd.jdime.stats.CodeStatistics;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

/**
 * Tests for <code>Parser</code>.
 */
public class ParserTest extends JDimeTest {

    @Test
    public void testParse() throws Exception {
        File file = file(resultsDir, "linebased", "SimpleTests", "Bag", "Bag2.java");
        String code = FileUtils.readFileToString(file, UTF_8);
        ParseResult result = Parser.parse(code);
        CodeStatistics cs = result.getStats();

        assertEquals(11, cs.getLinesOfCode());
        assertEquals(1, cs.getConflicts());
        assertEquals(7, cs.getConflictingLinesOfCode());
        assertEquals(normalize(code), normalize(result.toString()));

        file = file(resultsDir, "linebased", "SimpleTests", "Bag", "Bag3.java");
        code = FileUtils.readFileToString(file, UTF_8);
        result = Parser.parse(code);
        cs = result.getStats();

        assertEquals(20, cs.getLinesOfCode());
        assertEquals(0, cs.getConflicts());
        assertEquals(0, cs.getConflictingLinesOfCode());
        assertEquals(normalize(code), normalize(result.toString()));

        file = file(resultsDir, "linebased", "ParserTest", "Comments.java");
        code = FileUtils.readFileToString(file, UTF_8);
        result = Parser.parse(code);
        cs = result.getStats();

        assertEquals(7, cs.getLinesOfCode());
        assertEquals(0, cs.getConflicts());
        assertEquals(0, cs.getConflictingLinesOfCode());
        assertEquals(normalize(code), normalize(result.toString()));

        file = file(resultsDir, "linebased", "ParserTest", "CommentsConflict.java");
        code = FileUtils.readFileToString(file, UTF_8);
        result = Parser.parse(code);
        cs = result.getStats();

        assertEquals(11, cs.getLinesOfCode());
        assertEquals(1, cs.getConflicts());
        assertEquals(2, cs.getConflictingLinesOfCode());
        assertEquals(normalize(code), normalize(result.toString()));

        file = file(resultsDir, "linebased", "ParserTest", "FilteredConflicts.java");
        code = FileUtils.readFileToString(file, UTF_8);
        result = Parser.parse(code);
        cs = result.getStats();

        assertEquals(10, cs.getLinesOfCode());
        assertEquals(0, cs.getConflicts());
        assertEquals(0, cs.getConflictingLinesOfCode());
        assertEquals(normalize(code), normalize(result.toString()));
    }
}