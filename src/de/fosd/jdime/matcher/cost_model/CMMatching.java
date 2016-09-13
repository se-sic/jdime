package de.fosd.jdime.matcher.cost_model;

import java.util.Objects;

import de.fosd.jdime.common.Artifact;

final class CMMatching<T extends Artifact<T>> {

    final T m;
    final T n;

    private float exactCost;
    private Bounds costBounds;

    CMMatching(T m, T n) {
        this.m = m;
        this.n = n;
    }

    public boolean isNoMatch() {
        return m == null || n == null;
    }

    public boolean contains(T t) {
        return m == t || n == t;
    }

    public T other(T t) {
        if (m == t) {
            return n;
        } else if (n == t) {
            return m;
        } else {
            throw new IllegalArgumentException(t + " is not part of " + this);
        }
    }

    public float getExactCost() {
        return exactCost;
    }

    public void setExactCost(float exactCost) {
        this.exactCost = exactCost;
    }

    public Bounds getCostBounds() {
        return costBounds;
    }

    public void setBounds(float lower, float upper) {
        if (costBounds == null) {
            setCostBounds(new Bounds(lower, upper));
        } else {
            costBounds.setLower(lower);
            costBounds.setUpper(upper);
        }
    }

    public void setCostBounds(Bounds costBounds) {
        this.costBounds = costBounds;
    }

    @Override
    public String toString() {
        return String.format("{%s, %s, %f, %s}", m, n, exactCost, costBounds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CMMatching<?> that = (CMMatching<?>) o;
        return Objects.equals(m, that.m) && Objects.equals(n, that.n);
    }

    @Override
    public int hashCode() {
        return Objects.hash(m, n);
    }
}
