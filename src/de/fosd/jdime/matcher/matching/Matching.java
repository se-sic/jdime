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
package de.fosd.jdime.matcher.matching;

import java.util.Objects;
import java.util.logging.Logger;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.util.UnorderedTuple;

/**
 * A container class representing a matching between two <code>T</code>s.
 *
 * @param <T> the type of the <code>Artifact</code>
 */
public class Matching<T extends Artifact<T>> implements Cloneable, Comparable<Matching<T>> {

    private static final Logger LOG = Logger.getLogger(Matching.class.getCanonicalName());

    /**
     * The algorithm that found the matching.
     */
    private String algorithm;

    /**
     * The color to highlight the matching in.
     */
    private Color highlightColor;

    private UnorderedTuple<T, T> matchedArtifacts;
    private float percentage;
    private int score;
    private boolean fullyMatched;

    /**
     * Constructs a new <code>Matching</code> between the two given <code>T</code>s.
     *
     * @param left the left <code>Artifact</code>
     * @param right the right <code>Artifact</code>
     * @param score the score of the matching
     */
    public Matching(T left, T right, int score) {
        this(UnorderedTuple.of(left, right), score);
    }

    /**
     * Constructs a new <code>Matching</code> between the two given <code>T</code>s.
     *
     * @param matchedArtifacts the two matched <code>Artifact</code>s
     * @param score the score of the matching
     */
    public Matching(UnorderedTuple<T, T> matchedArtifacts, int score) {
        Objects.requireNonNull(matchedArtifacts);
        Objects.requireNonNull(matchedArtifacts.getX());
        Objects.requireNonNull(matchedArtifacts.getY());

        this.matchedArtifacts = matchedArtifacts;
        this.score = score;
        calculatePercentage();
    }

    /**
     * Performs a shallow copy (the matched <code>Artifact</code>s will not be copied).
     *
     * @param toCopy
     *         the <code>Matching</code> to copy
     */
    public Matching(Matching<T> toCopy) {
        this.algorithm = toCopy.algorithm;
        this.highlightColor = toCopy.highlightColor;
        this.matchedArtifacts = UnorderedTuple.of(toCopy.matchedArtifacts.getX(), toCopy.matchedArtifacts.getY());
        this.percentage = toCopy.percentage;
        this.score = toCopy.score;
    }

    /**
     * Returns the left <code>Artifact</code> of the matching.
     *
     * @return the left <code>Artifact</code>
     */
    public T getLeft() {
        return matchedArtifacts.getX();
    }

    /**
     * Returns the right <code>Artifact</code> of the matching.
     *
     * @return the right <code>Artifact</code>
     */
    public T getRight() {
        return matchedArtifacts.getY();
    }

    /**
     * Returns the matched <code>Artifact</code>s.
     *
     * @return the matched <code>Artifact</code>s
     */
    public UnorderedTuple<T, T> getMatchedArtifacts() {
        return matchedArtifacts;
    }

    /**
     * If one of the <code>Artifact</code>s contained in this <code>Matching</code> is referentially equal to
     * <code>artifact</code> this method returns the other <code>Artifact</code> in this <code>Matching</code>.
     * Otherwise <code>null</code> is returned.
     *
     * @param artifact
     *         the <code>Artifact</code> whose match is to be returned
     * @return the match of the <code>artifact</code> or <code>null</code>
     */
    public T getMatchingArtifact(Artifact<T> artifact) {
        T left = getLeft();
        T right = getRight();

        return left == artifact ? right : right == artifact ? left : null;
    }

    /**
     * Replaces {@code toReplace} with {@code artifact} in the {@link Artifact Artifacts} matched by this
     * {@link Matching}. If {@code toReplace} is not part of this {@link Matching} no action is taken.
     *
     * @param artifact
     *         the {@link Artifact} to replace {@code toReplace} with
     * @param toReplace
     *         the {@link Artifact} to replace with {@code artifact}
     */
    public void updateMatching(T artifact, T toReplace) {

        if (matchedArtifacts.getX() == toReplace) {
            matchedArtifacts.setX(artifact);
        } else if (matchedArtifacts.getY() == toReplace) {
            matchedArtifacts.setY(artifact);
        } else {
            LOG.warning("Ignoring a call to Matching#updateMatching(T, T) as the Artifact to replace was not part of " +
                        "the Matching.");
            return;
        }

        calculatePercentage();
    }

    /**
     * Returns the score of the matching.
     *
     * @return the score
     */
    public int getScore() {
        return score;
    }

    /**
     * Sets score to the given value.
     *
     * @param score the new score
     */
    public void setScore(int score) {
        this.score = score;
        calculatePercentage();
    }

    /**
     * Returns a float from [0, 1] describing the percentual match between the matched <code>Artifact</code>s.
     * The percentage is calculated as (2 * score) / (left.getTreeSize() + right.getTreeSize()).
     *
     * @return the matching percentage
     */
    public float getPercentage() {
        return percentage;
    }

    /**
     * Sets the <code>percentage</code> field to (2 * score) / (left.getTreeSize() + right.getTreeSize()) or 0 if
     * <code>left</code> or <code>right</code> is <code>null</code>.
     */
    private void calculatePercentage() {
        T left = getLeft();
        T right = getRight();

        if (left == null || right == null) {
            percentage = 0;
            fullyMatched = false;
        } else {
            int lSize = left.getTreeSize();
            int rSize = right.getTreeSize();

            percentage = (2 * (float) score) / (lSize + rSize);
            fullyMatched = (2 * score) == (lSize + rSize);
        }
    }

    /**
     * Returns true iff the nodes have fully matched, i.e., 100 percent of the trees has been matched.
     *
     * @return true iff trees have fully been matched
     */
    public boolean hasFullyMatched() {
        return fullyMatched;
    }

    /**
     * Returns a <code>String</code> describing the algorithm that found the matching.
     *
     * @return the algorithm description
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Sets the <code>String</code> describing the algorithm that found the matching to the given value.
     *
     * @param algorithm the new algorithm description
     */
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * Returns the color to highlight the matching in.
     *
     * @return the highlight color
     */
    public Color getHighlightColor() {
        return highlightColor;
    }

    /**
     * Sets the color to highlight the matching in to the given value.
     *
     * @param highlightColor the new highlight color
     */
    public void setHighlightColor(Color highlightColor) {
        this.highlightColor = highlightColor;
    }

    @Override
    public String toString() {
        int percentage = (int) (getPercentage() * 100);
        return String.format("(%s, %s) = %d (%d%%)", getLeft().getId(), getRight().getId(), score, percentage);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Matching<?> that = (Matching<?>) o;

        return matchedArtifacts.equals(that.matchedArtifacts);
    }

    @Override
    public int hashCode() {
        return matchedArtifacts.hashCode();
    }

    @Override
    @SuppressWarnings("unchecked") // the warning is inevitable but harmless
    public Matching<T> clone() {

        try {
            Matching<T> clone = (Matching<T>) super.clone();
            clone.matchedArtifacts = matchedArtifacts.clone();

            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int compareTo(Matching<T> o) {
        int dif = matchedArtifacts.getX().compareTo(o.getMatchedArtifacts().getX());

        if (dif != 0) {
            return dif;
        }

        dif = matchedArtifacts.getY().compareTo(o.getMatchedArtifacts().getY());

        if (dif != 0) {
            return dif;
        }

        return score - o.score;
    }
}
