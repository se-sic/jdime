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
 * This enum provides several types/"classes" of language elements in Java.
 * <p>
 * We want to distinct between language elements above class declarations
 * (<code>TOPLEVELNODE</code>), elements between class declarations and method
 * bodies (<code>CLASSLEVELNODE</code>), and elements within method bodies
 * <code>METHODLEVELNODE</code>. Additionally, there are the types
 * <code>CLASS</code> and <code>Method</code>, that represent the declarations
 * of classes and methods. Also, there is an element type <code>NODE</code>,
 * which every language element belongs to.
 * <p>
 * This distinction was mainly introduced to gather statistical data.
 *
 * @author lessenic
 */
public enum LangElem {
	/**
	 * Every node.
	 */
	NODE,

	/**
	 * Everything above class declaration, e.g. import statements.
	 */
	TOPLEVELNODE,

	/**
	 * Everything within class declaration but outside methods.
	 */
	CLASSLEVELNODE,

	/**
	 * Everything inside methods.
	 */
	METHODLEVELNODE,

	/**
	 * A class.
	 */
	CLASS,

	/**
	 * A method.
	 */
	METHOD;
}
