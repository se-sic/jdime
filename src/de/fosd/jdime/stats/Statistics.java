package de.fosd.jdime.stats;

import de.fosd.jdime.common.Revision;
import de.fosd.jdime.stats.parser.ParseResult;
import de.fosd.jdime.stats.parser.Parser;

import java.util.HashMap;
import java.util.Map;

public class Statistics {

    private Map<Revision, Map<KeyEnums.Level, ElementStatistics>> levelStats;
    private Map<Revision, Map<KeyEnums.Type, ElementStatistics>> typeStats;
    private Map<Revision, MergeStatistics> mergeStats;

    private ElementStatistics lineStats;
    private ElementStatistics fileStats;
    private ElementStatistics directoryStats;

    private int conflicts;

    public Statistics() {
        this.levelStats = new HashMap<>();
        this.typeStats = new HashMap<>();
        this.mergeStats = new HashMap<>();
        this.lineStats = new ElementStatistics();
        this.fileStats = new ElementStatistics();
        this.directoryStats = new ElementStatistics();
        this.conflicts = 0;
    }

    /**
     * Returns the <code>ElementStatistics</code> for the given <code>Revision</code> and <code>LEVEL</code>.
     * Creates and registers a new <code>ElementStatistics</code> object if necessary.
     *
     * @param rev the <code>Revision</code> to look up
     * @param level the <code>LEVEL</code> in the <code>Revision</code>
     * @return the corresponding <code>ElementStatistics</code>
     */
    public ElementStatistics getLevelStatistics(Revision rev, KeyEnums.Level level) {
        return levelStats.computeIfAbsent(rev, r -> new HashMap<>()).computeIfAbsent(level, l -> new ElementStatistics());
    }

    /**
     * Returns the <code>ElementStatistics</code> for the given <code>Revision</code> and <code>TYPE</code>.
     * Creates and registers a new <code>ElementStatistics</code> object if necessary. If <code>type</code> is one of
     * <code>FILE</code>, <code>DIRECTORY</code> or <code>LINE</code> the <code>Revision</code> is ignored.
     *
     * @param rev the <code>Revision</code> to look up
     * @param type the <code>TYPE</code> in the <code>Revision</code>
     * @return the corresponding <code>ElementStatistics</code>
     */
    public ElementStatistics getTypeStatistics(Revision rev, KeyEnums.Type type) {

        switch (type) {

            case FILE:
                return fileStats;
            case DIRECTORY:
                return directoryStats;
            case LINE:
                return lineStats;
            default:
                return typeStats.computeIfAbsent(rev, r -> new HashMap<>()).computeIfAbsent(type, l -> new ElementStatistics());
        }
    }

    public MergeStatistics getMergeStatistics(Revision rev) {
        return mergeStats.computeIfAbsent(rev, r -> new MergeStatistics());
    }

    public int getConflicts() {
        return conflicts;
    }

    public void add(Statistics other) {

        for (Map.Entry<Revision, Map<KeyEnums.Level, ElementStatistics>> entry : other.levelStats.entrySet()) {
            Revision rev = entry.getKey();

            for (Map.Entry<KeyEnums.Level, ElementStatistics> subEntry : entry.getValue().entrySet()) {
                KeyEnums.Level level = subEntry.getKey();
                getLevelStatistics(rev, level).add(subEntry.getValue());
            }
        }

        for (Map.Entry<Revision, Map<KeyEnums.Type, ElementStatistics>> entry : other.typeStats.entrySet()) {
            Revision rev = entry.getKey();

            for (Map.Entry<KeyEnums.Type, ElementStatistics> subEntry : entry.getValue().entrySet()) {
                KeyEnums.Type type = subEntry.getKey();
                getTypeStatistics(rev, type).add(subEntry.getValue());
            }
        }

        for (Map.Entry<Revision, MergeStatistics> entry : other.mergeStats.entrySet()) {
            getMergeStatistics(entry.getKey()).add(entry.getValue());
        }
    }

    public ParseResult addLineStatistics(String mergeResult) {
        ParseResult result = Parser.parse(mergeResult);

        lineStats.incrementTotal(result.getLinesOfCode());
        lineStats.incrementNumOccurInConflic(result.getConflictingLinesOfCode());
        conflicts += result.getConflicts();

        return result;
    }

    public ParseResult setLineStatistics(String mergeResult) {
        ParseResult result = Parser.parse(mergeResult);

        lineStats.setTotal(result.getLinesOfCode());
        lineStats.setNumOccurInConflict(result.getConflictingLinesOfCode());
        conflicts = result.getConflicts();

        return result;
    }
}
