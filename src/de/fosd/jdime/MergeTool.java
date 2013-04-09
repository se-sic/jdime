/**
 * 
 */
package de.fosd.jdime;

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
	 * @param str name of the merge tool
	 * @return MergeTool
	 */
	public static MergeTool parse(final String str) {
		String input = str.toLowerCase();
		switch (input) {
		case "linebased":
			return MergeTool.LINEBASED;
		case "structured":
			return MergeTool.STRUCTURED;
		case "combined":
			return MergeTool.COMBINED;
		default:
			break;
		}
		return null;
	}

}
