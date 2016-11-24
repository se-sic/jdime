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
import java.util.ArrayList;
import java.util.List;

import de.fosd.jdime.artifact.ArtifactList;
import de.fosd.jdime.artifact.file.FileArtifact;
import de.fosd.jdime.config.JDimeConfig;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.strategy.MergeStrategy;
import de.uni_passau.fim.seibt.gitwrapper.repo.GitWrapper;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static de.fosd.jdime.config.merge.MergeScenario.BASE;
import static de.fosd.jdime.config.merge.MergeScenario.LEFT;
import static de.fosd.jdime.config.merge.MergeScenario.MERGE;
import static de.fosd.jdime.config.merge.MergeScenario.RIGHT;
import static de.fosd.jdime.strategy.MergeStrategy.COMBINED;
import static de.fosd.jdime.strategy.MergeStrategy.LINEBASED;
import static de.fosd.jdime.strategy.MergeStrategy.STRUCTURED;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests the merge functionality of JDime as a black-box.
 */
public class MergeTest extends JDimeTest {

    /**
     * The name and directory name of a {@link MergeStrategy}.
     */
    private static final class MStrategy {

        /**
         * The name to be passed to {@link MergeStrategy#parse(String)}.
         */
        private final String name;

        /**
         * The directory name to be used to retrieve expected merge results.
         */
        private final String dirName;

        public MStrategy(String name) {
            this.name = name;
            this.dirName = name;
        }

        public MStrategy(String name, String dirName) {
            this.name = name;
            this.dirName = dirName;
        }
    }

    private static List<MStrategy> STRATEGIES;

    private MergeContext context;

    @BeforeClass
    public static void init() throws Exception {
        JDimeConfig.setLogLevel("WARNING");

        STRATEGIES = new ArrayList<>();
        STRATEGIES.add(new MStrategy(LINEBASED));
        STRATEGIES.add(new MStrategy(STRUCTURED));
        STRATEGIES.add(new MStrategy(String.join(",", LINEBASED, STRUCTURED), COMBINED));
    }

    @Before
    public void setUp() throws Exception {
        context = new MergeContext();
        context.setGit(new GitWrapper(MergeContext.DEFAULT_GIT_CMD));
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
            List<FileArtifact> inputArtifacts = new ArtifactList<>();

            inputArtifacts.add(new FileArtifact(LEFT, file(leftDir, filePath)));
            inputArtifacts.add(new FileArtifact(BASE, file(baseDir, filePath)));
            inputArtifacts.add(new FileArtifact(RIGHT, file(rightDir, filePath)));

            for (MStrategy strategy : STRATEGIES) {
                context.setMergeStrategy(MergeStrategy.parse(strategy.name).get());
                context.setInputFiles(inputArtifacts);

                File out = Files.createTempFile("jdime-tests", ".java").toFile();
                out.deleteOnExit();

                context.setOutputFile(new FileArtifact(MERGE, out));

                Main.merge(context);

                String expected = normalize(FileUtils.readFileToString(file("threeway", strategy.dirName, filePath), UTF_8));
                String output = normalize(context.getOutputFile().getContent());

                try {
                    assertEquals("Strategy " + strategy.name + " resulted in unexpected output.", expected, output);
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
    public void testMovedMethod() {
        runMerge("SimpleTests/MovedMethod.java");
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
