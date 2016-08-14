package de.fosd.jdime.matcher.cost_model;

import java.util.Comparator;

import static java.util.Comparator.comparing;

final class Bounds {

    static final Comparator<Bounds> BY_MIDDLE = comparing(Bounds::middle);
    static final Comparator<Bounds> BY_LOWER_UPPER = comparing(Bounds::getLower).thenComparing(Bounds::getUpper);

    private float lower;
    private float upper;

    Bounds(float lower, float upper) {
        this.lower = lower;
        this.upper = upper;
    }

    public float getLower() {
        return lower;
    }

    public void setLower(float lower) {
        this.lower = lower;
    }

    public float getUpper() {
        return upper;
    }

    public void setUpper(float upper) {
        this.upper = upper;
    }

    private float middle() {
        return lower + (upper - lower) / 2;
    }

    @Override
    public String toString() {
        return String.format("[%s, %s]", lower, upper);
    }
}
