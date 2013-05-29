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
package de.fosd.jdime.common;

/**
 * This class specifies the different methods to handle directories.
 * 
 * @author Olaf Lessenich
 *
 */
public enum DirectoryHandling {
	/**
	 * Handle directories within JDime.
	 */
	INTERNAL, 
	
	/**
	 * Let external merge engines handle the directories.
	 */
	EXTERNAL;
}
