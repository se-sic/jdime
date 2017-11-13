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
package de.fosd.jdime.matcher.cost_model;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.matcher.Matcher;
import de.fosd.jdime.matcher.matching.Color;

/**
 * The possible modes determining how the <code>CostModelMatcher</code> is used in the matching process.
 */
public enum CMMode {
    /**
     * The <code>CostModelMatcher</code> is not used.
     */
    OFF,

    /**
     * The <code>CostModelMatcher</code> will be the only matcher used to produce matchings in
     * {@link Matcher#match(MergeContext, Color)}.
     */
    REPLACEMENT,

    /**
     * After the matchings were calculated in {@link Matcher#match(MergeContext, Color)} they are
     * fixed and a matching between the unmatched artifacts is calculated using the <code>CostModelMatcher</code>.
     */
    POST_PROCESSOR,

    /**
     * Whenever a subtree could not be matched fully during the execution of
     * {@link Matcher#match(MergeContext, Artifact, Artifact)}, the <code>CostModelMatcher</code> is used to attempt
     * to refine the matchings.
     */
    INTEGRATED
}
