/**
 * 
 */
package de.fosd.jdime.common;

/**
 * @author lessenic
 *
 */
public class MergeOperation extends Operation {
	private MergeType mergeType;
	private MergeTriple mergeTriple;
	
	public MergeOperation(MergeType mergeType, MergeTriple mergeTriple) {
		this.mergeType = mergeType;
		this.mergeTriple = mergeTriple;
	}

	public MergeType getMergeType() {
		return mergeType;
	}

	public MergeTriple getMergeTriple() {
		return mergeTriple;
	}
	
	public String toString() {
		return "Merging " + mergeTriple.toString(true);
	}
}
