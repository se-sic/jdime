package de.fosd.jdime.matcher;

/**
 * A helper class used within the matrix of the LCST matcher.
 * @author lessenic
 *
 */
public enum Direction {

	/**
	 * 
	 */
	LEFT {

		@Override
		public String toString() {
			return "LEFT";
		}
	},
	
	/**
	 * 
	 */
	TOP {

		@Override
		public String toString() {
			return "TOP";
		}
	},
	
	/**
	 * 
	 */
	DIAG {

		@Override
		public String toString() {
			return "DIAG";
		}
	}
}