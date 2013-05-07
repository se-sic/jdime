/**
 * 
 */
package de.fosd.jdime.engine;

/**
 * This Exception can be thrown if a merge engine is not found. 
 * @author lessenic
 *
 */
public class EngineNotFoundException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7644611893570243018L;

	/**
	 * 
	 */
	public EngineNotFoundException() {
	}

	/**
	 * @param message 
	 */
	public EngineNotFoundException(final String message) {
		super(message);
	}

}
