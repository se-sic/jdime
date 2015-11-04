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

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.fosd.jdime.common.ArtifactList;
import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.strategy.MergeStrategy;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
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

    private File testFilesDir;
    private File leftDir;
    private File baseDir;
    private File rightDir;

    private MergeContext context;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {

        testFilesDir = new File("testfiles");

        assertTrue("The test files directory could not be found.", testFilesDir.exists() && testFilesDir.isDirectory());

        leftDir = new File(testFilesDir, "left");
        baseDir = new File(testFilesDir, "base");
        rightDir = new File(testFilesDir, "right");

        Arrays.asList(leftDir, baseDir, rightDir).forEach(f -> {
            assertTrue(f.getAbsolutePath() + " couldn't be found or isn't a directory.", f.exists() && f.isDirectory());
        });

        // initialize logger
        Logger root = Logger.getLogger(JDimeWrapper.class.getPackage().getName());
        root.setLevel(Level.WARNING);

        for (Handler handler : root.getHandlers()) {
            handler.setLevel(Level.WARNING);
        }

        // initialize context
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
                String output = context.getOutputFile().getContent();

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
