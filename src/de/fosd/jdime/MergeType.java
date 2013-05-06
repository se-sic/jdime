package de.fosd.jdime;

/**
 * @author lessenic
 *
 */
public enum MergeType {
	TWOWAY(2, "left", "right"), THREEWAY(3, "left", "base", "right");
	
	private int numFiles;

	/**
	 * At least two input files are needed.
	 */
	public static final int MINFILES = 2;

	/**
	 * More than three input files are not supported at the moment.
	 */
	public static final int MAXFILES = 3;
	
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
