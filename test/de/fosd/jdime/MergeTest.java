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

import java.io.File;
import java.nio.file.Files;

import de.fosd.jdime.common.ArtifactList;
import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.config.JDimeConfig;
import de.fosd.jdime.strategy.MergeStrategy;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static de.fosd.jdime.common.MergeScenario.BASE;
import static de.fosd.jdime.common.MergeScenario.LEFT;
import static de.fosd.jdime.common.MergeScenario.MERGE;
import static de.fosd.jdime.common.MergeScenario.RIGHT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests the merge functionality of JDime as a black-box.
 */
public class MergeTest extends JDimeTest {

    private static final String[] STRATEGIES = { "linebased", "structured", "combined" };

    private MergeContext context;

    @BeforeClass
    public static void init() throws Exception {
        JDimeConfig.setLogLevel("WARNING");
    }

    @Before
    public void setUp() throws Exception {
        context = new MergeContext();
        context.setQuiet(true);
        context.setPretend(false);
    }

    /**
     * Merges files under 'leftDir/filePath', 'rightDir/filePath' and 'baseDir/filePath' (if <code>threeWay</code> is
     * <code>true</code>). Merges will be performed using the strategies in {@link #STRATEGIES} and the output will
     * be compared with the file in '/threeway/strategy/filePath'.
     *
     * @param filePath
     *         the path to the files to be merged
     */
    private void runMerge(String filePath) {
        try {
            ArtifactList<FileArtifact> inputArtifacts = new ArtifactList<>();

            inputArtifacts.add(new FileArtifact(LEFT, file(leftDir, filePath)));
            inputArtifacts.add(new FileArtifact(BASE, file(baseDir, filePath)));
            inputArtifacts.add(new FileArtifact(RIGHT, file(rightDir, filePath)));

            for (String strategy : STRATEGIES) {
                context.setMergeStrategy(MergeStrategy.parse(strategy));
                context.setInputFiles(inputArtifacts);

                File out = Files.createTempFile("jdime-tests", ".java").toFile();
                out.deleteOnExit();

                context.setOutputFile(new FileArtifact(MERGE, out));

                Main.merge(context);

                String expected = normalize(FileUtils.readFileToString(file("threeway", strategy, filePath)));
                String output = normalize(context.getOutputFile().getContent());

                try {
                    assertEquals("Strategy " + strategy + " resulted in unexpected output.", expected, output);
                } catch (Exception e) {
                    System.out.println("----------Expected:-----------");
                    System.out.println(expected);
                    System.out.println("----------Received:-----------");
                    System.out.println(output);
                    System.out.println("------------------------------");
                    System.out.println();

                    throw e;
                }
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

    @Test
    public void testDeletionInsertion() throws Exception {
        runMerge("SimpleTests/DeletionInsertion.java");
    }

    @Test
    public void testVariableDeclaration() throws Exception {
        runMerge("SimpleTests/VariableDeclaration.java");
    }

    @Test
    public void testChangedMethod() throws Exception {
        runMerge("SimpleTests/ChangedMethod.java");
    }

    @Test
    public void testChangedMethod2() throws Exception {
        runMerge("SimpleTests/ChangedMethod2.java");
    }
}
