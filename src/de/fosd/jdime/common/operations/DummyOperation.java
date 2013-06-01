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
package de.fosd.jdime.common.operations;

import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.NotYetImplementedException;

/**
 * @author Olaf Lessenich
 * 
 */
public class DummyOperation extends Operation {

	/**
	 * 
	 */
	public DummyOperation() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public final void apply(final MergeContext context) {
		throw new NotYetImplementedException();
	}
	
	@Override
	public final String getName() {
		return "DUMMY";
	}

}
