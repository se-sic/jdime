/**
 * 
 */
package de.fosd.jdime;

import java.io.File;
import java.io.IOException;
import java.util.List;

import de.fosd.jdime.merge.Combined;
import de.fosd.jdime.merge.Linebased;
import de.fosd.jdime.merge.MergeInterface;
import de.fosd.jdime.merge.Structured;

/**
 * @author lessenic
 * 
 */
public enum MergeTool {
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
	 * Parses a String and returns a MergeTool. Null is returned if no
	 * appropriate Tool is found.
	 * 
	 * @param str
	 *            name of the merge tool
	 * @return MergeTool
	 * @throws EngineNotFoundException
	 */
	public static MergeTool parse(final String str)
			throws EngineNotFoundException {
		assert str != null : "MergeTool may not be null!";

		String input = str.toLowerCase();

		switch (input) {
		case "linebased":
			return MergeTool.LINEBASED;
		case "structured":
			return MergeTool.STRUCTURED;
		case "combined":
			return MergeTool.COMBINED;
		default:
			throw new EngineNotFoundException("Engine missing for " + str
					+ " merge.");
		}
	}

	/**
	 * @return 
	 * @throws EngineNotFoundException
	 * @throws IOException 
	 * @throws InterruptedException 
	 * 
	 */
	public MergeReport merge(MergeType mergeType, List<File> inputFiles)
			throws EngineNotFoundException, IOException, InterruptedException {
		assert mergeType != null : "MergeType may not be null!";
		assert inputFiles != null : "list of input files may not be null";

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
			throw new EngineNotFoundException("Engine missing for " + mergeType
					+ " merge.");
		}

		return engine.merge(mergeType, inputFiles);
	}

}
