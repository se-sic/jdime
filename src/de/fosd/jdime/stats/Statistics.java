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

    public MergeStatistics getMergeStatistics(Revision rev) {
        return mergeStats.computeIfAbsent(rev, r -> new MergeStatistics());
    }

    public void add(Statistics other) {

        for (Map.Entry<Revision, Map<KeyEnums.LEVEL, ElementStatistics>> entry : other.levelStats.entrySet()) {
            Revision rev = entry.getKey();

            for (Map.Entry<KeyEnums.LEVEL, ElementStatistics> subEntry : entry.getValue().entrySet()) {
                KeyEnums.LEVEL level = subEntry.getKey();
                getLevelStatistics(rev, level).add(subEntry.getValue());
            }
        }

        for (Map.Entry<Revision, Map<KeyEnums.TYPE, ElementStatistics>> entry : other.typeStats.entrySet()) {
            Revision rev = entry.getKey();

            for (Map.Entry<KeyEnums.TYPE, ElementStatistics> subEntry : entry.getValue().entrySet()) {
                KeyEnums.TYPE type = subEntry.getKey();
                getTypeStatistics(rev, type).add(subEntry.getValue());
            }
        }

        for (Map.Entry<Revision, MergeStatistics> entry : other.mergeStats.entrySet()) {
            getMergeStatistics(entry.getKey()).add(entry.getValue());
        }
    }
}
