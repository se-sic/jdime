/**
 * 
 */
package de.fosd.jdime.matcher;

/**
 * @author Olaf Lessenich
 *
 */
public enum Color {
	/**
	 * 
	 */
	GREEN {
		@Override
		public String toShell() {
			return "\033[32m";
		}
		
		@Override
		public String toGraphViz() {
			return "green";
		}
	}, 
	
	/**
	 * 
	 */
	BLUE {
		@Override
		public String toShell() {
			return "\033[34m";
		}
		
		@Override
		public String toGraphViz() {
			return "blue";
		}
	},
	
	/**
	 * 
	 */
	YELLOW {
		@Override
		public String toShell() {
			return "\033[33m";
		}
		
		@Override
		public String toGraphViz() {
			return "yellow";
		}
	},
	
	/**
	 * 
	 */
	RED {
		@Override
		public String toShell() {
			return "\033[31m";
		}
		
		@Override
		public String toGraphViz() {
			return "red";
		}
	},
	
	/**
	 * 
	 */
	DEFAULT {
		@Override
		public String toShell() {
			return "\033[0m";
		}

		@Override
		public String toGraphViz() {
			return "white";
		}
	};
	
	/**
	 * Returns a String representation that can be interpreted by terminals.
	 * @return String representation to be used in terminals
	 */
	public abstract String toShell();
	
	/**
	 * Returns a String representation that can be interpreted by GraphViz.
	 * @return String representation that can be interpreted by GraphViz
	 */
	public abstract String toGraphViz();
}
