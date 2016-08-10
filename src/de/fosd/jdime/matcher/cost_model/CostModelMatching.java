package de.fosd.jdime.matcher.cost_model;

import de.fosd.jdime.common.Artifact;

final class CostModelMatching<T extends Artifact<T>> {

    final T m;
    final T n;

    private float exactCost;
    private Bounds costBounds;

    CostModelMatching(T m, T n) {
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
}
