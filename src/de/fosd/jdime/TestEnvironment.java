/*******************************************************************************
 * Copyright (C) 2013 Olaf Lessenich.
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
 *     Olaf Lessenich - initial API and implementation
 ******************************************************************************/

package de.fosd.jdime;

import org.gnu.glpk.GLPK;

/**
 *
 * @author Olaf Lessenich
 */
public final class TestEnvironment {
	/**
	 * 
	 */
	private TestEnvironment() {
		
	}
	
    /**
     * 
     */
    public static void run() {
        GLPK.glp_create_prob();
        System.out.println(TestEnvironment.class.getCanonicalName() + ": OK");
    }
}
