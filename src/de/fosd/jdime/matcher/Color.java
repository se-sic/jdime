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
