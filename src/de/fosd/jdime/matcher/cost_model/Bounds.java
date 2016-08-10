package de.fosd.jdime.matcher.cost_model;

final class Bounds implements Comparable<Bounds> {

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
        return Float.compare(middle(), o.middle());
    }

    private float middle() {
        return lower + (upper - lower) / 2;
    }

    @Override
    public String toString() {
        return String.format("[%s, %s]", lower, upper);
    }
}
