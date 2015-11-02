package de.fosd.jdime.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by Georg on 02.11.2015.
 */
public class ArtifactTest {

    @Test
    public void testGetMaxDepth() throws Exception {
        TestArtifact artifact = TestTrees.paperTree();

        assertEquals(7, artifact.getMaxDepth());
    }
}