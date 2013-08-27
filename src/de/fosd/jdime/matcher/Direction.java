/*******************************************************************************
 * Copyright (c) 2013 Olaf Lessenich.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Olaf Lessenich - initial API and implementation
 ******************************************************************************/
package de.fosd.jdime.matcher;

/**
 * A helper class used within the matrix of the LCST matcher.
 * @author lessenic
 *
 */
public enum Direction {

	/**
	 * 
	 */
	LEFT {

		@Override
		public String toString() {
			return "LEFT";
		}
	},
	
	/**
	 * 
	 */
	TOP {

		@Override
		public String toString() {
			return "TOP";
		}
	},
	
	/**
	 * 
	 */
	DIAG {

		@Override
		public String toString() {
			return "DIAG";
		}
	}
}
