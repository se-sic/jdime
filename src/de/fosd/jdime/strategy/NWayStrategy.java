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
package de.fosd.jdime.strategy;

import java.io.FileWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.fosd.jdime.artifact.ast.ASTNodeArtifact;
import de.fosd.jdime.artifact.file.FileArtifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.config.merge.MergeScenario;
import de.fosd.jdime.config.merge.MergeType;
import de.fosd.jdime.config.merge.Revision;
import de.fosd.jdime.operations.MergeOperation;

import static de.fosd.jdime.config.merge.MergeScenario.BASE;
import static de.fosd.jdime.strdump.DumpMode.GRAPHVIZ_TREE;
import static de.fosd.jdime.strdump.DumpMode.PLAINTEXT_TREE;

/**
 * Performs a structured merge on <code>FileArtifacts</code>.
 *
 * @author Olaf Lessenich
 */
public class NWayStrategy extends MergeStrategy<FileArtifact> {

    private static final Logger LOG = Logger.getLogger(NWayStrategy.class.getCanonicalName());

    /**
     * The source <code>FileArtifacts</code> are extracted from the
     * <code>MergeOperation</code>, parsed by the <code>JastAddJ</code> parser
     * into abstract syntax trees, and on the fly encapsulated into
     * <code>ASTNodeArtifacts</code>.
     * <p>
     * A new <code>MergeOperation</code>, encapsulating
     * <code>ASTNodeArtifacts</code> as source and target nodes, is created and applied.
     * <p>
     * TODO: more high-level documentation.
     */
    @Override
    public void merge(MergeOperation<FileArtifact> operation, MergeContext context) {
        MergeScenario<FileArtifact> scenario = operation.getMergeScenario();
        Map<Revision, FileArtifact> variants = scenario.getArtifacts();

        assert (variants.size() > 1);

        /* ASTNodeArtifacts are created from the input files.
         * Then, a ASTNodeStrategy can be applied.
         * The result is pretty printed and can be written into the output file.
         */
        ASTNodeArtifact merged, next, targetNode;
        MergeContext mergeContext;

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Merging:");
            for (Revision rev : variants.keySet()) {
                LOG.fine(String.format("%s: %s", rev, variants.get(rev).getFile().getPath()));
            }
        }

        Iterator<Revision> it = variants.keySet().iterator();
        targetNode = new ASTNodeArtifact(variants.get(it.next()));

        while (it.hasNext()) {
            merged = targetNode;
            next = new ASTNodeArtifact(variants.get(it.next()));

            try {
                long cmdStart = System.currentTimeMillis();

                mergeContext = context;
                targetNode = merged.copy();

                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest(String.format("Plaintext tree dump of target node:%n%s", targetNode.dump(PLAINTEXT_TREE)));
                }

                MergeScenario<ASTNodeArtifact> astScenario = new MergeScenario<>(MergeType.TWOWAY, merged, merged.createEmptyArtifact(BASE), next);
                MergeOperation<ASTNodeArtifact> astMergeOp = new MergeOperation<>(astScenario, targetNode);

                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest("ASTMOperation.apply(context)");
                }

                astMergeOp.apply(mergeContext);
                long runtime = System.currentTimeMillis() - cmdStart;
                LOG.fine(() -> String.format("%s merge time was %d ms.", getClass().getSimpleName(), runtime));

                if (!context.isDiffOnly()) {
                    operation.getTarget().setContent(targetNode.prettyPrint());
                }

                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest("Structured merge finished.");

                    if (!context.isDiffOnly()) {
                        String dump = targetNode.dump(PLAINTEXT_TREE);
                        LOG.finest(String.format("Plaintext tree dump of target node:%n%s", dump));
                    }

                    LOG.finest(String.format("Pretty-printing merged:%n%s", merged.prettyPrint()));
                    LOG.finest(String.format("Pretty-printing next:%n%s", next.prettyPrint()));

                    if (!context.isDiffOnly()) {
                        LOG.finest(String.format("Pretty-printing target:%n%s", targetNode.prettyPrint()));
                    }
                }

                if (LOG.isLoggable(Level.FINE)) {

                    try (FileWriter fw = new FileWriter(merged + ".dot")) {
                        fw.write(targetNode.dump(GRAPHVIZ_TREE));
                    }
                }
            } catch (Throwable t) {
                LOG.severe("Exception while merging:");
                context.addCrash(scenario, t);

                for (Revision rev : variants.keySet()) {
                    LOG.severe(String.format("%s: %s", rev, variants.get(rev).getFile().getPath()));
                }
                LOG.severe(t.toString());

                if (!context.isKeepGoing()) {
                    throw new Error(t);
                }
            }
        }
    }
}
