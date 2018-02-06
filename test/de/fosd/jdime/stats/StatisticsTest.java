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
package de.fosd.jdime.stats;

import java.util.Arrays;
import java.util.List;

import de.fosd.jdime.JDimeTest;
import de.fosd.jdime.Main;
import de.fosd.jdime.artifact.ArtifactList;
import de.fosd.jdime.artifact.file.FileArtifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.config.merge.MergeScenario;
import de.fosd.jdime.config.merge.MergeType;
import de.fosd.jdime.config.merge.Revision;
import de.fosd.jdime.strategy.MergeStrategy;
import org.junit.Before;
import org.junit.Test;

import static de.fosd.jdime.artifact.file.FileArtifact.FileType.FILE;
import static de.fosd.jdime.strategy.MergeStrategy.STRUCTURED;
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
        List<FileArtifact> inputArtifacts = new ArtifactList<>();
        String filePath = "SimpleTests/Bag/Bag.java";

        inputArtifacts.add(new FileArtifact(MergeScenario.LEFT, file(leftDir, filePath)));
        inputArtifacts.add(new FileArtifact(MergeScenario.BASE, file(baseDir, filePath)));
        inputArtifacts.add(new FileArtifact(MergeScenario.RIGHT, file(rightDir, filePath)));

        context.setMergeStrategy(MergeStrategy.parse(STRUCTURED).get());
        context.setInputFiles(inputArtifacts);
        context.setOutputFile(new FileArtifact(MergeScenario.MERGE, FILE));

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
            assertEquals(13, elStats.getNumAdded());
            assertEquals(17, elStats.getNumMerged());
            assertEquals(0, elStats.getNumDeleted());
            assertEquals(0, elStats.getNumOccurInConflict());
        }

        {
            ElementStatistics elStats = fileMergeStats.getLevelStatistics(MergeScenario.LEFT, KeyEnums.Level.CLASS);
            assertEquals(15, elStats.getTotal());
            assertEquals(0, elStats.getNumAdded());
            assertEquals(15, elStats.getNumMerged());
            assertEquals(0, elStats.getNumDeleted());
            assertEquals(0, elStats.getNumOccurInConflict());
        }

        {
            ElementStatistics elStats = fileMergeStats.getLevelStatistics(MergeScenario.LEFT, KeyEnums.Level.TOP);
            assertEquals(5, elStats.getTotal());
            assertEquals(0, elStats.getNumAdded());
            assertEquals(5, elStats.getNumMerged());
            assertEquals(0, elStats.getNumDeleted());
            assertEquals(0, elStats.getNumOccurInConflict());
        }

        {
            ElementStatistics elStats = fileMergeStats.getLevelStatistics(MergeScenario.RIGHT, KeyEnums.Level.METHOD);
            assertEquals(31, elStats.getTotal());
            assertEquals(14, elStats.getNumAdded());
            assertEquals(17, elStats.getNumMerged());
            assertEquals(0, elStats.getNumDeleted());
            assertEquals(0, elStats.getNumOccurInConflict());
        }

        {
            ElementStatistics elStats = fileMergeStats.getLevelStatistics(MergeScenario.RIGHT, KeyEnums.Level.CLASS);
            assertEquals(15, elStats.getTotal());
            assertEquals(0, elStats.getNumAdded());
            assertEquals(15, elStats.getNumMerged());
            assertEquals(0, elStats.getNumDeleted());
            assertEquals(0, elStats.getNumOccurInConflict());
        }

        {
            ElementStatistics elStats = fileMergeStats.getLevelStatistics(MergeScenario.RIGHT, KeyEnums.Level.TOP);
            assertEquals(5, elStats.getTotal());
            assertEquals(0, elStats.getNumAdded());
            assertEquals(5, elStats.getNumMerged());
            assertEquals(0, elStats.getNumDeleted());
            assertEquals(0, elStats.getNumOccurInConflict());
        }

        {
            ElementStatistics elStats = fileMergeStats.getLevelStatistics(MergeScenario.TARGET, KeyEnums.Level.METHOD);
            assertEquals(44, elStats.getTotal());
            assertEquals(27, elStats.getNumAdded());
            assertEquals(0, elStats.getNumMerged());
            assertEquals(0, elStats.getNumDeleted());
            assertEquals(0, elStats.getNumOccurInConflict());
        }

        {
            ElementStatistics elStats = fileMergeStats.getLevelStatistics(MergeScenario.TARGET, KeyEnums.Level.CLASS);
            assertEquals(15, elStats.getTotal());
            assertEquals(0, elStats.getNumAdded());
            assertEquals(0, elStats.getNumMerged());
            assertEquals(0, elStats.getNumDeleted());
            assertEquals(0, elStats.getNumOccurInConflict());
        }

        {
            ElementStatistics elStats = fileMergeStats.getLevelStatistics(MergeScenario.TARGET, KeyEnums.Level.TOP);
            assertEquals(5, elStats.getTotal());
            assertEquals(0, elStats.getNumAdded());
            assertEquals(0, elStats.getNumMerged());
            assertEquals(0, elStats.getNumDeleted());
            assertEquals(0, elStats.getNumOccurInConflict());
        }

        // Type Statistics

        {
            ElementStatistics elStats = fileMergeStats.getTypeStatistics(MergeScenario.LEFT, KeyEnums.Type.CLASS);
            assertEquals(1, elStats.getTotal());
            assertEquals(0, elStats.getNumAdded());
            assertEquals(1, elStats.getNumMerged());
            assertEquals(0, elStats.getNumDeleted());
            assertEquals(0, elStats.getNumOccurInConflict());
        }

        {
            ElementStatistics elStats = fileMergeStats.getTypeStatistics(MergeScenario.LEFT, KeyEnums.Type.METHOD);
            assertEquals(2, elStats.getTotal());
            assertEquals(1, elStats.getNumAdded());
            assertEquals(1, elStats.getNumMerged());
            assertEquals(0, elStats.getNumDeleted());
            assertEquals(0, elStats.getNumOccurInConflict());
        }

        {
            ElementStatistics elStats = fileMergeStats.getTypeStatistics(MergeScenario.LEFT, KeyEnums.Type.BLOCK);
            assertEquals(2, elStats.getTotal());
            assertEquals(1, elStats.getNumAdded());
            assertEquals(1, elStats.getNumMerged());
            assertEquals(0, elStats.getNumDeleted());
            assertEquals(0, elStats.getNumOccurInConflict());
        }

        {
            ElementStatistics elStats = fileMergeStats.getTypeStatistics(MergeScenario.LEFT, KeyEnums.Type.NODE);
            assertEquals(45, elStats.getTotal());
            assertEquals(11, elStats.getNumAdded());
            assertEquals(34, elStats.getNumMerged());
            assertEquals(0, elStats.getNumDeleted());
            assertEquals(0, elStats.getNumOccurInConflict());
        }

        {
            ElementStatistics elStats = fileMergeStats.getTypeStatistics(MergeScenario.RIGHT, KeyEnums.Type.CLASS);
            assertEquals(1, elStats.getTotal());
            assertEquals(0, elStats.getNumAdded());
            assertEquals(1, elStats.getNumMerged());
            assertEquals(0, elStats.getNumDeleted());
            assertEquals(0, elStats.getNumOccurInConflict());
        }

        {
            ElementStatistics elStats = fileMergeStats.getTypeStatistics(MergeScenario.RIGHT, KeyEnums.Type.METHOD);
            assertEquals(2, elStats.getTotal());
            assertEquals(1, elStats.getNumAdded());
            assertEquals(1, elStats.getNumMerged());
            assertEquals(0, elStats.getNumDeleted());
            assertEquals(0, elStats.getNumOccurInConflict());
        }

        {
            ElementStatistics elStats = fileMergeStats.getTypeStatistics(MergeScenario.RIGHT, KeyEnums.Type.BLOCK);
            assertEquals(2, elStats.getTotal());
            assertEquals(1, elStats.getNumAdded());
            assertEquals(1, elStats.getNumMerged());
            assertEquals(0, elStats.getNumDeleted());
            assertEquals(0, elStats.getNumOccurInConflict());
        }

        {
            ElementStatistics elStats = fileMergeStats.getTypeStatistics(MergeScenario.RIGHT, KeyEnums.Type.NODE);
            assertEquals(46, elStats.getTotal());
            assertEquals(12, elStats.getNumAdded());
            assertEquals(34, elStats.getNumMerged());
            assertEquals(0, elStats.getNumDeleted());
            assertEquals(0, elStats.getNumOccurInConflict());
        }

        {
            ElementStatistics elStats = fileMergeStats.getTypeStatistics(MergeScenario.TARGET, KeyEnums.Type.CLASS);
            assertEquals(1, elStats.getTotal());
            assertEquals(0, elStats.getNumAdded());
            assertEquals(0, elStats.getNumDeleted());
            assertEquals(0, elStats.getNumOccurInConflict());
        }

        {
            ElementStatistics elStats = fileMergeStats.getTypeStatistics(MergeScenario.TARGET, KeyEnums.Type.METHOD);
            assertEquals(3, elStats.getTotal());
            assertEquals(2, elStats.getNumAdded());
            assertEquals(0, elStats.getNumDeleted());
            assertEquals(0, elStats.getNumOccurInConflict());
        }

        {
            ElementStatistics elStats = fileMergeStats.getTypeStatistics(MergeScenario.TARGET, KeyEnums.Type.BLOCK);
            assertEquals(3, elStats.getTotal());
            assertEquals(2, elStats.getNumAdded());
            assertEquals(0, elStats.getNumMerged());
            assertEquals(0, elStats.getNumDeleted());
            assertEquals(0, elStats.getNumOccurInConflict());
        }

        {
            ElementStatistics elStats = fileMergeStats.getTypeStatistics(MergeScenario.TARGET, KeyEnums.Type.NODE);
            assertEquals(57, elStats.getTotal());
            assertEquals(23, elStats.getNumAdded());
            assertEquals(0, elStats.getNumDeleted());
            assertEquals(0, elStats.getNumOccurInConflict());
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
            assertEquals(0, lineStats.getNumAdded());
            assertEquals(0, lineStats.getNumMerged());
            assertEquals(0, lineStats.getNumDeleted());
            assertEquals(0, lineStats.getNumOccurInConflict());
        }

        // File Statistics

        {
            ElementStatistics fileStats = fileMergeStats.getFileStatistics();
            assertEquals(0, fileStats.getTotal()); // TODO file total is not counted currently, figure out where
            assertEquals(0, fileStats.getNumAdded());
            assertEquals(1, fileStats.getNumMerged());
            assertEquals(0, fileStats.getNumDeleted());
            assertEquals(0, fileStats.getNumOccurInConflict());
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