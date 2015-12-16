package de.fosd.jdime.stats;

import java.util.Arrays;

import de.fosd.jdime.JDimeTest;
import de.fosd.jdime.Main;
import de.fosd.jdime.common.ArtifactList;
import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeScenario;
import de.fosd.jdime.common.MergeType;
import de.fosd.jdime.common.Revision;
import de.fosd.jdime.strategy.MergeStrategy;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
                break;
            }
        }

        assertNotNull("Could not find the MergeScenarioStatistics containing the FileArtifact merge statistics.", fileMergeStats);

        for (Revision rev : Arrays.asList(MergeScenario.LEFT, MergeScenario.BASE, MergeScenario.RIGHT)) {
            assertTrue(fileMergeStats.getMergeScenario().getArtifacts().keySet().stream().anyMatch(r -> r.equals(rev)));
        }

        // Level Statistics

        {
            ElementStatistics elStats = fileMergeStats.getLevelStatistics(MergeScenario.LEFT, KeyEnums.Level.METHOD);
            assertEquals(30, elStats.getTotal());
        }

        {
            ElementStatistics elStats = fileMergeStats.getLevelStatistics(MergeScenario.LEFT, KeyEnums.Level.CLASS);
            assertEquals(12, elStats.getTotal());
        }

        {
            ElementStatistics elStats = fileMergeStats.getLevelStatistics(MergeScenario.LEFT, KeyEnums.Level.TOP);
            assertEquals(5, elStats.getTotal());
        }

        {
            ElementStatistics elStats = fileMergeStats.getLevelStatistics(MergeScenario.RIGHT, KeyEnums.Level.METHOD);
            assertEquals(31, elStats.getTotal());
        }

        {
            ElementStatistics elStats = fileMergeStats.getLevelStatistics(MergeScenario.RIGHT, KeyEnums.Level.CLASS);
            assertEquals(12, elStats.getTotal());
        }

        {
            ElementStatistics elStats = fileMergeStats.getLevelStatistics(MergeScenario.RIGHT, KeyEnums.Level.TOP);
            assertEquals(5, elStats.getTotal());
        }

        {
            ElementStatistics elStats = fileMergeStats.getLevelStatistics(MergeScenario.TARGET, KeyEnums.Level.METHOD);
            assertEquals(44, elStats.getTotal());
        }

        {
            ElementStatistics elStats = fileMergeStats.getLevelStatistics(MergeScenario.TARGET, KeyEnums.Level.CLASS);
            assertEquals(12, elStats.getTotal());
        }

        {
            ElementStatistics elStats = fileMergeStats.getLevelStatistics(MergeScenario.TARGET, KeyEnums.Level.TOP);
            assertEquals(5, elStats.getTotal());
        }

        // Type Statistics

        {
            ElementStatistics elStats = fileMergeStats.getTypeStatistics(MergeScenario.LEFT, KeyEnums.Type.CLASS);
            assertEquals(1, elStats.getTotal());
        }

        {
            ElementStatistics elStats = fileMergeStats.getTypeStatistics(MergeScenario.LEFT, KeyEnums.Type.METHOD);
            assertEquals(2, elStats.getTotal());
        }

        {
            ElementStatistics elStats = fileMergeStats.getTypeStatistics(MergeScenario.LEFT, KeyEnums.Type.NODE);
            assertEquals(44, elStats.getTotal());
        }

        {
            ElementStatistics elStats = fileMergeStats.getTypeStatistics(MergeScenario.RIGHT, KeyEnums.Type.CLASS);
            assertEquals(1, elStats.getTotal());
        }

        {
            ElementStatistics elStats = fileMergeStats.getTypeStatistics(MergeScenario.RIGHT, KeyEnums.Type.METHOD);
            assertEquals(2, elStats.getTotal());
        }

        {
            ElementStatistics elStats = fileMergeStats.getTypeStatistics(MergeScenario.RIGHT, KeyEnums.Type.NODE);
            assertEquals(45, elStats.getTotal());
        }

        {
            ElementStatistics elStats = fileMergeStats.getTypeStatistics(MergeScenario.TARGET, KeyEnums.Type.CLASS);
            assertEquals(1, elStats.getTotal());
        }

        {
            ElementStatistics elStats = fileMergeStats.getTypeStatistics(MergeScenario.TARGET, KeyEnums.Type.METHOD);
            assertEquals(3, elStats.getTotal());
        }

        {
            ElementStatistics elStats = fileMergeStats.getTypeStatistics(MergeScenario.TARGET, KeyEnums.Type.NODE);
            assertEquals(57, elStats.getTotal());
        }

        // Merge Statistics

        {
            MergeStatistics mStats = fileMergeStats.getMergeStatistics(MergeScenario.LEFT);
            assertEquals(1, mStats.getNumChunks());
            assertEquals(13.0, mStats.getAvgChunkSize(), 0.0001);
            assertEquals(13, mStats.getMaxASTDepth());
            assertEquals(5, mStats.getMaxNumChildren());
        }

        {
            MergeStatistics mStats = fileMergeStats.getMergeStatistics(MergeScenario.RIGHT);
            assertEquals(1, mStats.getNumChunks());
            assertEquals(14.0, mStats.getAvgChunkSize(), 0.0001);
            assertEquals(14, mStats.getMaxASTDepth());
            assertEquals(5, mStats.getMaxNumChildren());
        }

        {
            MergeStatistics mStats = fileMergeStats.getMergeStatistics(MergeScenario.TARGET);
            assertEquals(1, mStats.getNumChunks());
            assertEquals(27.0, mStats.getAvgChunkSize(), 0.0001);
            assertEquals(14, mStats.getMaxASTDepth());
            assertEquals(5, mStats.getMaxNumChildren());
        }

        // Line Statistics

        {
            ElementStatistics lineStats = fileMergeStats.getLineStatistics();
            assertEquals(12, lineStats.getTotal());
        }

        // File Statistics

        {
            ElementStatistics fileStats = fileMergeStats.getFileStatistics();
            assertEquals(1, fileStats.getNumMerged());
        }

        // Directory Statistics

        {
            ElementStatistics dirStats = fileMergeStats.getDirectoryStatistics();
            assertEquals(0, dirStats.getTotal());
            assertEquals(0, dirStats.getNumAdded());
            assertEquals(0, dirStats.getNumMerged());
            assertEquals(0, dirStats.getNumDeleted());
            assertEquals(0, dirStats.getNumOccurInConflict());
        }
    }
}