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
package de.fosd.jdime.config.merge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.artifact.ArtifactList;

/**
 * A <code>MergeScenario</code> collects the <code>Artifact</code>s that are participating in the merge and stores its
 * <code>MergeType</code>.
 *
 * @param <T>
 *         the type of the <code>Artifact</code>s being merged
 * @author Olaf Lessenich
 * @author Georg Seibt
 */
public class MergeScenario<T extends Artifact<T>> {

    private static final Logger LOG = Logger.getLogger(MergeScenario.class.getCanonicalName());

    public static final Revision LEFT = new Revision("left");
    public static final Revision BASE = new Revision("base");
    public static final Revision RIGHT = new Revision("right");
    public static final Revision TARGET = new Revision("target");
    public static final Revision MERGE = new Revision("merge");
    public static final Revision CONFLICT = new Revision("conflict");
    public static final Revision CHOICE = new Revision("choice");

    private MergeType mergeType;
    private Map<Revision, T> artifacts;

    /**
     * Constructs a {@link MergeType#TWOWAY} or {@link MergeType#THREEWAY} merge scenario.
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

        this.artifacts.put(left.getRevision(), left);
        this.artifacts.put(base.getRevision(), base);
        this.artifacts.put(right.getRevision(), right);
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
        }
    }

    /**
     * Performs a shallow copy (the actual Artifacts that are part of the MergeScenario will not be copied).
     *
     * @param toCopy
     *         the <code>MergeScenario</code> to copy
     */
    public MergeScenario(MergeScenario<T> toCopy) {
        this.mergeType = toCopy.mergeType;
        this.artifacts = new HashMap<>(toCopy.artifacts);
    }

    /**
     * Returns the <code>MergeType</code> of this <code>MergeScenario</code>.
     *
     * @return the <code>MergeType</code>
     */
    public MergeType getMergeType() {
        return mergeType;
    }

    /**
     * Returns an unmodifiable view of the <code>Map</code> used to store the <code>Artifact</code>s in this
     * <code>MergeScenario</code>.
     *
     * @return the <code>Artifact</code>s in this <code>MergeScenario</code>
     */
    public Map<Revision, T> getArtifacts() {
        return Collections.unmodifiableMap(artifacts);
    }

    /**
     * Returns the n-th <code>Artifact</code> that was added to this <code>MergeScenario</code>. Will return
     * <code>null</code> if <code>n</code> is invalid (not smaller than the number of artifacts in the scenario).
     *
     * @param n the number of the artifact to return
     * @return the n-th <code>Artifact</code> of this <code>MergeScenario</code> by insertion order
     */
    public T get(int n) {
        int i = 0;
        T artifact = null;

        for (Map.Entry<Revision, T> entry : artifacts.entrySet()) {

            if (i == n) {
                artifact = entry.getValue();
            }

            i++;
        }

        return artifact;
    }

    /**
     * Returns the left <code>Artifact</code>.
     *
     * @return the left <code>Artifact</code>
     */
    public T getLeft() {
        return artifacts.get(LEFT);
    }

    /**
     * Sets the left <code>Artifact</code> to the new value.
     *
     * @param left the new left <code>Artifact</code>
     */
    public void setLeft(T left) {
        artifacts.put(LEFT, left);
    }

    /**
     * Returns the base <code>Artifact</code>.
     *
     * @return the base <code>Artifact</code>
     */
    public T getBase() {
        return artifacts.get(BASE);
    }

    /**
     * Sets the base <code>Artifact</code> to the new value.
     *
     * @param base the new base <code>Artifact</code>
     */
    public void setBase(T base) {
        artifacts.put(BASE, base);
    }

    /**
     * Returns the right <code>Artifact</code>.
     *
     * @return the right <code>Artifact</code>
     */
    public T getRight() {
        return artifacts.get(RIGHT);
    }

    /**
     * Sets the right <code>Artifact</code> to the new value.
     *
     * @param right the new right <code>Artifact</code>
     */
    public void setRight(T right) {
        artifacts.put(RIGHT, right);
    }

    /**
     * Returns whether this is a valid merge scenario.
     *
     * @return true iff the merge scenario is valid
     */
    public boolean isValid() {
        // TODO: this needs to be reimplemented considering the possibility of an n-way merge
        return true;
    }

    /**
     * Returns an <code>ArtifactList</code> containing the <code>Artifact</code>s in this <code>MergeScenario</code>
     * in the order of their insertion.
     *
     * @return the <code>ArtifactList</code>
     */
    public List<T> asList() {
        return artifacts.entrySet().stream().map(Map.Entry::getValue).collect(ArtifactList::new, ArrayList::add, ArrayList::addAll);
    }

    @Override
    public String toString() {
        return toString(" ", false);
    }

    /**
     * Returns a <code>String</code> representing the <code>MergeScenario</code> separated by a whitespace.
     *
     * @param humanReadable
     *         whether to omit empty dummy <code>Artifact</code>s
     * @return the <code>String</code> representing this <code>MergeScenario</code>
     */
    public String toString(boolean humanReadable) {
        return toString(" ", humanReadable);
    }

    /**
     * Returns a <code>String</code> representing the <code>MergeScenario</code>.
     *
     * @param sep
     *         the separator to use between the representations of the <code>Artifact</code>s
     * @param humanReadable
     *         whether to omit empty dummy <code>Artifact</code>s
     * @return the <code>String</code> representing this <code>MergeScenario</code>
     */
    public String toString(String sep, boolean humanReadable) {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<Revision, T> entry : artifacts.entrySet()) {
            T artifact = entry.getValue();

            if (!humanReadable || !artifact.isEmpty()) {
                sb.append(artifact.getId()).append(sep);
            }
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MergeScenario<?> that = (MergeScenario<?>) o;

        return Objects.equals(mergeType, that.mergeType) &&
                Objects.equals(artifacts, that.artifacts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mergeType, artifacts);
    }
}
