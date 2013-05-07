/**
 * 
 */
package de.fosd.jdime.common;

/**
 * This class represents a merge scenario.
 * 
 * @author lessenic
 * 
 */
public class MergeTriple {
	/**
	 * Left artifact.
	 */
	private Artifact left;

	/**
	 * Base artifact.
	 */
	private Artifact base;

	/**
	 * Right artifact.
	 */
	private Artifact right;

	/**
	 * Creates a new merge triple.
	 * 
	 * @param left
	 *            artifact
	 * @param base
	 *            artifact
	 * @param right
	 *            artifact
	 */
	public MergeTriple(final Artifact left, final Artifact base,
			final Artifact right) {
		this.left = left;
		this.base = base;
		this.right = right;
	}

	/**
	 * Returns the left artifact.
	 * 
	 * @return the left
	 */
	public final Artifact getLeft() {
		return left;
	}

	/**
	 * Sets the left artifact.
	 * 
	 * @param left
	 *            the left to set
	 */
	public final void setLeft(final Artifact left) {
		this.left = left;
	}

	/**
	 * Returns the base artifact.
	 * 
	 * @return the base
	 */
	public final Artifact getBase() {
		return base;
	}

	/**
	 * Sets the base artifact.
	 * 
	 * @param base
	 *            the base to set
	 */
	public final void setBase(final Artifact base) {
		this.base = base;
	}

	/**
	 * Returns the right artifact.
	 * 
	 * @return the right
	 */
	public final Artifact getRight() {
		return right;
	}

	/**
	 * Sets the right artifact.
	 * 
	 * @param right
	 *            the right to set
	 */
	public final void setRight(final Artifact right) {
		this.right = right;
	}
}
