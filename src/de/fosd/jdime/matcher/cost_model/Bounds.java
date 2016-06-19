package de.fosd.jdime.matcher.cost_model;

import java.util.Comparator;

import static java.util.Comparator.comparingDouble;

final class Bounds implements Comparable<Bounds> {

    public static final Comparator<Bounds> COMPARATOR = comparingDouble(Bounds::getLower).thenComparingDouble(Bounds::getUpper);

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

    @Override
    public int compareTo(Bounds o) {
        return COMPARATOR.compare(this, o);
    }

    @Override
    public String toString() {
        return String.format("[%s, %s]", lower, upper);
    }
}
