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
package de.fosd.jdime.strategy;

import java.io.IOException;
import java.util.logging.Logger;

import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeScenario;
import de.fosd.jdime.common.NotYetImplementedException;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.merge.Merge;
import de.fosd.jdime.stats.Stats;

/**
 * @author Olaf Lessenich
 *
 */
public class DirectoryStrategy extends MergeStrategy<FileArtifact> {

    private static final Logger LOG = Logger.getLogger(DirectoryStrategy.class.getCanonicalName());
    private static Merge<FileArtifact> merge = null;

    /**
     * TODO: high-level documentation
     *
     * @param operation the <code>MergeOperation</code> to perform
     * @param context the <code>MergeContext</code>
     */
    @Override
    public void merge(MergeOperation<FileArtifact> operation, MergeContext context) {
        assert (operation != null);
        assert (context != null);
        assert (context.isRecursive()) : "Recursive merging needs to "
                + "be enabled in order to merge directories. "
                + "Use '-r' or see '-help'!";

        MergeScenario<FileArtifact> triple = operation.getMergeScenario();

        assert (triple.isValid());

        assert (triple.getLeft() instanceof FileArtifact);
        assert (triple.getBase() instanceof FileArtifact);
        assert (triple.getRight() instanceof FileArtifact);

        FileArtifact left = triple.getLeft();
        FileArtifact base = triple.getBase();
        FileArtifact right = triple.getRight();

        FileArtifact[] revisions = { left, base, right };

        for (FileArtifact dir : revisions) {
            assert ((dir.exists() && dir.isDirectory()) || dir.isEmpty());
        }

        if (merge == null) {
            merge = new Merge<>();
        }

        LOG.finest(() -> String.format("Merging using operation %s and context %s", operation, context));
        merge.merge(operation, context);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public final Stats createStats() {
        return new Stats(new String[] { "directories", "files" });
    }

    @Override
    public final String toString() {
        return "directory";
    }

    @Override
    public final String getStatsKey(final FileArtifact artifact) {
        return artifact.isDirectory() ? "directories" : "files";
    }

    @Override
    public final String dumpTree(final FileArtifact artifact,
            final boolean graphical) throws IOException {
        throw new NotYetImplementedException("TODO: print directory tree");
    }

    @Override
    public final String dumpFile(final FileArtifact artifact, final boolean graphical)
            throws IOException {
        throw new NotYetImplementedException("TODO: print content of all files");
    }
}
