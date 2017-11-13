/**
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2017 University of Passau, Germany
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
 *     Georg Seibt <seibt@fim.uni-passau.de>
 */
package de.fosd.jdime.matcher.unordered;

import java.util.logging.Logger;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.matcher.Matcher;
import de.fosd.jdime.matcher.MatcherInterface;
import de.fosd.jdime.matcher.matching.Matchings;

/**
 * <code>UnorderedMatcher</code>s ignore the order of the elements they match when comparing <code>Artifact</code>s.
 *
 * @param <T>
 *         the type of the <code>Artifact</code>s
 * @author Olaf Lessenich
 */
public abstract class UnorderedMatcher<T extends Artifact<T>> implements MatcherInterface<T> {

    protected static final Logger LOG = Logger.getLogger(Matcher.class.getCanonicalName());

    /**
     * The matcher is used for recursive matching calls. It can determine whether the order of artifacts is essential.
     */
    protected MatcherInterface<T> matcher;

    /**
     * Constructs a new <code>UnorderedMatcher</code> using the given <code>matcher</code> for recursive calls.
     *
     * @param matcher
     *         the parent <code>MatcherInterface</code>
     */
    public UnorderedMatcher(MatcherInterface<T> matcher) {
        this.matcher = matcher;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Compares <code>left</code> and <code>right</code> while ignoring the order of the elements.
     */
    @Override
    public abstract Matchings<T> match(MergeContext context, T left, T right);
}
