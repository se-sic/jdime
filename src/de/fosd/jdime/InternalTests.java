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
 *     Georg Seibt <seibt@fim.uni-passau.de>
 */
package de.fosd.jdime;

import org.gnu.glpk.GLPK;

/**
 * Contains static methods testing the functionality of various aspects of the program.
 * 
 * @author Olaf Lessenich
 */
public final class InternalTests {

    private static String delimiter = "====================================================";

    /**
     * Utility class constructor.
     */
    private InternalTests() {

    }

    /**
     * Runs all internal tests.
     */
    public static void run() {
        runEnvironmentTest();
    }

    /**
     * Checks whether the environment for the program is correctly configured. Particularly this verifies that
     * the native libraries are working. 
     */
    public static void runEnvironmentTest() {
        
        try {
            System.out.println("Library search path: ");

            String[] split = System.getProperty("java.library.path").split(";");
            for (int i = 0; i < split.length; i++) {
                System.out.println(i + ": " + split[i]);
            }
            System.out.println();
            
            System.out.println("GLPK " + GLPK.glp_version() +  " is working.");
            System.out.println(InternalTests.class.getCanonicalName() + " : OK");
        } catch (Throwable t) {
            System.out.println(t);
            System.out.println(InternalTests.class.getCanonicalName() + " : FAILED");
            
            throw t;
        }
    }
}
