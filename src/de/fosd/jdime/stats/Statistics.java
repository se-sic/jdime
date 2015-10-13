package de.fosd.jdime.stats;

import java.util.ArrayList;
import java.util.List;

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

    public ElementStatistics getFileStatistics() {
        return fileStatistics;
    }

    public ElementStatistics getDirectoryStatistics() {
        return directoryStatistics;
    }
}
