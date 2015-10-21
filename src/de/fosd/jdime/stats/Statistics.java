package de.fosd.jdime.stats;

import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.fosd.jdime.common.MergeScenario;

public class Statistics {

    private Map<MergeScenario<?>, MergeScenarioStatistics> scenarioStatistics;

    public Statistics() {
        this.scenarioStatistics = new HashMap<>();
    }

    public MergeScenarioStatistics getScenarioStatistics(MergeScenario<?> mergeScenario) {
        return scenarioStatistics.computeIfAbsent(mergeScenario, MergeScenarioStatistics::new);
    }

    public List<MergeScenarioStatistics> getScenarioStatistics() {
        return scenarioStatistics.values().stream().collect(Collectors.toList());
    }

    public void addScenarioStatistics(MergeScenarioStatistics statistics) {
        scenarioStatistics.put(statistics.getMergeScenario(), statistics);
    }

    public IntSummaryStatistics getConflictStatistics() {
        return scenarioStatistics.values().stream().collect(Collectors.summarizingInt(MergeScenarioStatistics::getConflicts));
    }

    public boolean hasConflicts() {
        return scenarioStatistics.values().stream().anyMatch(s -> s.getConflicts() > 0);
    }

    public void add(Statistics other) {
        for (Map.Entry<MergeScenario<?>, MergeScenarioStatistics> entry : other.scenarioStatistics.entrySet()) {
            getScenarioStatistics(entry.getKey()).add(entry.getValue());
        }
    }
}
