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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import de.fosd.jdime.Main;
import de.fosd.jdime.common.MergeReport;
import de.fosd.jdime.common.operations.MergeOperation;

/**
 * Performs a linebased merge.
 * 
 * @author Olaf Lessenich
 * 
 */
public class Linebased implements MergeInterface {

	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(Linebased.class);

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
	public final MergeReport merge(final MergeOperation operation)
			throws IOException, InterruptedException {
		LOG.setLevel(Main.getLogLevel());
		LOG.debug("Engine started: " + this.getClass().getName());
		LOG.debug(operation.getMergeType() + " merge will be performed.");

		MergeReport report = new MergeReport(operation);

		String cmd = BASECMD + " " + operation.getMergeTriple();

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
			report.appendLine(line);
		}

		buf.close();

		// process error stream
		buf = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
		while ((line = buf.readLine()) != null) {
			report.appendErrorLine(line);
		}

		buf.close();
		pr.getInputStream().close();
		pr.getErrorStream().close();
		pr.getOutputStream().close();

		pr.waitFor();

		long cmdStop = System.currentTimeMillis();

		LOG.debug("External command has finished after " + (cmdStop - cmdStart)
				+ " ms.");

		if (report.hasErrors()) {
			System.err.println(report.getStdErr());
		}

		return report;
	}

}
