package de.fosd.jdime.common;

/**
 * @author lessenic
 *
 */
public enum MergeType {
	/**
	 * Two-way merge.
	 */
	TWOWAY(2, "left", "right"), 
	
	/**
	 * Three-way merge.
	 */
	THREEWAY(3, "left", "base", "right");
	
	/**
	 * Number of required input files.
	 */
	private int numFiles;

	/**
	 * At least two input files are needed.
	 */
	public static final int MINFILES = 2;

	/**
	 * More than three input files are not supported at the moment.
	 */
	public static final int MAXFILES = 3;
	
	/**
	 * Names of input revisions.
	 */
	private String[] revisions;
	
	/**
	 * Creates a new instance of MergeType.
	 * @param numFiles number of required input files
	 * @param revisions names of input revisions
	 */
	MergeType(final int numFiles, final String ... revisions) {
		this.numFiles = numFiles;
		this.revisions = revisions;
	}
	
	/**
	 * Returns revision name of the input file at a certain position.
	 * @param pos position of the input file
	 * @return revision name
	 */
	public String getRevision(final int pos) {
		return revisions[pos];
	}
	
	/**
	 * Returns number of required input files.
	 * @return required input files.
	 */
	public int getNumFiles() {
		return numFiles;
	}
}
