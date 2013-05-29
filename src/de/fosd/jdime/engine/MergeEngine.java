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

import java.io.IOException;

import de.fosd.jdime.common.MergeReport;
import de.fosd.jdime.common.operations.MergeOperation;

/**
 * @author Olaf Lessenich
 * 
 */
public enum MergeEngine {
	/**
	 * Performs a textual, line-based merge.
	 */
	LINEBASED {
		/**
		 * 
		 */
		private String name = "linebased";

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return name;
		}
	},

	/**
	 * Performs a syntactic, AST-based merge.
	 */
	STRUCTURED {
		/**
		 * 
		 */
		private String name = "structured";

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return name;
		}
	},

	/**
	 * Performs a linebased merge. If conflicts are found, a structured merge is
	 * performed as well.
	 */
	COMBINED {
		/**
		 * 
		 */
		private String name = "combined";

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return name;
		}
	};

	/**
	 * Parses a String and returns a MergeEngine. Null is returned if no
	 * appropriate Tool is found.
	 * 
	 * @param str
	 *            name of the merge tool
	 * @return MergeEngine
	 * @throws EngineNotFoundException
	 *             if the given String cannot be matched to a merge engine
	 */
	public static MergeEngine parse(final String str)
			throws EngineNotFoundException {
		assert str != null : "MergeEngine may not be null!";

		String input = str.toLowerCase();

		switch (input) {
		case "linebased":
			return MergeEngine.LINEBASED;
		case "structured":
			return MergeEngine.STRUCTURED;
		case "combined":
			return MergeEngine.COMBINED;
		default:
			throw new EngineNotFoundException("Engine missing for " + str
					+ " merge.");
		}
	}

	/**
	 * Performs a merge operation.
	 * 
	 * @param mergeType
	 *            type of merge
	 * @param triple
	 *            the merge triple
	 * @return merge report
	 * @throws EngineNotFoundException
	 *             if the merge engine cannot be found
	 * @throws IOException
	 *             IOException
	 * @throws InterruptedException
	 *             InterruptedException
	 * 
	 */
	public MergeReport merge(final MergeOperation operation) throws EngineNotFoundException,
			IOException, InterruptedException {
		assert operation.getMergeType() != null : "MergeType may not be null!";
		assert operation.getMergeTriple() != null : "list of input files may not be null";

		MergeInterface engine = null;

		switch (this) {
		case LINEBASED:
			engine = new Linebased();
			break;
		case STRUCTURED:
			engine = new Structured();
			break;
		case COMBINED:
			engine = new Combined();
			break;
		default:
			throw new EngineNotFoundException("Engine missing for " + operation.getMergeType()
					+ " merge.");
		}

		return engine.merge(operation);
	}

}
