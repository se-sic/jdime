/**
 * 
 */
package de.fosd.jdime.common;

import de.fosd.jdime.common.operations.DummyOperation;
import de.fosd.jdime.common.operations.Operation;

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
