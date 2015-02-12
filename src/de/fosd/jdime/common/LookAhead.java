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

package de.fosd.jdime.common;

/**
 * @author lessenic
 */
public enum LookAhead {
	/**
	 * Stop looking for subtree matches if the two nodes compared are not equal.
	 */
    OFF,

	/**
	 * Do look at a specified amount of levels of nodes in the subtree if the
	 * compared nodes are not equal. If there are no matches within the
	 * specified number of levels, do not look for matches deeper in the
	 * subtree.
	 */
	PARTIAL,

	/**
	 * Do look at all nodes in the subtree even if the compared nodes are not
	 * equal.
	 */
	FULL;
}
