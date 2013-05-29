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
	public DummyReport(Operation operation) {
		super(operation);
		// TODO Auto-generated constructor stub
	}
	
	public DummyReport() {
		super(new DummyOperation());
	}
	
	public String toString() {
		return "DummyReport";
	}

}
