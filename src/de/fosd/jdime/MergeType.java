package de.fosd.jdime;

/**
 * @author lessenic
 *
 */
public enum MergeType {
	TWOWAY(2, "left", "right"), THREEWAY(3, "left", "base", "right");
	
	private int numFiles;
	
	private String[] revisions;
	
	MergeType(int numFiles, String ... revisions) {
		this.numFiles = numFiles;
		this.revisions = revisions;
	}
	
	public String getRevision(int pos) {
		return revisions[pos];
	}
	
	public int getNumFiles() {
		return numFiles;
	}
}
