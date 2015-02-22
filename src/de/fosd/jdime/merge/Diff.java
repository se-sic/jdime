/*
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2015 University of Passau, Germany
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 *
 * Contributors:
 *     Olaf Lessenich <lessenic@fim.uni-passau.de>
 */
package de.fosd.jdime.merge;

import org.apache.commons.lang3.ClassUtils;
import org.apache.log4j.Logger;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.matcher.Color;
import de.fosd.jdime.matcher.Matcher;
import de.fosd.jdime.matcher.Matching;

/**
 * TODO: this probably needs an interface to implement as well, as external tools might want to use it.
 *
 * @author Olaf Lessenich
 * 
 * @param <T>
 *            type of artifact
 */
public class Diff<T extends Artifact<T>> {
	private static final Logger LOG = Logger.getLogger(ClassUtils
			.getShortClassName(Diff.class));

	/**
	 * Compares two nodes and returns a respective matching.
	 * 
	 * @param left
	 *            left node
	 * @param right
	 *            right node
	 * @param color
	 *            color of the matching (for debug output only)
	 * @return Matching of the two nodes
	 */
	public final Matching<T> compare(final T left, final T right,
			final Color color) {
		Matcher<T> matcher = new Matcher<>();
		Matching<T> m = matcher.match(left, right);

		if (LOG.isDebugEnabled()) {
			LOG.debug("match(" + left.getRevision() + ", "
					+ right.getRevision() + ") = " + m.getScore());
			LOG.debug(matcher.getLog());
			LOG.trace("Store matching information within nodes.");
		}

		matcher.storeMatching(m, color);
		if (LOG.isTraceEnabled()) {
			LOG.trace("Dumping matching of " + left.getRevision() + " and "
					+ right.getRevision());
			System.out.println(m.dumpTree());
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug(left.getRevision() + ".dumpTree():");
			System.out.println(left.dumpTree());

			LOG.debug(right.getRevision() + ".dumpTree():");
			System.out.println(right.dumpTree());
		}

		return m;
	}
}
