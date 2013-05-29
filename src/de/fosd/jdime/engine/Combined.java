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
package de.fosd.jdime.engine;

import org.apache.log4j.Logger;

import de.fosd.jdime.Main;
import de.fosd.jdime.common.DummyReport;
import de.fosd.jdime.common.MergeOperation;
import de.fosd.jdime.common.MergeReport;

/**
 * Performs a structured merge with auto-tuning.
 * @author Olaf Lessenich
 *
 */
public class Combined implements MergeInterface {
	
	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(Combined.class);

	/* (non-Javadoc)
	 * @see de.fosd.jdime.engine.MergeInterface#merge()
	 */
	@Override
	public final MergeReport merge(final MergeOperation operation) {
		// TODO Auto-generated method stub
		LOG.setLevel(Main.getLogLevel());
		LOG.debug("Engine started: " + this.getClass().getName());
		
		MergeReport report = new DummyReport();

		throw new UnsupportedOperationException();
		
		//return report;
	}

}
