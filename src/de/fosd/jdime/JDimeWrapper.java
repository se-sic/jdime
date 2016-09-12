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
package de.fosd.jdime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.fosd.jdime.common.ArtifactList;
import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.Revision;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.common.operations.Operation;
import de.fosd.jdime.strategy.MergeStrategy;
import de.fosd.jdime.strategy.NWayStrategy;
import de.fosd.jdime.strategy.StructuredStrategy;
import org.apache.commons.io.FilenameUtils;

public class JDimeWrapper {
    private static final Logger LOG = Logger.getLogger(JDimeWrapper.class.getCanonicalName());

    public static void main(String[] args) throws IOException, InterruptedException {
        // setup logging
        Logger root = Logger.getLogger(JDimeWrapper.class.getPackage().getName());
        root.setLevel(Level.WARNING);

        for (Handler handler : root.getHandlers()) {
            handler.setLevel(Level.WARNING);
        }

        // setup JDime using the MergeContext
        MergeContext context = new MergeContext();
        context.setPretend(true);
        context.setQuiet(false);

        // prepare the list of input files
        ArtifactList<FileArtifact> inputArtifacts = new ArtifactList<>();

        for (String filename : args) {
            try {
                File file = new File(filename);

                // the revision name, this will be used as condition for ifdefs
                // as an example, I'll just use the filenames
                Revision rev = new Revision(FilenameUtils.getBaseName(file.getPath()));
                FileArtifact newArtifact = new FileArtifact(rev, file);

                inputArtifacts.add(newArtifact);
            } catch (FileNotFoundException e) {
                System.err.println("Input file not found: " + filename);
            }
        }

        context.setInputFiles(inputArtifacts);

        // setup strategies
        MergeStrategy<FileArtifact> structured = new StructuredStrategy();
        MergeStrategy<FileArtifact> conditional = new NWayStrategy();

        // run the merge first with structured strategy to see whether there are conflicts
        context.setMergeStrategy(structured);
        context.collectStatistics(true);
        Operation<FileArtifact> merge = new MergeOperation<>(context.getInputFiles(), context.getOutputFile(), null, null, context.isConditionalMerge());
        merge.apply(context);

        // if there are no conflicts, run the conditional strategy
        if (context.getStatistics().hasConflicts()) {
            context = new MergeContext(context);
            context.collectStatistics(false);
            context.setMergeStrategy(conditional);
            // use regular merging outside of methods
            context.setConditionalOutsideMethods(false);
            // we have to recreate the operation because now we will do a conditional merge
            merge = new MergeOperation<>(context.getInputFiles(), context.getOutputFile(), null, null, context.isConditionalMerge());
            merge.apply(context);
        }
    }
}
