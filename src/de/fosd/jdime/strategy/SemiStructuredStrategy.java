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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import de.fosd.jdime.artifact.ast.ASTNodeArtifact;
import de.fosd.jdime.artifact.ast.SemiStructuredArtifact;
import de.fosd.jdime.artifact.file.FileArtifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.operations.MergeOperation;
import de.fosd.jdime.stats.KeyEnums;

import static de.fosd.jdime.stats.KeyEnums.Type.BLOCK;

public class SemiStructuredStrategy extends StructuredStrategy {

    private static final Logger LOG = Logger.getLogger(SemiStructuredStrategy.class.getCanonicalName());

    /**
     * Regex used to split a String into lines while retaining the original line separators.
     */
    private static Pattern LINES = Pattern.compile("(?<=\\R)");

    @Override
    public void merge(MergeOperation<FileArtifact> operation, MergeContext context) {
        boolean oldSemiStructured = context.isSemiStructured();

        context.setSemiStructured(true);
        super.merge(operation, context);
        context.setSemiStructured(oldSemiStructured);
    }

    static ASTNodeArtifact makeSemiStructured(ASTNodeArtifact root, KeyEnums.Level level, FileArtifact original) {
        List<ASTNodeArtifact> toReplace = collectBlocks(root, level, new ArrayList<>());
        String[] lines = LINES.split(original.getContent());

        for (ASTNodeArtifact artifact : toReplace) {

            SemiStructuredArtifact replacement;

            try {
                // The SemiStructuredArtifact constructor inserts the new SemiStructuredArtifact into the tree.
                replacement = new SemiStructuredArtifact(artifact, lines);
            } catch (SemiStructuredArtifact.NotReplaceableException e) {
                LOG.log(Level.FINE, e, () -> {
                    Optional<ASTNodeArtifact> enclosing = artifact.enclosingClassOrMethod();
                    String msg = "Skipping replacement of " + artifact.getId() + " (" + artifact + ")";

                    if (enclosing.isPresent()) {
                        msg += " under " + enclosing.get().getId() + " (" + enclosing.get() + ")";
                    }

                    return msg;
                });
                continue;
            }

            if (artifact == root) {
                root = replacement;
            }
        }

        return root;
    }

    private static List<ASTNodeArtifact> collectBlocks(ASTNodeArtifact artifact, KeyEnums.Level level, List<ASTNodeArtifact> blocks) {

        if (artifact.getType() == BLOCK && artifact.getLevel() == level) {
            blocks.add(artifact);
        } else {
            artifact.getChildren().forEach(c -> collectBlocks(c, level, blocks));
        }

        return blocks;
    }
}
