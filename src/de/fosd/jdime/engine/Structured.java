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
import de.fosd.jdime.common.MergeReport;
import de.fosd.jdime.common.MergeTriple;
import de.fosd.jdime.common.MergeType;

/**
 * Performs a structured merge.
 * 
 * @author Olaf Lessenich
 * 
 */
public class Structured implements MergeInterface {

	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(Structured.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.engine.MergeInterface#merge()
	 */
	@Override
	public final MergeReport merge(final MergeType mergeType,
			final MergeTriple triple) {
		// TODO Auto-generated method stub
		LOG.setLevel(Main.getLogLevel());
		LOG.debug("Engine started: " + this.getClass().getName());

		MergeReport report = new MergeReport(mergeType, triple);

		throw new UnsupportedOperationException();
		
		//return report;
	}

}
