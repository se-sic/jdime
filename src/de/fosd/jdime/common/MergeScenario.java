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
 *     Georg Seibt <seibt@fim.uni-passau.de>
 */
package de.fosd.jdime.common;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.fosd.jdime.common.operations.MergeOperation;

/**
 * This class represents a merge scenario.
 *
 * @param <T> type of artifact
 * @author Olaf Lessenich
 */
public class MergeScenario<T extends Artifact<T>> {

    private static final Logger LOG = Logger.getLogger(MergeScenario.class.getCanonicalName());

    public static final Revision LEFT = new Revision("left");
    public static final Revision BASE = new Revision("base");
    public static final Revision RIGHT = new Revision("right");

    private MergeType mergeType;
    private Map<Revision, T> artifacts;

    /**
     * Constructs a {@link MergeType#TWOWAY} or {@link MergeType#THREEWAY} merge scenario. For all
     * <code>Artifact</code>s the <code>Revision</code> will be (recursively) set to the appropriate static constant
     * defined in this class.
     *
     * @param mergeType
     *         the <code>MergeType</code> for this <code>MergeScenario</code>
     * @param left
     *         the <code>Artifact</code> for the {@link #LEFT} <code>Revision</code>
     * @param base
     *         the <code>Artifact</code> for the {@link #BASE} <code>Revision</code>
     * @param right
     *         the <code>Artifact</code> for the {@link #RIGHT} <code>Revision</code>
     */
    public MergeScenario(MergeType mergeType, T left, T base, T right) {

        if (mergeType != MergeType.TWOWAY && mergeType != MergeType.THREEWAY) {
            LOG.warning(() -> String.format("Constructing a %s MergeScenario using the Left/Base/Right constructor.", mergeType));
        }

        this.artifacts = new LinkedHashMap<>();
        this.mergeType = mergeType;

        left.setRevision(LEFT, true);
        base.setRevision(BASE, true);
        right.setRevision(RIGHT, true);

        this.artifacts.put(left.getRevision(), left);
        logAddition(left);

        this.artifacts.put(base.getRevision(), base);
        logAddition(base);

        this.artifacts.put(right.getRevision(), right);
        logAddition(right);
    }

    /**
     * Constructs a {@link MergeType#NWAY} <code>MergeScenario</code> from the given <code>Artifact</code>s.
     *
     * @param inputArtifacts
     *         the <code>Artifact</code>s participating in the merge
     */
    public MergeScenario(List<T> inputArtifacts) {
        this.artifacts = new LinkedHashMap<>();
        this.mergeType = MergeType.NWAY;

        for (T artifact : inputArtifacts) {
            artifacts.put(artifact.getRevision(), artifact);
            logAddition(artifact);
        }
    }

    /**
     * Logs the addition of the given <code>artifact</code> to this <code>MergeScenario</code>.
     *
     * @param artifact
     *         the added artifact
     */
    private void logAddition(T artifact) {
        LOG.finest(() -> String.format("Adding %s to the MergeScenario.", artifact.getId()));
    }

    private final T get(int position) {
        int i = 0;

        if (LOG.isLoggable(Level.FINEST)) {
            i++;
            LOG.finest("Mergescenario.artifacts:");

            for (Revision rev : artifacts.keySet()) {
                LOG.finest(String.format("[%d] %s", i, artifacts.get(rev).getId()));
            }

            i = 0;
        }

        for (Revision rev : artifacts.keySet()) {
            i++;
            if (i == position) {
                return artifacts.get(rev);
            }
        }

        return null;
    }

    /**
     * Returns the baseRev artifact.
     *
     * @return the baseRev
     */
    public final T getBase() {
        try {
            T base = artifacts.size() == 3 ? get(2) : getLeft().createEmptyArtifact();
            LOG.finest(() -> ("scenario.getBase() returns " + base.getId()));
            return base;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the leftRev artifact.
     *
     * @return the leftRev
     */
    public final T getLeft() {
        T left = get(1);
        LOG.finest(() -> String.format("scenario.getLeft() returns %s", left.getId()));
        return left;
    }

    /**
     * Returns the type of merge.
     *
     * @return type of merge
     */
    public final MergeType getMergeType() {
        return mergeType;
    }

    /**
     * Returns the rightRev artifact.
     *
     * @return the rightRev
     */
    public final T getRight() {
        T right = artifacts.size() == 3 ? get(3) : get(2);
        LOG.finest(() -> String.format("scenario.getRight() returns %s", right.getId()));
        return right;
    }

    /**
     * Returns whether this is a valid merge scenario.
     *
     * @return true if the merge scenario is valid.
     */
    public final boolean isValid() {
        // FIXME: this needs to be reimplemented
        return true;
    }

    /**
     * Run the merge scenario.
     *
     * @param mergeOperation merge operation
     * @param context merge context
     * @throws IOException
     * @throws InterruptedException
     */
    public void run(final MergeOperation mergeOperation, final MergeContext context) throws IOException, InterruptedException {
        // FIXME: I think this could be done easier. It's just too fucking ugly.
        //        We need the first element that was inserted and run the merge on it.
        artifacts.get(artifacts.keySet().iterator().next()).merge(mergeOperation, context);
    }

    /**
     * Sets the baseRev artifact.
     *
     * @param base the baseRev to set
     */
    public final void setBase(final T base) {
        artifacts.put(BASE, base);
    }

    /**
     * Sets the leftRev artifact.
     *
     * @param left the leftRev to set
     */
    public final void setLeft(final T left) {
        artifacts.put(LEFT, left);
    }

    /**
     * Sets the rightRev artifact.
     *
     * @param right the rightRev to set
     */
    public final void setRight(final T right) {
        artifacts.put(RIGHT, right);
    }

    /**
     * Returns a String representing the MergeScenario separated by whitespace.
     *
     * @return String representation
     */
    @Override
    public final String toString() {
        return toString(" ", false);
    }

    /**
     * Returns a String representing the MergeScenario separated by whitespace,
     * omitting empty dummy files.
     *
     * @param humanReadable do not print dummy files if true
     * @return String representation
     */
    public final String toString(final boolean humanReadable) {
        return toString(" ", humanReadable);
    }

    /**
     * Returns a String representing the MergeScenario.
     *
     * @param sep           separator
     * @param humanReadable do not print dummy files if true
     * @return String representation
     */
    public final String toString(final String sep, final boolean humanReadable) {
        StringBuilder sb = new StringBuilder("");

        for (Revision rev : artifacts.keySet()) {
            T artifact = artifacts.get(rev);
            if (!humanReadable || !artifact.isEmpty()) {
                sb.append(artifact.getId()).append(sep);
            }
        }
        return sb.toString();
    }

    /**
     * Returns a list containing all three revisions. Empty dummies for baseRev are
     * included.
     *
     * @return list of artifacts
     */
    public final ArtifactList<T> getList() {
        ArtifactList<T> list = new ArtifactList<>();
        list.add(getLeft());
        list.add(getBase());
        list.add(getRight());
        return list;
    }

    public Map<Revision, T> getArtifacts() {
        return artifacts;
    }
}
