package de.fosd.jdime.stats;

import de.fosd.jdime.JDimeTest;
import de.fosd.jdime.Main;
import de.fosd.jdime.common.ArtifactList;
import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeScenario;
import de.fosd.jdime.common.MergeType;
import de.fosd.jdime.strategy.MergeStrategy;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Tests for <code>Statistics</code>.
 */
public class StatisticsTest extends JDimeTest {

    private MergeContext context;

    @Before
    public void setUp() throws Exception {
        context = new MergeContext();
        context.collectStatistics(true);
        context.setQuiet(true);
        context.setPretend(true);
    }

    @Test
    public void collectStatistics() throws Exception {
        ArtifactList<FileArtifact> inputArtifacts = new ArtifactList<>();
        String filePath = "SimpleTests/Bag/Bag.java";

        inputArtifacts.add(new FileArtifact(file(leftDir, filePath)));
        inputArtifacts.add(new FileArtifact(file(baseDir, filePath)));
        inputArtifacts.add(new FileArtifact(file(rightDir, filePath)));

        context.setMergeStrategy(MergeStrategy.parse("structured"));
        context.setInputFiles(inputArtifacts);

        Main.merge(context);

        Statistics statistics = context.getStatistics();
        MergeScenarioStatistics fileMergeStats = null;

        for (MergeScenarioStatistics s : statistics.getScenarioStatistics()) {
            MergeScenario<?> scenario = s.getMergeScenario();

            if (scenario.getMergeType() != MergeType.THREEWAY) {
                continue;
            }

            if (scenario.asList().stream().allMatch(o -> o instanceof FileArtifact)) {
                fileMergeStats = s;
            }
        }

        assertNotNull("Could not find the MergeScenarioStatistics containing the FileArtifact merge statistics.", fileMergeStats);

        // TODO check the numbers
    }
}