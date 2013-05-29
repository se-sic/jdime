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

import java.io.IOException;

import de.fosd.jdime.common.MergeReport;
import de.fosd.jdime.common.MergeTriple;
import de.fosd.jdime.common.MergeType;
import de.fosd.jdime.engine.EngineNotFoundException;
import de.fosd.jdime.engine.MergeEngine;

/**
 * @author Olaf Lessenich
 *
 */
public class MergeOperation extends Operation {
	private MergeType mergeType;
	private MergeTriple mergeTriple;
	private MergeEngine engine;
	
	public MergeOperation(MergeType mergeType, MergeTriple mergeTriple, MergeEngine engine) {
		this.mergeType = mergeType;
		this.mergeTriple = mergeTriple;
		this.engine = engine;
	}

	public MergeType getMergeType() {
		return mergeType;
	}

	public MergeTriple getMergeTriple() {
		return mergeTriple;
	}
	
	public String toString() {
		return "MERGE " + mergeType + " " + mergeTriple.toString(true);
	}
	
	public String description() {
		return "Merging " + mergeTriple.toString(true);
	}
	
	public final MergeReport apply() throws EngineNotFoundException, IOException, InterruptedException {
		return engine.merge(this);
	}
}
