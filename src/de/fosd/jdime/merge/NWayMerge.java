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

        import java.io.IOException;
        import java.util.Iterator;
        import java.util.List;
        import java.util.Objects;

        import de.fosd.jdime.common.*;
        import de.fosd.jdime.common.operations.ConflictOperation;
        import de.fosd.jdime.common.operations.DeleteOperation;
        import de.fosd.jdime.common.operations.MergeOperation;
        import de.fosd.jdime.common.operations.Operation;
        import de.fosd.jdime.matcher.Color;
        import de.fosd.jdime.matcher.Matching;
        import org.apache.commons.lang3.ClassUtils;
        import org.apache.log4j.Logger;

/**
 * @author Olaf Lessenich
 *
 * @param <T>
 *            type of artifact
 */
public class NWayMerge<T extends Artifact<T>> {

    private static final Logger LOG = Logger.getLogger(ClassUtils
            .getShortClassName(Merge.class));
    private UnorderedMerge<T> unorderedMerge = null;
    private OrderedMerge<T> orderedMerge = null;
    private String logprefix;

    /**
     * TODO: this needs high-level explanation.
     *
     * @param context
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public final void merge(final ArtifactList<T> inputArtifacts,
                            final MergeContext context) throws IOException,
            InterruptedException {
        logprefix = "NWayMerge - ";

        Diff<T> diff = new Diff<>();

        Matching<T> m;

        T merged = inputArtifacts.get(0);

        for (int i = 1; i < inputArtifacts.size(); i++) {
        }
    }

    /**
     * Returns the logging prefix.
     *
     * @return logging prefix
     */
    private String prefix() {
        return logprefix;
    }

    /**
     * Returns the logging prefix.
     *
     * @param artifact
     *            artifact that is subject of the logging
     * @return logging prefix
     */
    private String prefix(final T artifact) {
        return logprefix + "[" + artifact.getId() + "] ";
    }
}
