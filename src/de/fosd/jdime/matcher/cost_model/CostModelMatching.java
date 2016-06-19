package de.fosd.jdime.matcher.cost_model;

import de.fosd.jdime.common.Artifact;

final class CostModelMatching<T extends Artifact<T>> {

    final T m;
    final T n;

    private Bounds bounds;

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

    public Bounds getBounds() {
        return bounds;
    }

    public void setBounds(float lower, float upper) {
        if (bounds == null) {
            setBounds(new Bounds(lower, upper));
        } else {
            bounds.setLower(lower);
            bounds.setUpper(upper);
        }
    }

    public void setBounds(Bounds bounds) {
        this.bounds = bounds;
    }

    @Override
    public String toString() {
        return String.format("{%s, %s, %s}", m, n, bounds);
    }
}
