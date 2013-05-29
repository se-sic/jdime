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
/**
 * 
 */
package de.fosd.jdime.engine;

/**
 * This Exception can be thrown if a merge engine is not found. 
 * @author Olaf Lessenich
 *
 */
public class EngineNotFoundException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7644611893570243018L;

	/**
	 * 
	 */
	public EngineNotFoundException() {
	}

	/**
	 * @param message 
	 */
	public EngineNotFoundException(final String message) {
		super(message);
	}

}
