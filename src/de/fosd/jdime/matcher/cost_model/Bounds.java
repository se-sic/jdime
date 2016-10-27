package de.fosd.jdime.matcher.cost_model;

import java.util.Comparator;

import static java.util.Comparator.comparing;

/**
 * An interval bounded by two floats [lower, upper].
 */
final class Bounds {

    static final Comparator<Bounds> BY_MIDDLE = comparing(Bounds::middle);
    static final Comparator<Bounds> BY_LOWER_UPPER = comparing(Bounds::getLower).thenComparing(Bounds::getUpper);

    private float lower;
    private float upper;

    /**
     * Constructs new instance with the given bounds.
     *
     * @param lower
     *         the lower bound
     * @param upper
     *         the upper bound
     */
    Bounds(float lower, float upper) {
        this.lower = lower;
        this.upper = upper;
    }

    /**
     * Returns the lower bound.
     *
     * @return the lower bound
     */
    public float getLower() {
        return lower;
    }

    /**
     * Sets the lower bound.
     *
     * @param lower
     *         the new lower bound
     */
    public void setLower(float lower) {
        this.lower = lower;
    }

    /**
     * Returns the upper bound.
     *
     * @return the upper bound
     */
    public float getUpper() {
        return upper;
    }

    /**
     * Sets the upper bound.
     *
     * @param upper
     *         the new upper bound
     */
    public void setUpper(float upper) {
        this.upper = upper;
    }

    /**
     * Returns the middle of the interval.
     *
     * @return the middle of the interval
     */
    private float middle() {
        return lower + (upper - lower) / 2;
    }

    @Override
    public String toString() {
        return String.format("[%s, %s]", lower, upper);
    }
}
