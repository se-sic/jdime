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
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.fosd.jdime.common.ASTNodeArtifact;
import de.fosd.jdime.common.FileArtifact;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Olaf Lessenich
 *
 */
public class PrettyPrintTest {

    /**
     * Sets up the test fixture. (Called before every test case method.)
     */
    @Before
    public void setUp() {
        // initialize logger
        Logger root = Logger.getLogger(JDimeWrapper.class.getPackage().getName());
        root.setLevel(Level.WARNING);

        for (Handler handler : root.getHandlers()) {
            handler.setLevel(Level.WARNING);
        }
    }

    /**
     * Test method for
     * {@link de.fosd.jdime.common.ASTNodeArtifact#ASTNodeArtifact(de.fosd.jdime.common.FileArtifact)}
     * .
     */
    @Test
    public final void testASTNodeArtifactFileArtifact() {
        try {
            new ASTNodeArtifact(new FileArtifact(new File(
                    "testfiles/left/SimpleTests/Bag/Bag2.java")));
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * Test method for
     * {@link de.fosd.jdime.common.ASTNodeArtifact#prettyPrint()}.
     */
    @Test
    public final void testPrettyPrint() {
        try {
            ASTNodeArtifact artifact = new ASTNodeArtifact(new FileArtifact(
                    new File("testfiles/left/SimpleTests/Bag/Bag2.java")));
            String prettyPrinted = artifact.prettyPrint();
            String expected = "class Bag {\n  int[] values;\n  Bag(int[] v) {\n    values = v;\n  }\n  int[] get() {\n    return values;\n  }\n}";
            assertEquals(expected, prettyPrinted);
        } catch (Exception e) {
            fail(e.toString());
        }
    }

}
