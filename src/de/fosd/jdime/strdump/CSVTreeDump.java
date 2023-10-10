package de.fosd.jdime.strdump;
import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.config.merge.Revision;
import de.fosd.jdime.matcher.matching.Color;
import de.fosd.jdime.matcher.matching.Matching;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

/**
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2018 University of Passau, Germany
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

/**
 * Dumps the given <code>Artifact</code> tree as CSV FILES.
 */

public class CSVTreeDump implements StringDumper {

    private static final String LS = System.lineSeparator();

    /**
     * Appends a plaintext representation of the tree with <code>artifact</code> at its root to the given
     * <code>builder</code>.
     *
     * @param artifact
     *         the <code>Artifact</code> to dump
     * @param getLabel
     *         the <code>Function</code> to use for producing a label an <code>Artifact</code>
     * @param builder
     *         the <code>StringBuilder</code> to append to
     * @param <T>
     *         the type of the <code>Artifact</code>
     */
    private <T extends Artifact<T>> void dumpTree(Artifact<T> artifact, Function<Artifact<T>, String> getLabel
                                                 , StringBuilder builder, String parentID) {
        //File csvFile = new File("AST.csv");
        String[] csvColumns = {"NodeNumber","ParentNumber","NodeType", "NodeID"};


        if (artifact == null) {
            builder.append("NONE");
            builder.append(LS);
            return;
        }
        String id = artifact.getId();

        if (artifact.isChoice() || artifact.isConflict()) {


            builder.append(Color.RED.toShell());

            if (artifact.isChoice()) {
                // isChoice() means that we insert something?
                appendArtifact(artifact, getLabel, builder, "fake:0"); builder.append(LS);

                for (Map.Entry<String, T> entry : artifact.getVariants().entrySet()) {
                    builder.append("#ifdef ").append(entry.getKey()).append(LS);
                    dumpTree(entry.getValue(), getLabel, builder,parentID);
                    builder.append("#endif").append(LS);
                }
            } else if (artifact.isConflict()) {
                Artifact<T> left = artifact.getLeft();
                Artifact<T> right = artifact.getRight();

                //builder.append("<<<<<<<").append(LS);
                dumpTree(left, getLabel, builder,parentID);
                //builder.append("=======").append(LS);
                dumpTree(right, getLabel, builder,parentID);
                //builder.append(">>>>>>>").append(LS);
            }

            builder.append(Color.DEFAULT.toShell());
            return;
        }

        if (artifact.hasMatches()) {
            Iterator<Map.Entry<Revision, Matching<T>>> it = artifact.getMatches().entrySet().iterator();
            Matching<T> firstEntry = it.next().getValue();

            builder.append(firstEntry.getHighlightColor().toShell());
            //id[1]= "both";
            appendArtifact(artifact, getLabel, builder, parentID);


            //int percentage = (int) (firstEntry.getPercentage() * 100);
            //builder.append(String.format(" <(%d, %d%%)> ", firstEntry.getScore(), percentage));


            //appendArtifact(firstEntry.getMatchingArtifact(artifact), getLabel, builder, id2);

            /*it.forEachRemaining(entry -> {
                builder.append(Color.DEFAULT.toShell()).append(", ");
                builder.append(entry.getValue().getHighlightColor().toShell());
                String[] id1 = artifact.getId().split(":");
                //id1[1] = "both";
                //appendArtifact(entry.getValue().getMatchingArtifact(artifact), getLabel, builder, id1);
            });*/

            builder.append(Color.DEFAULT.toShell());
        } else {
            // handle insertion of new lines

            Artifact<T> fake = artifact;
            //fake.setRevision(new Revision("insert:00"));
            id = fake.getId();
            //builder.append(fake.isMerged());
            appendArtifact(fake, getLabel, builder, parentID);
        }

        builder.append(LS);
//rekursion mit dumpTree()
        for (Iterator<T> it = artifact.getChildren().iterator(); it.hasNext(); ) {
            Artifact<T> next = it.next();
            dumpTree(next, getLabel, builder, id);

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
                                                        StringBuilder builder, String parentID) {
        // NodeNr, side, Type, ID, Package,  ParentNodeNr, ParentSide
        // getLabel can also hold Literals etc. we reserve 1 column for ID, the other one can hold
        // values of Literals or packages
        String[] id = artifact.getId().split(":");
        String[] parId = parentID.split(":");

        builder.append(id[1]).append(",").append(id[0]).append(",");

        String[] type = getLabel.apply(artifact).split(" ");
        int len = type.length;
        // make sure that the same amount of entries are done into the table
        // assume that ID is the second entry, if it exists

        for (int i = 0; i < 3; i++) {
            if (i < len) {
                if (i == 1){
                    //builder.append(type[1]).append(",");
                    if(type[1].contains("ID=")) {
                        builder.append(type[1].substring(type[1].indexOf("ID=") + 3 ).replace("\"",""));
                        //.split("\"")[0])
                    }else{
                        builder.append(",").append(type[1]).append(",");
                        break;
                    }
                }else{
                    builder.append(type[i]);

                }

            }
            builder.append(",");
        }

        try {
            builder.append(parId[1]).append(",").append(parId[0]);
        }catch (ArrayIndexOutOfBoundsException e){
            builder.append("Indexoutofbounds").append(parentID);
        }

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
        String id = "target:-1";
        dumpTree(artifact, getLabel, builder,id);

        int lastLS = builder.lastIndexOf(LS);

        if (lastLS != -1) {
            builder.delete(lastLS, lastLS + LS.length());
        }

        return builder.toString();
    }
}

