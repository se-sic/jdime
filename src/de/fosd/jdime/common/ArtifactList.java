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
package de.fosd.jdime.common;

import java.util.LinkedList;

/**
 * @author Olaf Lessenich
 * @param <E> artifact element
 */
public class ArtifactList<E extends Artifact<E>> extends LinkedList<E> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5294838641795231473L;

	/* (non-Javadoc)
	 * @see java.util.AbstractCollection#toString()
	 */
	@Override
	public final String toString() {
		return toString(" ");
	}

	/**
	 * Returns a String representing a list of artifacts.
	 * 
	 * @param sep
	 *            separator
	 * @return String representation
	 */
	public final String toString(final String sep) {
		assert (sep != null);

		StringBuilder sb = new StringBuilder("");
		for (E element : this) {
			sb.append(element.getId());
			sb.append(sep);
		}

		return sb.toString();
	}
}
