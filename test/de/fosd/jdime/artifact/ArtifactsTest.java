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
package de.fosd.jdime.artifact;

import java.util.Arrays;
import java.util.List;

import de.fosd.jdime.stats.KeyEnums;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ArtifactsTest {

    private static TestArtifact traversalRoot;

    private static TestArtifact ta;
    private static TestArtifact tb;
    private static TestArtifact tc;
    private static TestArtifact td;
    private static TestArtifact te;
    private static TestArtifact tf;
    private static TestArtifact tg;

    @BeforeClass
    public static void init() throws Exception {
        ta = new TestArtifact("a", KeyEnums.Type.NODE);
        tb = new TestArtifact("b", KeyEnums.Type.NODE);
        tc = new TestArtifact("c", KeyEnums.Type.NODE);
        td = new TestArtifact("d", KeyEnums.Type.NODE);
        te = new TestArtifact("e", KeyEnums.Type.NODE);
        tf = new TestArtifact("f", KeyEnums.Type.NODE);
        tg = new TestArtifact("g", KeyEnums.Type.NODE);

        ta.addChild(tb);
        ta.addChild(tc);
        ta.addChild(td);

        tb.addChild(te);
        tb.addChild(tf);

        td.addChild(tg);

        traversalRoot = ta;
    }

    @Test
    public void root() throws Exception {
        for (TestArtifact artifact : Arrays.asList(ta, tb, tc, td, te, tf, tg)) {
            assertEquals(traversalRoot, Artifacts.root(artifact));
        }
    }

    @Test
    public void bfs() throws Exception {
        List<TestArtifact> expected = Arrays.asList(ta, tb, tc, td, te, tf, tg);
        assertEquals(expected, Artifacts.bfs(traversalRoot));
    }

    @Test
    public void dfs() throws Exception {
        List<TestArtifact> expected = Arrays.asList(ta, tb, te, tf, tc, td, tg);
        assertEquals(expected, Artifacts.dfs(traversalRoot));
    }
}