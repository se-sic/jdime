package de.fosd.jdime;

import java.util.Collection;
import java.util.Iterator;

/**
 * Contains static utility methods.
 */
public final class Util {

	private Util() {}

	/**
	 * Joins the results of the {@link Object#toString()} method of all <code>objects</code> into one
	 * <code>String</code> using the supplied separator character. <code>null</code> values in the <code>objects</code>
	 * array will be represented as an empty <code>String</code>.
	 *
	 * @param objects
	 * 		the objects whose <code>String</code> representations are to be joined
	 * @param sep
	 * 		the separator character to use
	 * @return the resulting <code>String</code>
	 */
	public static String joinToString(Object[] objects, char sep) {
		StringBuilder b = new StringBuilder(objects.length * 16);

		for (int i = 0; i < objects.length; i++) {

			if (objects[i] != null) {
				b.append(objects[i]);
			}

			if (i != objects.length - 1) {
				b.append(sep);
			}
		}

		return b.toString();
	}

	/**
	 * Joins the results of the {@link Object#toString()} method of all <code>objects</code> into one
	 * <code>String</code> using the supplied separator character. <code>null</code> values in the <code>objects</code>
	 * collection will be represented as an empty <code>String</code>.
	 *
	 * @param objects
	 * 		the objects whose <code>String</code> representations are to be joined
	 * @param sep
	 * 		the separator character to use
	 * @return the resulting <code>String</code>
	 */
	public static String joinToString(Collection<?> objects, char sep) {
		StringBuilder b = new StringBuilder(objects.size() * 16);

		for (Iterator<?> it = objects.iterator(); it.hasNext(); ) {
			Object s = it.next();

			if (s != null) {
				b.append(s);
			}

			if (it.hasNext()) {
				b.append(sep);
			}
		}

		return b.toString();
	}
}
