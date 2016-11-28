package de.fosd.jdime;

import java.util.List;

import de.fosd.jdime.config.merge.MergeType;

public class MergeTestCase {

    private static final String UNNAMED = "UNNAMED";

    private final String name;

    public final List<String> strategies;

    public final MergeType type;
    public final String path;

    public MergeTestCase(String name, List<String> strategies, MergeType type, String path) {
        this.name = name;
        this.strategies = strategies;
        this.type = type;
        this.path = path;
    }

    public boolean valid() {

        if (strategies == null || strategies.isEmpty()) {
            return false;
        }

        if (type == null) {
            return false;
        }

        if (path == null) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return name != null ? name : UNNAMED;
    }
}
