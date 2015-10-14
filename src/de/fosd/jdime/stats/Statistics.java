package de.fosd.jdime.stats;

import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.stream.Collectors;

public class Statistics {

    private List<MergeScenarioStatistics> scenarioStatistics;
    private ElementStatistics fileStatistics;
    private ElementStatistics directoryStatistics;

    public Statistics() {
        this.scenarioStatistics = new ArrayList<>();
        this.fileStatistics = new ElementStatistics();
        this.directoryStatistics = new ElementStatistics();
    }

    public List<MergeScenarioStatistics> getScenarioStatistics() {
        return scenarioStatistics;
    }

    public void addScenarioStatistics(MergeScenarioStatistics statistics) {
        scenarioStatistics.add(statistics);
    }

    public ElementStatistics getFileStatistics() {
        return fileStatistics;
    }

    public ElementStatistics getDirectoryStatistics() {
        return directoryStatistics;
    }

    public IntSummaryStatistics getConflictStatistics() {
        return scenarioStatistics.stream().collect(Collectors.summarizingInt(MergeScenarioStatistics::getConflicts));
    }

    public boolean hasConflicts() {
        return scenarioStatistics.stream().anyMatch(s -> s.getConflicts() > 0);
    }
}
