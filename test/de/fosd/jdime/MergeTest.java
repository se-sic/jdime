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

package de.fosd.jdime;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;

import de.fosd.jdime.common.ArtifactList;
import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.strategy.MergeStrategy;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Olaf Lessenich
 *
 */
public class MergeTest {

    private static final String[] STRATEGIES = { "linebased", "structured", "combined" };

    private static File leftDir;
    private static File baseDir;
    private static File rightDir;

    private MergeContext context;

    @BeforeClass
    public static void init() throws Exception {

       leftDir = file("/left");
       baseDir = file("/base");
       rightDir =file("/right");

        Arrays.asList(leftDir, baseDir, rightDir).forEach(f -> {
            assertTrue(f.getAbsolutePath() + " is not a directory.", f.isDirectory());
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
                String expected = normalize(FileUtils.readFileToString(file(strategy, filepath)));
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
     * Returns a file using the {@link Class#getResource(String)} method of the class <code>MergeTest</code> and
     * the given path.
     *
     * @param path
     *         the file path
     * @return the resulting <code>File</code>
     * @throws Exception
     *         if the file does not exist or there is an exception constructing it
     */
    private static File file(String path) throws Exception {
        URL res = MergeTest.class.getResource(path);

        assertNotNull("The file " + path + " was not found.", res);
        return new File(res.toURI());
    }

    /**
     * Constructs an absolute (in the classpath) path from the given names an passes it to {@link #file(String)}.
     *
     * @param firstName
     *         the first element of the path
     * @param names
     *         the other elements of the path
     * @return the resulting <code>File</code>
     * @throws Exception
     *         if the file does not exist or there is an exception constructing it
     */
    private static File file(String firstName, String... names) throws Exception {
        String path = String.format("/%s/%s", firstName, String.join("/", names));
        return file(path);
    }

    /**
     * Replaces the system file separator in every line starting with a conflict marker by the expected '/' separator.
     *
     * @param content
     *         the content in which to replace file separators
     * @return the normalized <code>String</code>
     */
    private static String normalize(String content) {
        String conflictStart = "<<<<<<<";
        String conflictEnd = ">>>>>>>";
        String lineSeparator = System.lineSeparator();
        StringBuilder b = new StringBuilder(content.length());

        try (BufferedReader r = new BufferedReader(new StringReader(content))) {
            r.lines().forEachOrdered(l -> {

                if (l.startsWith(conflictStart)) {
                    l = conflictStart;
                } else if (l.startsWith(conflictEnd)) {
                    l = conflictEnd;
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
