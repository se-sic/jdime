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
 * 
 */
public class ArtifactList extends LinkedList<Artifact> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5294838641795231473L;

	/**
	 * Returns true if this list contains the specified element.
	 * 
	 * @param artifact
	 *            element whose presence in this list is to be tested
	 * @return true if this list contains the specified element
	 */
	public final boolean containsRelative(final Artifact artifact) {
		String relativeArtifact = Artifact.computeRelativePath(artifact,
				artifact.getParent());
		
		for (Artifact elem : this) {
			String relativeElem = Artifact.computeRelativePath(elem,
					elem.getParent());

			if (relativeElem.equals(relativeArtifact)) {
				return true;
			}
		}

		return false;
	}
}
