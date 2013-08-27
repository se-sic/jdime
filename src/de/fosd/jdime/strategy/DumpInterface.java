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
package de.fosd.jdime.strategy;

import java.io.IOException;

import de.fosd.jdime.common.Artifact;

/**
 * @author Olaf Lessenich
 *
 * @param <T>
 */
public interface DumpInterface<T extends Artifact<T>> {
	/**
	 * @param artifact artifact to dump
	 * @param graphical output option
	 * @throws IOException 
	 */
	void dump(final T artifact, final boolean graphical) throws IOException;
}
