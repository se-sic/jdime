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

/**
 * @author Olaf Lessenich
 *
 */
public class DeleteOperation extends Operation {
	private Artifact artifact;
	
	public Artifact getArtifact() {
		return artifact;
	}

	public DeleteOperation(Artifact artifact) {
		this.artifact = artifact;
	}
	
	
	public String toString() {
		return "Deleting " + artifact.toString();
	}
}
