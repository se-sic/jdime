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
package de.fosd.jdime.common.operations;

import java.util.Stack;

import de.fosd.jdime.common.Artifact;

/**
 * @author Olaf Lessenich
 * 
 * @param <T> type of artifact
 *
 */
public class OperationStack<T extends Artifact<T>> extends Stack<Operation<T>> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8820110802865564142L;

}
