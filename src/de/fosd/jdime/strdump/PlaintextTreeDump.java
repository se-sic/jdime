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
package de.fosd.jdime.strdump;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.config.merge.Revision;
import de.fosd.jdime.matcher.matching.Color;
import de.fosd.jdime.matcher.matching.Matching;

/**
 * Dumps the given <code>Artifact</code> tree as indented plaintext.
 */
public class PlaintextTreeDump implements StringDumper {

    private static final String LS = System.lineSeparator();

    /**
     * Appends a plaintext representation of the tree with <code>artifact</code> at its root to the given
     * <code>builder</code>.
     *
     * @param artifact
     *         the <code>Artifact</code> to dump
     * @param getLabel
     *         the <code>Function</code> to use for producing a label an <code>Artifact</code>
     * @param prefix
     *         the prefix to append before the given <code>artifact</code>
     * @param childPrefix
     *         the prefix to append before all children of the given <code>artifact</code>
     * @param builder
     *         the <code>StringBuilder</code> to append to
     * @param <T>
     *         the type of the <code>Artifact</code>
     */
    private <T extends Artifact<T>> void dumpTree(Artifact<T> artifact, Function<Artifact<T>, String> getLabel,
                                                  String prefix, String childPrefix, StringBuilder builder) {

        if (artifact.isChoice() || artifact.isConflict()) {
            String emptyPrefix = replicate(" ", childPrefix.length());
            String emptyChildPrefix = emptyPrefix + "    ";

            builder.append(Color.RED.toShell());
            builder.append(prefix); appendArtifact(artifact, getLabel, builder); builder.append(LS);

            if (artifact.isChoice()) {

                for (Map.Entry<String, T> entry : artifact.getVariants().entrySet()) {
                    builder.append("#ifdef ").append(entry.getKey()).append(LS);
                    dumpTree(entry.getValue(), getLabel, emptyPrefix, emptyChildPrefix, builder);
                    builder.append("#endif").append(LS);
                }
            } else if (artifact.isConflict()) {
                Artifact<T> left = artifact.getLeft();
                Artifact<T> right = artifact.getRight();

                builder.append("<<<<<<<").append(LS);
                dumpTree(left, getLabel, emptyPrefix, emptyChildPrefix, builder);
                builder.append("=======").append(LS);
                dumpTree(right, getLabel, emptyPrefix, emptyChildPrefix, builder);
                builder.append(">>>>>>>").append(LS);
            }

            builder.append(Color.DEFAULT.toShell());
            return;
        }

        if (artifact.hasMatches()) {
            Iterator<Map.Entry<Revision, Matching<T>>> it = artifact.getMatches().entrySet().iterator();
            Matching<T> firstEntry = it.next().getValue();

            builder.append(firstEntry.getHighlightColor().toShell()).append(prefix);

            appendArtifact(artifact, getLabel, builder);

            int percentage = (int) (firstEntry.getPercentage() * 100);
            builder.append(String.format(" <(%d, %d%%)> ", firstEntry.getScore(), percentage));
            appendArtifact(firstEntry.getMatchingArtifact(artifact), getLabel, builder);

            it.forEachRemaining(entry -> {
                builder.append(Color.DEFAULT.toShell()).append(", ");
                builder.append(entry.getValue().getHighlightColor().toShell());
                appendArtifact(entry.getValue().getMatchingArtifact(artifact), getLabel, builder);
            });

            builder.append(Color.DEFAULT.toShell());
        } else {
            builder.append(prefix);
            appendArtifact(artifact, getLabel, builder);
        }

        builder.append(LS);

        for (Iterator<T> it = artifact.getChildren().iterator(); it.hasNext(); ) {
            Artifact<T> next = it.next();

            builder.append(childPrefix);

            char c = next.hasChildren() ? '┬' : '─';

            if (it.hasNext()) {
                dumpTree(next, getLabel, "├──" + c, childPrefix + "│  ", builder);
            } else {
                dumpTree(next, getLabel, "└──" + c, childPrefix + "   ", builder);
            }
        }
    }

    /**
     * Appends the representation of the given <code>Artifact</code> to the <code>builder</code>.
     *
     * @param artifact
     *         the <code>Artifact</code> to append to the <code>builder</code>
     * @param getLabel
     *         the <code>Function</code> to use for producing a label for the <code>Artifact</code>
     * @param builder
     *         the <code>StringBuilder</code> to append to
     * @param <T>
     *         the type of the <code>Artifact</code>
     */
    private <T extends Artifact<T>> void appendArtifact(Artifact<T> artifact, Function<Artifact<T>, String> getLabel,
                                                        StringBuilder builder) {

        builder.append("(").append(artifact.getId()).append(") ");
        builder.append(getLabel.apply(artifact));
    }

    /**
     * Replicates the given <code>String</code> <code>n</code> times and returns the concatenation.
     *
     * @param s
     *         the <code>String</code> to replicate
     * @param n
     *         the number of replications
     * @return the concatenation
     */
    private static String replicate(String s, int n) {
        return new String(new char[n]).replace("\0", s).intern();
    }

    @Override
    public <T extends Artifact<T>> String dump(Artifact<T> artifact, Function<Artifact<T>, String> getLabel) {
        StringBuilder builder = new StringBuilder();

        dumpTree(artifact, getLabel, "", "", builder);

        int lastLS = builder.lastIndexOf(LS);

        if (lastLS != -1) {
            builder.delete(lastLS, lastLS + LS.length());
        }

        return builder.toString();
    }
}
