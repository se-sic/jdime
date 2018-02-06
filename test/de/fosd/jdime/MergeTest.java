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
package de.fosd.jdime;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.google.gson.Gson;
import de.fosd.jdime.artifact.ArtifactList;
import de.fosd.jdime.artifact.file.FileArtifact;
import de.fosd.jdime.config.JDimeConfig;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.strategy.MergeStrategy;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static de.fosd.jdime.artifact.file.FileArtifact.FileType.FILE;
import static de.fosd.jdime.config.merge.MergeScenario.BASE;
import static de.fosd.jdime.config.merge.MergeScenario.LEFT;
import static de.fosd.jdime.config.merge.MergeScenario.MERGE;
import static de.fosd.jdime.config.merge.MergeScenario.RIGHT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Executes merge tests defined in the file {@value TEST_CASES_FILE}.
 */
@RunWith(Parameterized.class)
public class MergeTest extends JDimeTest {

    private static final String TEST_CASES_FILE = "JDimeMergeTests.json";

    private MergeTestCase testCase;
    private MergeContext context;

    @Parameters(name = "MergeTest: {0}")
    public static Iterable<?> data() throws Exception {
        Gson gson = new Gson();

        List<MergeTestCase> testCases;
        File testCasesFile = file(TEST_CASES_FILE);

        try (FileReader reader = new FileReader(testCasesFile)) {
            testCases = Arrays.asList(gson.fromJson(reader, MergeTestCase[].class));
        }

        return testCases;
    }

    @BeforeClass
    public static void init() throws Exception {
        JDimeConfig.setLogLevel("WARNING");
    }

    public MergeTest(MergeTestCase testCase) {
        this.testCase = testCase;
    }

    @Before
    public void setUp() throws Exception {
        context = new MergeContext();
    }

    @Test
    public void mergeTest() {
        assertTrue("The " + MergeTestCase.class.getSimpleName() + " named " + testCase + " is invalid.", testCase.valid());

        List<FileArtifact> inputs = new ArtifactList<>();

        switch (testCase.type) {
            case TWOWAY:
                inputs.add(new FileArtifact(LEFT, file(leftDir, testCase.path)));
                inputs.add(new FileArtifact(RIGHT, file(rightDir, testCase.path)));
                break;
            case THREEWAY:
                inputs.add(new FileArtifact(LEFT, file(leftDir, testCase.path)));
                inputs.add(new FileArtifact(BASE, file(baseDir, testCase.path)));
                inputs.add(new FileArtifact(RIGHT, file(rightDir, testCase.path)));
                break;
            case NWAY:
            default:
                fail(MergeStrategy.NWAY + " test cases are not supported yet.");
                break;
        }

        for (String strategy : testCase.strategies) {
            Optional<MergeStrategy<FileArtifact>> oStrategy = MergeStrategy.parse(strategy);
            assertTrue("Strategy " + strategy + " is invalid.", oStrategy.isPresent());

            context.setMergeStrategy(oStrategy.get());

            context.setInputFiles(inputs);
            context.setOutputFile(new FileArtifact(MERGE, FILE));

            String dirName = strategy.replaceAll(",", "_");
            File expectedFile = file(resultsDir, dirName, testCase.path);

            Main.merge(context);

            String expected;
            String output;

            try {
                expected = normalize(FileUtils.readFileToString(expectedFile, UTF_8));
                output = normalize(context.getOutputFile().getContent());
            } catch (IOException e) {
                fail(e.getMessage());
                return;
            }

            try {
                assertEquals("Strategy " + strategy + " resulted in unexpected output.", expected, output);
            } catch (AssertionError e) {
                System.out.println("----------Expected:-----------");
                System.out.println(expected);
                System.out.println("----------Received:-----------");
                System.out.println(output);
                System.out.println("------------------------------");
                System.out.println();

                throw e;
            }
        }
    }
}
