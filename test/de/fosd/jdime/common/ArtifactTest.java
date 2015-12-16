package de.fosd.jdime.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for the <code>Artifact</code> class.
 */
public class ArtifactTest {

    @Test
    public void testGetMaxDepth() throws Exception {
        TestArtifact artifact = TestTrees.paperTree();

        assertEquals(7, artifact.getMaxDepth());
    }
}