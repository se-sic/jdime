/**
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
 *     Georg Seibt <seibt@fim.uni-passau.de>
 */
package de.fosd.jdime;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.fosd.jdime.common.ArtifactList;
import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.strategy.MergeStrategy;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Olaf Lessenich
 *
 */
public class MergeTest {

    private static final String[] STRATEGIES = { "linebased", "structured", "combined" };

    private static File testFilesDir;
    private static File leftDir;
    private static File baseDir;
    private static File rightDir;

    private MergeContext context;

    @BeforeClass
    public static void init() throws Exception {
        testFilesDir = new File("testfiles");

        assertTrue("The test files directory could not be found.", testFilesDir.exists() && testFilesDir.isDirectory());

        leftDir = new File(testFilesDir, "left");
        baseDir = new File(testFilesDir, "base");
        rightDir = new File(testFilesDir, "right");

        Arrays.asList(leftDir, baseDir, rightDir).forEach(f -> {
            assertTrue(f.getAbsolutePath() + " couldn't be found or isn't a directory.", f.exists() && f.isDirectory());
        });

        Main.setLogLevel("WARNING");
    }

    @Before
    public void setUp() throws Exception {
        context = new MergeContext();
        context.setQuiet(true);
        context.setPretend(false);
    }

    private void runMerge(String filepath, boolean threeway) {
        try {
            // initialize input files
            ArtifactList<FileArtifact> inputArtifacts = new ArtifactList<>();

            inputArtifacts.add(new FileArtifact(new File(leftDir, filepath)));

            if (threeway) {
                inputArtifacts.add(new FileArtifact(new File(baseDir, filepath)));
            }

            inputArtifacts.add(new FileArtifact(new File(rightDir, filepath)));

            for (String strategy : STRATEGIES) {

                // setup context
                context.setMergeStrategy(MergeStrategy.parse(strategy));
                context.setInputFiles(inputArtifacts);

                File out = Files.createTempFile("jdime-tests", ".java").toFile();
                out.deleteOnExit();

                context.setOutputFile(new FileArtifact(out));

                // run
                System.out.printf("Running %s strategy on %s%n", strategy, filepath);
                Main.merge(context);

                // check
                String expected = FileUtils.readFileToString(FileUtils.getFile(testFilesDir, strategy, filepath));
                String output = normalize(context.getOutputFile().getContent());

                System.out.println("----------Expected:-----------");
                System.out.print(expected);
                System.out.println("----------Received:-----------");
                System.out.print(output);
                System.out.println("------------------------------");

                assertEquals("Strategy " + strategy + " resulted in unexpected output.", expected, output);

                System.out.println();
            }
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * Replaces the system file separator in every line starting with a conflict marker by the expected '/' separator.
     *
     * @param content
     *         the content in which to replace file separators
     * @return the normalized <code>String</code>
     */
    private static String normalize(String content) {
        String lineSeparator = System.lineSeparator();
        StringBuilder b = new StringBuilder(content.length());

        try (BufferedReader r = new BufferedReader(new StringReader(content))) {
            r.lines().forEachOrdered(l -> {
                if (l.startsWith("<<<<<<<") || l.startsWith(">>>>>>>")) {
                    l = l.replaceAll(Pattern.quote(File.separator), Matcher.quoteReplacement("/"));
                }

                b.append(l).append(lineSeparator);
            });
        } catch (IOException e) {
            fail(e.getMessage());
        }

        return b.toString();
    }

    @Test
    public final void testBag() {
        runMerge("SimpleTests/Bag/Bag.java", true);
    }

    @Test
    public final void testBag2() {
        runMerge("SimpleTests/Bag/Bag2.java", true);
    }

    @Test
    public final void testBag3() {
        runMerge("SimpleTests/Bag/Bag3.java", true);
    }
    
    @Test
    public final void testImportConflict () {
        runMerge("SimpleTests/ImportMess.java", true);
    }

    @Test
    public final void testExprTest () {
        runMerge("SimpleTests/ExprTest.java", true);
    }
}
