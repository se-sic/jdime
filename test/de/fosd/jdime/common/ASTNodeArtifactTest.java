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
package de.fosd.jdime.common;

import java.io.File;

import de.fosd.jdime.JDimeConfig;
import de.fosd.jdime.JDimeTest;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests the <code>ASTNodeArtifact</code>.
 */
public class ASTNodeArtifactTest extends JDimeTest {

    private static File testFile;

    @BeforeClass
    public static void init() throws Exception {
        JDimeConfig.setLogLevel("WARNING");
        testFile = file("threeway", "left", "SimpleTests", "Bag", "Bag2.java");
    }

    @Test
    public void testASTNodeArtifactFileArtifact() {
        try {
            new ASTNodeArtifact(new FileArtifact(testFile));
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    private static final String prettyPrintExpected = "class Bag {\n" +
                                                      "  int[] values;\n" +
                                                      "  Bag(int[] v) {\n" +
                                                      "    values = v;\n" +
                                                      "  }\n" +
                                                      "  int[] get() {\n" +
                                                      "    return values;\n" +
                                                      "  }\n" +
                                                      "}";

    @Test
    public void testPrettyPrint() {
        try {
            ASTNodeArtifact artifact = new ASTNodeArtifact(new FileArtifact(testFile));
            String prettyPrinted = artifact.prettyPrint();

            assertEquals(prettyPrintExpected, prettyPrinted);
        } catch (Exception e) {
            fail(e.toString());
        }
    }
}
