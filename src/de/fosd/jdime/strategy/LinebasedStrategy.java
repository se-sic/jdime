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
package de.fosd.jdime.strategy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeTriple;
import de.fosd.jdime.common.operations.MergeOperation;

/**
 * Performs a linebased merge.
 * 
 * @author Olaf Lessenich
 * 
 */
public class LinebasedStrategy extends MergeStrategy {

	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(LinebasedStrategy.class);

	/**
	 * Constant prefix of the base merge command.
	 */
	private static final String BASECMD = "merge -q -p";

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.engine.MergeInterface#merge()
	 */
	@Override
	public final void merge(final MergeOperation operation, 
			final MergeContext context)
			throws IOException, InterruptedException {
		
		assert (operation != null);
		assert (context != null);
		
		MergeTriple triple = operation.getMergeTriple();
		
		assert (triple != null);
		assert (triple.isValid()) 
					: "The merge triple is not valid!";
		assert (triple.getLeft() instanceof FileArtifact);
		assert (triple.getBase() instanceof FileArtifact);
		assert (triple.getRight() instanceof FileArtifact);
		assert (triple.getLeft().isLeaf());
		assert (triple.getBase().isLeaf() || triple.getBase().isEmptyDummy());
		assert (triple.getRight().isLeaf());
		
		FileArtifact target = null;
		
		if (operation.getTarget() != null) {
			assert (operation.getTarget() instanceof FileArtifact);
			target = (FileArtifact) operation.getTarget();
		}
		
		String cmd = BASECMD + " " + triple;
		

		// launch the merge process by invoking GNU merge (rcs has to be
		// installed)
		LOG.debug("Running external command: " + cmd);

		long cmdStart = System.currentTimeMillis();

		Runtime run = Runtime.getRuntime();
		Process pr = run.exec(cmd.toString());

		// process input stream
		BufferedReader buf = new BufferedReader(new InputStreamReader(
				pr.getInputStream()));
		String line = "";
		while ((line = buf.readLine()) != null) {
			context.appendLine(line);
		}

		buf.close();

		// process error stream
		buf = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
		while ((line = buf.readLine()) != null) {
			context.appendErrorLine(line);
		}

		buf.close();
		pr.getInputStream().close();
		pr.getErrorStream().close();
		pr.getOutputStream().close();

		pr.waitFor();

		long cmdStop = System.currentTimeMillis();

		LOG.debug("External command has finished after " + (cmdStop - cmdStart)
				+ " ms.");

		if (context.hasErrors()) {
			System.err.println(context.getStdErr());
		}
		
		// write output
		if (target != null) {
			target.write(context.getReader());
		}
		
	}

}
