/*******************************************************************************
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
 *******************************************************************************/
package de.fosd.jdime.matcher.unordered;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.matcher.Matcher;
import de.fosd.jdime.matcher.Matching;
import de.fosd.jdime.matcher.MatchingInterface;

/**
 * @author Olaf Lessenich
 *
 * @param <T>
 *            type of artifact
 *
 */
public abstract class UnorderedMatcher<T extends Artifact<T>> implements
		MatchingInterface<T> {

	/**
	 * The matcher is used for recursive matching calls. It can determine
	 * whether the order of artifacts is essential.
	 */
	protected Matcher<T> matcher;

	/**
	 * Creates a new instance of UnorderedMatcher.
	 *
	 * @param matcher
	 *            matcher
	 */
	public UnorderedMatcher(final Matcher<T> matcher) {
		this.matcher = matcher;
	}

	/**
	 * Compares two nodes while ignoring the order of the elements.
	 *
	 * @param left
	 *            left tree
	 * @param right
	 *            right tree
	 * @return largest common subtree of left and right tree
	 */
	@Override
	public abstract Matching<T> match(final T left, final T right);
}
