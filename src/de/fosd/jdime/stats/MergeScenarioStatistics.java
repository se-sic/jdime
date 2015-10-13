package de.fosd.jdime.stats;

import java.util.HashMap;
import java.util.Map;

import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeScenario;
import de.fosd.jdime.common.Revision;
import de.fosd.jdime.stats.parser.ParseResult;
import de.fosd.jdime.stats.parser.Parser;

public class MergeScenarioStatistics {

    private MergeScenario<FileArtifact> mergeScenario;

    private Map<Revision, Map<KeyEnums.Level, ElementStatistics>> levelStatistics;
    private Map<Revision, Map<KeyEnums.Type, ElementStatistics>> typeStatistics;
    private Map<Revision, MergeStatistics> mergeStatistics;

    private ElementStatistics lineStatistics;
    private int conflicts;

    public MergeScenarioStatistics(MergeScenario<FileArtifact> mergeScenario) {
        this.mergeScenario = mergeScenario;
        this.levelStatistics = new HashMap<>();
        this.typeStatistics = new HashMap<>();
        this.mergeStatistics = new HashMap<>();
        this.lineStatistics = new ElementStatistics();
        this.conflicts = 0;
    }

    /**
     * Returns the <code>ElementStatistics</code> for the given <code>Revision</code> and <code>LEVEL</code>.
     * Creates and registers a new <code>ElementStatistics</code> object if necessary.
     *
     * @param rev
     *         the <code>Revision</code> to look up
     * @param level
     *         the <code>LEVEL</code> in the <code>Revision</code>
     * @return the corresponding <code>ElementStatistics</code>
     */
    public ElementStatistics getLevelStatistics(Revision rev, KeyEnums.Level level) {
        return levelStatistics.computeIfAbsent(rev, r -> new HashMap<>()).computeIfAbsent(level, l -> new ElementStatistics());
    }

    /**
     * Returns the <code>ElementStatistics</code> for the given <code>Revision</code> and <code>TYPE</code>.
     * Creates and registers a new <code>ElementStatistics</code> object if necessary.
     *
     * @param rev
     *         the <code>Revision</code> to look up
     * @param type
     *         the <code>TYPE</code> in the <code>Revision</code>
     * @return the corresponding <code>ElementStatistics</code>
     */
    public ElementStatistics getTypeStatistics(Revision rev, KeyEnums.Type type) {
        return typeStatistics.computeIfAbsent(rev, r -> new HashMap<>()).computeIfAbsent(type, l -> new ElementStatistics());
    }

    public MergeStatistics getMergeStatistics(Revision rev) {
        return mergeStatistics.computeIfAbsent(rev, r -> new MergeStatistics());
    }

    public ElementStatistics getLineStatistics() {
        return lineStatistics;
    }

    public ParseResult addLineStatistics(String mergeResult) {
        ParseResult result = Parser.parse(mergeResult);

        lineStatistics.incrementTotal(result.getLinesOfCode());
        lineStatistics.incrementNumOccurInConflic(result.getConflictingLinesOfCode());
        conflicts += result.getConflicts();

        return result;
    }

    public ParseResult setLineStatistics(String mergeResult) {
        ParseResult result = Parser.parse(mergeResult);

        lineStatistics.setTotal(result.getLinesOfCode());
        lineStatistics.setNumOccurInConflict(result.getConflictingLinesOfCode());
        conflicts = result.getConflicts();

        return result;
    }

    public int getConflicts() {
        return conflicts;
    }
}
