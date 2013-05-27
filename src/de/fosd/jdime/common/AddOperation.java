/**
 * 
 */
package de.fosd.jdime.common;

/**
 * @author lessenic
 *
 */
public class AddOperation extends Operation {
	private Artifact artifact;
	
	public Artifact getArtifact() {
		return artifact;
	}

	public AddOperation(Artifact artifact) {
		this.artifact = artifact;
	}
	
	public String toString() {
		return "Adding " + artifact.toString();
	}
}
