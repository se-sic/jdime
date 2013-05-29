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

import de.fosd.jdime.common.operations.DummyOperation;
import de.fosd.jdime.common.operations.Operation;

/**
 * @author Olaf Lessenich
 *
 */
public class DummyReport extends MergeReport {

	/**
	 * @param operation operation
	 */
	public DummyReport(final Operation operation) {
		super(operation);
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * 
	 */
	public DummyReport() {
		super(new DummyOperation());
	}
	
	/* (non-Javadoc)
	 * @see de.fosd.jdime.common.MergeReport#toString()
	 */
	@Override
	public final String toString() {
		return "DummyReport";
	}

}
