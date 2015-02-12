/*******************************************************************************
 * Copyright (C) 2013, 2014 Olaf Lessenich.
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
 *******************************************************************************/

package de.fosd.jdime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import de.fosd.jdime.common.ASTNodeArtifact;
import de.fosd.jdime.common.FileArtifact;

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
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
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
			String expected = "class Bag {\n  int[] values;\n  Bag(int[] v) {\n    super();\n    values = v;\n  }\n  int[] get() {\n    return values;\n  }\n}";
			assertEquals(expected, prettyPrinted);
		} catch (Exception e) {
			fail(e.toString());
		}
	}

}
