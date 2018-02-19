/**
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2017 University of Passau, Germany
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 *
 * Contributors:
 *     Olaf Lessenich <lessenic@fim.uni-passau.de>
 *     Georg Seibt <seibt@fim.uni-passau.de>
 */
package de.fosd.jdime.stats;

import de.fosd.jdime.config.merge.MergeScenario;
import de.fosd.jdime.config.merge.Revision;
import de.fosd.jdime.matcher.matching.Matching;
import de.fosd.jdime.util.parser.ParseResult;
import de.fosd.jdime.util.parser.Parser;

import java.io.PrintStream;
import java.util.*;

import static de.fosd.jdime.stats.MergeScenarioStatus.OK;

/**
 * A collection of statistics values about a <code>MergeScenario</code>.
 */
public class MergeScenarioStatistics {

    private MergeScenario<?> mergeScenario;
    private MergeScenarioStatus status;

    private Set<Matching<?>> matchings;
    private Map<Revision, Map<KeyEnums.Level, ElementStatistics>> levelStatistics;
    private Map<Revision, Map<KeyEnums.Type, ElementStatistics>> typeStatistics;
    private Map<Revision, MergeStatistics> mergeStatistics;

    private ElementStatistics charStatistics;
    private ElementStatistics tokenStatistics;
    private ElementStatistics lineStatistics;
    private ElementStatistics fileStatistics;
    private ElementStatistics directoryStatistics;
    private int conflicts;

    private Map<String, Runtime> runtimes;

    /**
     * Constructs a new <code>MergeScenarioStatistics</code> object for the given <code>MergeScenario</code>.
     *
     * @param mergeScenario
     *         the <code>MergeScenario</code> this <code>MergeScenarioStatistics</code> collects statistics for
     */
    public MergeScenarioStatistics(MergeScenario<?> mergeScenario) {
        this.mergeScenario = mergeScenario;
        this.status = OK;
        this.matchings = new HashSet<>();
        this.levelStatistics = new HashMap<>();
        this.typeStatistics = new HashMap<>();
        this.mergeStatistics = new HashMap<>();
        this.charStatistics = new ElementStatistics();
        this.tokenStatistics = new ElementStatistics();
        this.lineStatistics = new ElementStatistics();
        this.fileStatistics = new ElementStatistics();
        this.directoryStatistics = new ElementStatistics();
        this.conflicts = 0;
        this.runtimes = new HashMap<>();
    }

    /**
     * Copy constructor.
     *
     * @param toCopy
     *         the <code>MergeScenarioStatistics</code> to copy
     */
    public MergeScenarioStatistics(MergeScenarioStatistics toCopy) {
        this.mergeScenario = new MergeScenario<>(toCopy.mergeScenario);
        this.status = toCopy.status;

        this.matchings = new HashSet<>(toCopy.matchings.size());

        for (Matching<?> matching : toCopy.matchings) {
            this.matchings.add(new Matching<>(matching));
        }

        this.levelStatistics = new HashMap<>(toCopy.levelStatistics.size());

        for (Map.Entry<Revision, Map<KeyEnums.Level, ElementStatistics>> entry : toCopy.levelStatistics.entrySet()) {
            Map<KeyEnums.Level, ElementStatistics> map = new HashMap<>();

            for (Map.Entry<KeyEnums.Level, ElementStatistics> subEntry : entry.getValue().entrySet()) {
                map.put(subEntry.getKey(), new ElementStatistics(subEntry.getValue()));
            }

            this.levelStatistics.put(entry.getKey(), map);
        }

        this.typeStatistics = new HashMap<>(toCopy.typeStatistics.size());

        for (Map.Entry<Revision, Map<KeyEnums.Type, ElementStatistics>> entry : toCopy.typeStatistics.entrySet()) {
            Map<KeyEnums.Type, ElementStatistics> map = new HashMap<>();

            for (Map.Entry<KeyEnums.Type, ElementStatistics> subEntry : entry.getValue().entrySet()) {
                map.put(subEntry.getKey(), new ElementStatistics(subEntry.getValue()));
            }

            this.typeStatistics.put(entry.getKey(), map);
        }

        this.mergeStatistics = new HashMap<>(toCopy.mergeStatistics.size());

        for (Map.Entry<Revision, MergeStatistics> entry : toCopy.mergeStatistics.entrySet()) {
            this.mergeStatistics.put(entry.getKey(), new MergeStatistics(entry.getValue()));
        }

        this.charStatistics = new ElementStatistics(toCopy.charStatistics);
        this.tokenStatistics = new ElementStatistics(toCopy.tokenStatistics);
        this.lineStatistics = new ElementStatistics(toCopy.lineStatistics);
        this.fileStatistics = new ElementStatistics(toCopy.fileStatistics);
        this.directoryStatistics = new ElementStatistics(toCopy.directoryStatistics);
        this.conflicts = toCopy.conflicts;

        this.runtimes = new HashMap<>(toCopy.runtimes.size());

        for (Map.Entry<String, Runtime> entry : toCopy.runtimes.entrySet()) {
            this.runtimes.put(entry.getKey(), new Runtime(entry.getValue()));
        }
    }

    /**
     * Returns the <code>MergeScenario</code> this <code>MergeScenarioStatistics</code> collects statistics for.
     *
     * @return the <code>MergeScenario</code>
     */
    public MergeScenario<?> getMergeScenario() {
        return mergeScenario;
    }

    /**
     * Returns the current {@link MergeScenarioStatus}.
     *
     * @return the current {@link MergeScenarioStatus}
     */
    public MergeScenarioStatus getStatus() {
        return status;
    }

    /**
     * Sets current {@link MergeScenarioStatus} to the given {@code status}.
     *
     * @param status the new {@link MergeScenarioStatus}
     */
    public void setStatus(MergeScenarioStatus status) {
        this.status = status;
    }

    /**
     * Adds a <code>Matching</code> to this <code>MergeScenarioStatistics</code>.
     *
     * @param matching
     *         the <code>Matching</code> to add
     */
    public void addMatching(Matching<?> matching) {
        matchings.add(matching);
    }

    /**
     * Adds all <code>Matching</code>s contained in <code>matchings</code> to this <code>MergeScenarioStatistics</code>.
     *
     * @param matchings
     *         the <code>Matching</code>s to add
     */
    public void addAllMatchings(Collection<? extends Matching<?>> matchings) {
        this.matchings.addAll(matchings);
    }

    /**
     * Checks whether an <code>ElementStatistics</code> was registered for the <code>Revision</code> and
     * <code>KeyEnums.Level</code> combination.
     *
     * @param rev
     *         the <code>Revision</code> to check for
     * @param level
     *         the <code>KeyEnums.Level</code> to check for
     * @return true iff an <code>ElementStatistics</code> was registered
     */
    public boolean containsLevelStatistics(Revision rev, KeyEnums.Level level) {
        return levelStatistics.containsKey(rev) && levelStatistics.get(rev).containsKey(level);
    }

    /**
     * Returns the statistics container for the different <code>KeyEnums.Level</code> values.
     *
     * @return the <code>Map</code> from the <code>Revision</code>s of the <code>MergeScenario</code> to the statistics
     *         collected for different <code>KeyEnums.Level</code>s
     */
    public Map<Revision, Map<KeyEnums.Level, ElementStatistics>> getLevelStatistics() {
        return levelStatistics;
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
     * Checks whether an <code>ElementStatistics</code> was registered for the <code>Revision</code> and
     * <code>KeyEnums.Type</code> combination.
     *
     * @param rev
     *         the <code>Revision</code> to check for
     * @param type
     *         the <code>KeyEnums.Type</code> to check for
     * @return true iff an <code>ElementStatistics</code> was registered
     */
    public boolean containsTypeStatistics(Revision rev, KeyEnums.Type type) {
        return typeStatistics.containsKey(rev) && typeStatistics.get(rev).containsKey(type);
    }

    /**
     * Returns the statistics container for the different <code>KeyEnums.Type</code> values.
     *
     * @return the <code>Map</code> from the <code>Revision</code>s of the <code>MergeScenario</code> to the statistics
     *         collected for different <code>KeyEnums.Type</code>s
     */
    public Map<Revision, Map<KeyEnums.Type, ElementStatistics>> getTypeStatistics() {
        return typeStatistics;
    }

    /**
     * Returns the <code>ElementStatistics</code> for the given <code>Revision</code> and <code>TYPE</code>.
     * Creates and registers a new <code>ElementStatistics</code> object if necessary. If <code>type</code> is
     * one of <code>LINE, FILE or DIRECTORY</code> the <code>Revision</code> is ignored the appropriate
     * <code>getXStatistics</code> method is used.
     *
     * @param rev
     *         the <code>Revision</code> to look up
     * @param type
     *         the <code>TYPE</code> in the <code>Revision</code>
     * @return the corresponding <code>ElementStatistics</code>
     */
    public ElementStatistics getTypeStatistics(Revision rev, KeyEnums.Type type) {

        switch (type) {

            case LINE:
                return lineStatistics;
            case FILE:
                return fileStatistics;
            case DIRECTORY:
                return directoryStatistics;
        }

        return typeStatistics.computeIfAbsent(rev, r -> new HashMap<>()).computeIfAbsent(type, l -> new ElementStatistics());
    }

    /**
     * Returns the statistics container for the <code>MergeStatistics</code> of the <code>Revision</code>s of the
     * <code>MergeScenario</code>.
     *
     * @return the <code>Map</code> from the <code>Revision</code>s of the <code>MergeScenario</code> to the
     *         <code>MergeStatistics</code> collected for them
     */
    public Map<Revision, MergeStatistics> getMergeStatistics() {
        return mergeStatistics;
    }

    /**
     * Returns the <code>MergeStatistics</code> collected for the given <code>Revision</code>. A new
     * <code>MergeStatistics</code> object will be created of necessary.
     *
     * @param rev
     *         the <code>Revision</code> to get the <code>MergeStatistics</code> for
     * @return the corresponding <code>MergeStatistics</code>
     */
    public MergeStatistics getMergeStatistics(Revision rev) {
        return mergeStatistics.computeIfAbsent(rev, r -> new MergeStatistics());
    }

    /**
     * Returns statistics for {@link KeyEnums.Type#LINE}.
     *
     * @return the line statistics
     */
    public ElementStatistics getLineStatistics() {
        return lineStatistics;
    }

    /**
     * Parses the given <code>mergeResult</code> using {@link Parser#parse(String)} and adds the resulting statistics
     * to this <code>MergeScenarioStatistics</code>.
     *
     * @param mergeResult
     *         the code to parse
     * @return the <code>ParseResult</code> from {@link Parser#parse(String)}
     */
    public ParseResult addLineStatistics(String mergeResult) {
        ParseResult result = Parser.parse(mergeResult);

        charStatistics.incrementTotal(result.getChars());
        charStatistics.incrementNumOccurInConflict(result.getConflictingChars());
        tokenStatistics.incrementTotal(result.getTokens());
        tokenStatistics.incrementNumOccurInConflict(result.getConflictingTokens());
        lineStatistics.incrementTotal(result.getLinesOfCode());
        lineStatistics.incrementNumOccurInConflict(result.getConflictingLinesOfCode());
        conflicts += result.getConflicts();

        return result;
    }

    /**
     * Parses the given <code>mergeResult</code> using {@link Parser#parse(String)} and sets the resulting statistics
     * to this <code>MergeScenarioStatistics</code>.
     *
     * @param mergeResult
     *         the code to parse
     * @return the <code>ParseResult</code> from {@link Parser#parse(String)}
     */
    public ParseResult setLineStatistics(String mergeResult) {
        ParseResult result = Parser.parse(mergeResult);

        charStatistics.setTotal(result.getChars());
        charStatistics.setNumOccurInConflict(result.getConflictingChars());
        tokenStatistics.setTotal(result.getTokens());
        tokenStatistics.setNumOccurInConflict(result.getConflictingTokens());
        lineStatistics.setTotal(result.getLinesOfCode());
        lineStatistics.setNumOccurInConflict(result.getConflictingLinesOfCode());
        conflicts = result.getConflicts();

        return result;
    }

    /**
     * Returns the statistics for {@link KeyEnums.Type#FILE}.
     *
     * @return the file statistics
     */
    public ElementStatistics getFileStatistics() {
        return fileStatistics;
    }

    /**
     * Returns the statistics for {@link KeyEnums.Type#DIRECTORY}.
     *
     * @return the directory statistics
     */
    public ElementStatistics getDirectoryStatistics() {
        return directoryStatistics;
    }

    /**
     * Returns the number conflicts.
     *
     * @return the number of conflicts
     */
    public int getConflicts() {
        return conflicts;
    }

    /**
     * Returns the {@link Runtime} for the given {@code label}. A new {@link Runtime} will be created an stored in this
     * {@link MergeScenarioStatistics} is necessary.
     *
     * @param label
     *         the label of the {@link Runtime}
     * @return the {@link Runtime}
     */
    public Runtime getRuntime(String label) {
        return runtimes.computeIfAbsent(label, Runtime::new);
    }

    /**
     * Returns whether a {@link Runtime} with the given label was stored in this {@link MergeScenarioStatistics}.
     *
     * @param label
     *         the label to look for
     * @return true iff a {@link Runtime} with the given label was stored in this {@link MergeScenarioStatistics}
     */
    public boolean hasRuntime(String label) {
        return runtimes.containsKey(label);
    }

    /**
     * Stores the given {@link Runtime} in this {@link MergeScenarioStatistics}. If a {@link Runtime} with the given
     * label is already present, it will be overwritten.
     *
     * @param runtime
     *         the {@link Runtime} to store in this {@link MergeScenarioStatistics}
     */
    public void putRuntime(Runtime runtime) {
        runtimes.put(runtime.getLabel(), runtime);
    }

    /**
     * Adds all <code>ElementStatistics</code> in <code>other</code> to the corresponding
     * <code>ElementStatistics</code> added to <code>this</code>. If an <code>ElementStatistics</code> in
     * <code>other</code> has no partner in <code>this</code> it will simply be added to <code>this</code>.
     *
     * @param other
     *         the <code>MergeScenarioStatistics</code> to add to <code>this</code>
     * @see ElementStatistics#add(ElementStatistics)
     */
    public void add(MergeScenarioStatistics other) {

        addAllMatchings(other.matchings);

        for (Map.Entry<Revision, Map<KeyEnums.Level, ElementStatistics>> entry : other.levelStatistics.entrySet()) {
            Revision rev = entry.getKey();

            for (Map.Entry<KeyEnums.Level, ElementStatistics> subEntry : entry.getValue().entrySet()) {
                KeyEnums.Level level = subEntry.getKey();
                getLevelStatistics(rev, level).add(subEntry.getValue());
            }
        }

        for (Map.Entry<Revision, Map<KeyEnums.Type, ElementStatistics>> entry : other.typeStatistics.entrySet()) {
            Revision rev = entry.getKey();

            for (Map.Entry<KeyEnums.Type, ElementStatistics> subEntry : entry.getValue().entrySet()) {
                KeyEnums.Type type = subEntry.getKey();
                getTypeStatistics(rev, type).add(subEntry.getValue());
            }
        }

        for (Map.Entry<Revision, MergeStatistics> entry : other.mergeStatistics.entrySet()) {
            getMergeStatistics(entry.getKey()).add(entry.getValue());
        }

        charStatistics.add(other.charStatistics);
        tokenStatistics.add(other.tokenStatistics);
        lineStatistics.add(other.lineStatistics);
        fileStatistics.add(other.fileStatistics);
        directoryStatistics.add(other.directoryStatistics);
        conflicts += other.conflicts;

        for (Map.Entry<String, Runtime> entry : other.runtimes.entrySet()) {
            getRuntime(entry.getKey()).add(entry.getValue());
        }
    }

    /**
     * Writes a human readable representation of this <code>MergeScenarioStatistics</code> object to the given
     * <code>PrintStream</code>.
     *
     * @param os
     *         the <code>PrintStream</code> to write to
     */
    public void print(PrintStream os) {
        String indent = "    ";

        os.printf("%s for %s:%n", MergeScenarioStatistics.class.getSimpleName(), MergeScenario.class.getSimpleName());
        mergeScenario.asList().forEach(artifact -> os.printf("%s%s%n", indent, artifact.getId()));
        os.println("General:");
        os.printf("%sConflicts: %s%n", indent, conflicts);

        runtimes.forEach((label, runtime) ->
                os.printf("%sRuntime (%s): %dms%n", indent, label, runtime.getTimeMS())
        );

        if (!matchings.isEmpty()) os.println("Matchings");
        matchings.stream().sorted().forEachOrdered(matching ->
            os.printf("%s%s%n", indent, matching)
        );

        if (!levelStatistics.isEmpty()) os.println("Level Statistics");
        levelStatistics.forEach((rev, map) -> map.forEach((level, stats) -> {
            os.printf("%s %s %s %s%n", Revision.class.getSimpleName(), rev, KeyEnums.Level.class.getSimpleName(), level);
            stats.print(os, indent);
        }));

        if (!typeStatistics.isEmpty()) os.println("Type Statistics");
        typeStatistics.forEach((rev, map) -> map.forEach((type, stats) -> {
            os.printf("%s %s %s %s%n", Revision.class.getSimpleName(), rev, KeyEnums.Type.class.getSimpleName(), type);
            stats.print(os, indent);
        }));

        if (!mergeStatistics.isEmpty()) os.println("Merge Statistics");
        mergeStatistics.forEach((rev, stats) -> {
            os.printf("%s %s%n", Revision.class.getSimpleName(), rev);
            stats.print(os, indent);
        });

        os.println("Char statistics");
        charStatistics.print(os, indent);

        os.println("Token statistics");
        tokenStatistics.print(os, indent);

        os.println("Line Statistics");
        lineStatistics.print(os, indent);

        os.println("File Statistics");
        fileStatistics.print(os, indent);

        os.println("Directory Statistics");
        directoryStatistics.print(os, indent);
    }
}
