/**
 * 
 */
package de.fosd.jdime.common;

/**
 * @author lessenic
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
