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
 * Tests the merge functionality of JDime as a black-box.
 */
public class MergeTest extends JDimeTest {

    private static final String[] STRATEGIES = { "linebased", "structured", "combined" };

    private static File leftDir;
    private static File baseDir;
    private static File rightDir;

    private MergeContext context;

    @BeforeClass
    public static void init() throws Exception {

       leftDir = file("/threeway/left");
       baseDir = file("/threeway/base");
       rightDir =file("/threeway/right");

        Arrays.asList(leftDir, baseDir, rightDir).forEach(f -> {
            assertTrue(f.getAbsolutePath() + " is not a directory.", f.isDirectory());
        });

        JDimeConfig.setLogLevel("WARNING");
    }

    @Before
    public void setUp() throws Exception {
        context = new MergeContext();
        context.setQuiet(true);
        context.setPretend(false);
    }

    /**
     * Merges files under '/left/filePath', '/right/filePath' and '/base/filePath' (if <code>threeWay</code> is
     * <code>true</code>). Merges will be performed using the strategies in {@link #STRATEGIES} and the output will
     * be compared with the file in '/strategy/filePath'.
     *
     * @param filePath
     *         the path to the files to be merged
     */
    private void runMerge(String filePath) {
        try {
            ArtifactList<FileArtifact> inputArtifacts = new ArtifactList<>();

            inputArtifacts.add(new FileArtifact(file(leftDir, filePath)));
            inputArtifacts.add(new FileArtifact(file(baseDir, filePath)));
            inputArtifacts.add(new FileArtifact(file(rightDir, filePath)));

            for (String strategy : STRATEGIES) {

                // setup context
                context.setMergeStrategy(MergeStrategy.parse(strategy));
                context.setInputFiles(inputArtifacts);

                File out = Files.createTempFile("jdime-tests", ".java").toFile();
                out.deleteOnExit();

                context.setOutputFile(new FileArtifact(out));

                // run
                System.out.printf("Running %s strategy on %s%n", strategy, filePath);
                Main.merge(context);

                // check
                String expected = normalize(FileUtils.readFileToString(file("threeway", strategy, filePath)));
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

    @Test
    public void testBag() {
        runMerge("SimpleTests/Bag/Bag.java");
    }

    @Test
    public void testBag2() {
        runMerge("SimpleTests/Bag/Bag2.java");
    }

    @Test
    public void testBag3() {
        runMerge("SimpleTests/Bag/Bag3.java");
    }
    
    @Test
    public void testImportConflict () {
        runMerge("SimpleTests/ImportMess.java");
    }

    @Test
    public void testExprTest () {
        runMerge("SimpleTests/ExprTest.java");
    }
}
