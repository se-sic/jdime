/**
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
 *     Georg Seibt <seibt@fim.uni-passau.de>
 */
package de.fosd.jdime.merge;

import java.util.logging.Level;
import java.util.logging.Logger;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.matcher.Color;
import de.fosd.jdime.matcher.Matcher;
import de.fosd.jdime.matcher.Matching;
import de.fosd.jdime.matcher.Matchings;

import static de.fosd.jdime.strdump.DumpMode.PLAINTEXT_TREE;

/**
 * TODO: this probably needs an interface to implement as well, as external tools might want to use it.
 *
 * @author Olaf Lessenich
 *
 * @param <T>
 *            type of artifact
 */
public class Diff<T extends Artifact<T>> {

    private static final Logger LOG = Logger.getLogger(Diff.class.getCanonicalName());

    /**
     * Compares two nodes and returns matchings between them and possibly their sub-nodes.
     *
     * @param context <code>MergeContext</code>
     * @param left
     *            left node
     * @param right
     *            right node
     * @param color
     *            color of the matching (for debug output only)
     * @return <code>Matchings</code> of the two nodes
     */
    public Matchings<T> compare(MergeContext context, T left, T right, Color color) {
        Matcher<T> matcher = new Matcher<>();
        Matchings<T> matchings = matcher.match(context, left, right, context.getLookAhead());
        Matching<T> matching = matchings.get(left, right).get();

        LOG.fine(() -> String.format("match(%s, %s) = %d", left.getRevision(), right.getRevision(), matching.getScore()));
        LOG.fine(matcher::getLog);
        LOG.finest("Store matching information within nodes.");

        matcher.storeMatchings(context, matchings, color);

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest(String.format("Dumping matching of %s and %s", left.getRevision(), right.getRevision()));
            System.out.println(matchings);
        }

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine(left.getRevision() + ".dumpTree():");
            System.out.println(left.dump(PLAINTEXT_TREE));

            LOG.fine(right.getRevision() + ".dumpTree():");
            System.out.println(right.dump(PLAINTEXT_TREE));
        }

        return matchings;
    }
}
