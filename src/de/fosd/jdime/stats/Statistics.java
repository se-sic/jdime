package de.fosd.jdime.stats;

import de.fosd.jdime.common.Revision;

import java.util.HashMap;
import java.util.Map;

public class Statistics {

    private Map<Revision, Map<KeyEnums.LEVEL, ElementStatistics>> levelStats;
    private Map<Revision, Map<KeyEnums.TYPE, ElementStatistics>> typeStats;
    private Map<Revision, MergeStatistics> mergeStats;

    public Statistics() {
        this.levelStats = new HashMap<>();
        this.typeStats = new HashMap<>();
        this.mergeStats = new HashMap<>();
    }

    public ElementStatistics getLevelStatistics(Revision rev, KeyEnums.LEVEL level) {
        return levelStats.computeIfAbsent(rev, r -> new HashMap<>()).computeIfAbsent(level, l -> new ElementStatistics());
    }

    public ElementStatistics getTypeStatistics(Revision rev, KeyEnums.TYPE type) {
        return typeStats.computeIfAbsent(rev, r -> new HashMap<>()).computeIfAbsent(type, l -> new ElementStatistics());
    }
}
