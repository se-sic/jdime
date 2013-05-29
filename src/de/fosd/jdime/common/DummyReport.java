/**
 * 
 */
package de.fosd.jdime.common;

/**
 * @author lessenic
 *
 */
public class DummyReport extends MergeReport {

	/**
	 * @param mergeType
	 * @param mergeTriple
	 */
	public DummyReport(MergeType mergeType, MergeTriple mergeTriple) {
		super(mergeType, mergeTriple);
		// TODO Auto-generated constructor stub
	}
	
	public DummyReport() {
		super(null, null);
	}
	
	public String toString() {
		return "DummyReport";
	}

}
