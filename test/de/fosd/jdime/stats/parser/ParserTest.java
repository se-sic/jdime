package de.fosd.jdime.stats.parser;

import java.io.File;

import de.fosd.jdime.JDimeTest;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for <code>Parser</code>.
 */
public class ParserTest extends JDimeTest {

    @Test
    public void testParse() throws Exception {
        File file = file("threeway", "linebased", "SimpleTests", "Bag", "Bag2.java");
        String code = FileUtils.readFileToString(file);
        ParseResult result = Parser.parse(code);

        assertEquals(11, result.getLinesOfCode());
        assertEquals(1, result.getConflicts());
        assertEquals(7, result.getConflictingLinesOfCode());
        assertEquals(normalize(code), normalize(result.toString()));

        file = file("threeway", "linebased", "SimpleTests", "Bag", "Bag3.java");
        code = FileUtils.readFileToString(file);
        result = Parser.parse(code);

        assertEquals(20, result.getLinesOfCode());
        assertEquals(0, result.getConflicts());
        assertEquals(0, result.getConflictingLinesOfCode());
        assertEquals(normalize(code), normalize(result.toString()));

        file = file("threeway", "linebased", "ParserTest", "Comments.java");
        code = FileUtils.readFileToString(file);
        result = Parser.parse(code);

        assertEquals(7, result.getLinesOfCode());
        assertEquals(0, result.getConflicts());
        assertEquals(0, result.getConflictingLinesOfCode());
        assertEquals(normalize(code), normalize(result.toString()));

        file = file("threeway", "linebased", "ParserTest", "CommentsConflict.java");
        code = FileUtils.readFileToString(file);
        result = Parser.parse(code);

        assertEquals(11, result.getLinesOfCode());
        assertEquals(1, result.getConflicts());
        assertEquals(2, result.getConflictingLinesOfCode());
        assertEquals(normalize(code), normalize(result.toString()));
    }
}