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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeTriple;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.stats.Stats;
import de.fosd.jdime.stats.StatsElement;

/**
 * Performs a linebased merge.
 * 
 * @author Olaf Lessenich
 * 
 */
public class LinebasedStrategy extends MergeStrategy<FileArtifact> {

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
	 * @see
	 * de.fosd.jdime.strategy.MergeStrategy#merge(
	 * de.fosd.jdime.common.operations.MergeOperation, 
	 * de.fosd.jdime.common.MergeContext)
	 */
	@Override
	public final void merge(final MergeOperation<FileArtifact> operation,
			final MergeContext context) throws IOException,
			InterruptedException {

		assert (operation != null);
		assert (context != null);

		MergeTriple<FileArtifact> triple = operation.getMergeTriple();

		assert (triple != null);
		assert (triple.isValid()) : "The merge triple is not valid!";
		assert (triple.getLeft() instanceof FileArtifact);
		assert (triple.getBase() instanceof FileArtifact);
		assert (triple.getRight() instanceof FileArtifact);
		assert (triple.getLeft().exists() && !triple.getLeft().isDirectory());
		assert ((triple.getBase().exists() && !triple.getBase().isDirectory()) 
				|| triple.getBase().isEmptyDummy());
		assert (triple.getRight().exists() && !triple.getRight().isDirectory());

		context.resetStreams();

		FileArtifact target = null;

		if (operation.getTarget() != null) {
			assert (operation.getTarget() instanceof FileArtifact);
			target = (FileArtifact) operation.getTarget();
			assert (!target.exists() || target.isEmpty()) 
					: "Would be overwritten: " + target;
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
		boolean conflict = false;
		boolean comment = false;
		int conflicts = 0;
		int loc = 0;
		int cloc = 0;
		int tmp = 0;
		String line = "";
		while ((line = buf.readLine()) != null) {
			context.appendLine(line);
			
			if (context.hasStats()) {
				if (line.matches("^$") || line.matches("^\\s*$") 
						|| line.matches("^\\s*//.*$")) {
	        		// skip empty lines and single line comments
	        		continue;
	        	} else if (line.matches("^\\s*/\\*.*")) {
	        		if (line.matches("^\\s*/\\*.*?\\*/")) {
	        			// one line comment
	        			continue;
	        		} else {
	        			// starting block comment
	        			comment = true;
	        			continue;
	        		}
	        	} else if (line.matches("^.*?\\*/")) {
	        		// ending block comment
	        		comment = false;
	        		continue;
	        	}
	            if (line.matches("^\\s*<<<<<<<.*")) {
	                conflict = true;
	                comment = false;
	                tmp = cloc;
	                conflicts++;
	            } else if (line.matches("^\\s*=======.*")) {
	            	comment = false;
	            } else if (line.matches("^\\s*>>>>>>>.*")) {
	                conflict = false;
	                comment = false;
	                if (tmp == cloc) {
	                	// only conflicting comments or empty lines
	                	conflicts--;
	                }
	            } else {
	                loc++;
	                if (conflict && !comment) {
	                    cloc++;
	                }
	            }
			}
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
			assert (target.exists());
			target.write(context.getStdIn());
		}
		
		// add statistical data to context
		if (context.hasStats()) {
			assert (cloc <= loc);
			
			Stats stats = context.getStats();
			StatsElement linesElement = stats.getElement("lines");
			assert (linesElement != null);
			StatsElement newElement = new StatsElement();
			newElement.setMerged(loc);
			newElement.setConflicting(cloc);
			linesElement.addStatsElement(newElement);
			
			if (conflicts > 0) {
				assert (cloc > 0);
				stats.addConflicts(conflicts);
				StatsElement filesElement = stats.getElement("files");
				assert (filesElement != null);
				filesElement.incrementConflicting();
			} else {
				assert (cloc == 0);
			}
			
		}

	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final Stats createStats() {
		return new Stats(new String[] {"directories", "files", "lines"});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString() {
		return "linebased";
	}

	@Override
	public final String getStatsKey(final FileArtifact artifact) {
		return "lines";
	}

	@Override
	public void dump(final FileArtifact artifact) throws IOException {	
		BufferedReader buf = new BufferedReader(
				new FileReader(artifact.getFile()));
		
		String line = null;
		while ((line = buf.readLine()) != null) {
			System.out.println(line);
			// TODO: save to outputfile
		}
		buf.close();
	}

}
