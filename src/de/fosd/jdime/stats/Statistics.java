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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.ImplicitCollectionMapper;
import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.artifact.file.FileArtifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.config.merge.MergeScenario;
import de.fosd.jdime.config.merge.Revision;
import de.fosd.jdime.matcher.matching.LookAheadMatching;
import de.fosd.jdime.matcher.matching.Matching;

/**
 * A collection of <code>MergeScenarioStatistics</code> containing collected statistics about
 * <code>MergeScenario</code>s that were merged during a run of JDime.
 */
public class Statistics {

    private static final Logger LOG = Logger.getLogger(Statistics.class.getCanonicalName());

    private MergeScenario<FileArtifact> currentFileMergeScenario;
    private Map<MergeScenario<?>, MergeScenarioStatistics> scenarioStatistics;

    /**
     * Constructs a new <code>Statistics</code> object.
     */
    public Statistics() {
        this.scenarioStatistics = new HashMap<>();
    }

    /**
     * Copy constructor.
     *
     * @param toCopy
     *         the <code>Statistics</code> to copy
     */
    public Statistics(Statistics toCopy) {

        if (toCopy.currentFileMergeScenario != null) {
            this.currentFileMergeScenario = new MergeScenario<>(toCopy.currentFileMergeScenario);
        }

        this.scenarioStatistics = new HashMap<>();

        for (Map.Entry<MergeScenario<?>, MergeScenarioStatistics> entry : toCopy.scenarioStatistics.entrySet()) {
            MergeScenario<?> mScenario = new MergeScenario<>(entry.getKey());
            MergeScenarioStatistics mStats = new MergeScenarioStatistics(entry.getValue());

            this.scenarioStatistics.put(mScenario, mStats);
        }
    }

    /**
     * Gets the <code>MergeScenarioStatistics</code> for the current <code>FileArtifact</code>
     * <code>MergeScenario</code>.
     *
     * @return the <code>MergeScenarioStatistics</code>
     */
    public MergeScenarioStatistics getCurrentFileMergeScenarioStatistics() {
        return getScenarioStatistics(currentFileMergeScenario);
    }

    /**
     * Sets the currently active <code>MergeScenario</code> for <code>FileArtifacts</code> to the new value.
     *
     * @param currentFileMergeScenario the new <code>MergeScenario</code> for <code>FileArtifacts</code>
     */
    public void setCurrentFileMergeScenario(MergeScenario<FileArtifact> currentFileMergeScenario) {
        this.currentFileMergeScenario = currentFileMergeScenario;
    }

    /**
     * Checks whether a <code>MergeScenarioStatistics</code> for the given <code>MergeScenario</code> was added to
     * this <code>Statistics</code>.
     *
     * @param mergeScenario
     *         the <code>MergeScenario</code> to check for
     * @return true iff a <code>MergeScenarioStatistics</code> was registered for <code>mergeScenario</code>
     */
    public boolean containsStatistics(MergeScenario<?> mergeScenario) {
        return scenarioStatistics.containsKey(mergeScenario);
    }

    /**
     * Returns the <code>MergeScenarioStatistics</code> for the given <code>MergeScenario</code>. A new
     * <code>MergeScenarioStatistics</code> instance will be created and added if necessary.
     *
     * @param mergeScenario
     *         the <code>MergeScenario</code> to get the <code>MergeScenarioStatistics</code> for
     * @return the <code>MergeScenarioStatistics</code> for the given <code>MergeScenario</code>
     */
    public MergeScenarioStatistics getScenarioStatistics(MergeScenario<?> mergeScenario) {
        return scenarioStatistics.computeIfAbsent(mergeScenario, MergeScenarioStatistics::new);
    }

    /**
     * Returns all <code>MergeScenarioStatistics</code> currently added to this <code>Statistics</code> instance.
     *
     * @return the <code>MergeScenarioStatistics</code>
     */
    public List<MergeScenarioStatistics> getScenarioStatistics() {
        return scenarioStatistics.values().stream().collect(Collectors.toList());
    }

    /**
     * Adds a <code>MergeScenarioStatistics</code> instance to this <code>Statistics</code>. If there already is a
     * <code>MergeScenarioStatistics</code> for the <code>MergeScenario</code> stored in <code>statistics</code> it will
     * be added to the old value using {@link MergeScenarioStatistics#add(MergeScenarioStatistics)}.
     *
     * @param statistics
     *         the <code>MergeScenarioStatistics</code> to be added
     */
    public void addScenarioStatistics(MergeScenarioStatistics statistics) {
        scenarioStatistics.merge(statistics.getMergeScenario(), statistics, (o, n) -> {o.add(n); return o;});
    }

    /**
     * Removes the <code>MergeScenarioStatistics</code> for the given <code>scenario</code> from this
     * <code>Statistics</code> instance.
     *
     * @param scenario the <code>MergeScenario</code> whose <code>MergeScenarioStatistics</code> are to be removed.
     */
    public void removeScenarioStatistics(MergeScenario<?> scenario) {
        scenarioStatistics.remove(scenario);
    }

    /**
     * Returns an <code>IntSummaryStatistics</code> for the conflict ({@link MergeScenarioStatistics#getConflicts()})
     * statistics collected in all added <code>MergeScenarioStatistics</code>.
     *
     * @return the <code>IntSummaryStatistics</code> about conflicts that occurred
     */
    public IntSummaryStatistics getConflictStatistics() {
        return scenarioStatistics.values().stream().collect(Collectors.summarizingInt(MergeScenarioStatistics::getConflicts));
    }

    /**
     * Returns whether any added <code>MergeScenarioStatistics</code> instance recorded more than 0 conflicts.
     *
     * @return true iff any added <code>MergeScenarioStatistics</code> recorded conflicts
     */
    public boolean hasConflicts() {
        return scenarioStatistics.values().stream().anyMatch(s -> s.getConflicts() > 0);
    }

    /**
     * Adds all <code>MergeScenarioStatistics</code> in <code>other</code> to the corresponding
     * <code>MergeScenarioStatistics</code> added to <code>this</code>. If a <code>MergeScenarioStatistics</code> in
     * <code>other</code> has no partner in <code>this</code> it will simply be added to <code>this</code>.
     *
     * @param other
     *         the <code>Statistics</code> to add to <code>this</code>
     * @see MergeScenarioStatistics#add(MergeScenarioStatistics)
     */
    public void add(Statistics other) {
        for (Map.Entry<MergeScenario<?>, MergeScenarioStatistics> entry : other.scenarioStatistics.entrySet()) {
            getScenarioStatistics(entry.getKey()).add(entry.getValue());
        }
    }

    /**
     * Stores the collected statistics in XML format in the given <code>File</code>. The <code>File</code> will
     * be overwritten if it exists.
     *
     * @param file
     *         the <code>File</code> to write the statistics to
     * @throws FileNotFoundException
     *         if an exception occurs accessing the <code>File</code>
     */
    public void printXML(File file, MergeContext context) throws FileNotFoundException {

        if (!check(file)) {
            return;
        }

        try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
            printXML(os, context);
        } catch (FileNotFoundException fnf) {
            throw fnf;
        } catch (IOException e) {
            LOG.log(Level.WARNING, e, () -> "Exception while closing an OutputStream.");
        }
    }

    /**
     * Writes the collected statistics in XML format to the given <code>OutputStream</code>.
     *
     * @param os
     *         the <code>OutputStream</code> to write to
     */
    public void printXML(OutputStream os, MergeContext context) {
        buildSerializer(context).toXML(this, os);
    }

    /**
     * Writes a human readable representation of the collected statistics to the given <code>File</code>.
     *
     * @param file
     *         the <code>File</code> to write the statistics to
     * @throws FileNotFoundException
     *         if an exception occurs accessing the <code>File</code>
     */
    public void print(File file) throws FileNotFoundException {

        if (!check(file)) {
            return;
        }

        try (PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            print(ps);
        }
    }

    /**
     * Writes a human readable representation of the collected statistics to the given <code>PrintStream</code>.
     *
     * @param ps
     *         the <code>PrintStream</code> to write to
     */
    public void print(PrintStream ps) {

        for (Iterator<MergeScenarioStatistics> it = getScenarioStatistics().iterator(); it.hasNext(); ) {
            it.next().print(ps);

            if (it.hasNext()) {
                ps.println();
            }
        }
    }

    /**
     * Checks whether <code>file</code> is a valid file to write to and if necessary creates the directory structure
     * above it.
     *
     * @param file
     *         the <code>File</code> to check
     * @return true iff the file is valid
     */
    private boolean check(File file) {

        if (file.isDirectory()) {
            LOG.warning(() -> file.getAbsolutePath() + " is a directory and can't be written to.");
            return false;
        }

        File parent = file.getAbsoluteFile().getParentFile();

        if (parent == null) {
            LOG.warning(() -> file.getAbsolutePath() + " does not have a parent directory.");
            return false;
        }

        if (!(parent.exists() || parent.mkdirs())) {
            LOG.warning(() -> "Could not create the directory structure for " + parent.getAbsolutePath());
            return false;
        }

        return true;
    }

    /**
     * Constructs an {@link XStream} for serializing {@link Statistics}.
     *
     * @param context
     *         the {@link MergeContext} containing the configuration options to be used
     * @return the {@link XStream} for serializing {@link Statistics}
     */
    private XStream buildSerializer(MergeContext context) {
        XStream serializer;

        serializer = new XStream();
        serializer.setMode(XStream.NO_REFERENCES);

        serializer.alias(MergeScenarioStatistics.class.getSimpleName().toLowerCase(), MergeScenarioStatistics.class);
        serializer.useAttributeFor(MergeScenarioStatistics.class, "status");

        serializer.addImplicitMap(MergeScenarioStatistics.class, "runtimes", Runtime.class, "label");

        for (Field field : context.getExcludeStatisticsMSSFields()) {
            serializer.omitField(MergeScenarioStatistics.class, field.getName());
        }

        serializer.alias(Runtime.class.getSimpleName().toLowerCase(), Runtime.class);
        serializer.useAttributeFor(Runtime.class, "label");
        serializer.useAttributeFor(Runtime.class, "timeMS");

        serializer.aliasType(Artifact.class.getSimpleName().toLowerCase(), Artifact.class);

        serializer.alias(Revision.class.getSimpleName().toLowerCase(), Revision.class);
        serializer.useAttributeFor(Revision.class, "name");

        serializer.alias(Statistics.class.getSimpleName().toLowerCase(), Statistics.class);
        serializer.omitField(Statistics.class, "currentFileMergeScenario");
        serializer.addImplicitMap(Statistics.class, "scenarioStatistics", MergeScenarioStatistics.class, "mergeScenario");

        serializer.alias(KeyEnums.Type.class.getSimpleName().toLowerCase(), KeyEnums.Type.class);
        serializer.alias(KeyEnums.Level.class.getSimpleName().toLowerCase(), KeyEnums.Level.class);

        serializer.alias(Matching.class.getSimpleName().toLowerCase(), Matching.class);
        serializer.alias(Matching.class.getSimpleName().toLowerCase(), LookAheadMatching.class);
        serializer.omitField(Matching.class, "highlightColor");

        for (Field field : ElementStatistics.class.getDeclaredFields()) {
            serializer.useAttributeFor(ElementStatistics.class, field.getName());
        }
        serializer.alias(ElementStatistics.class.getSimpleName().toLowerCase(), ElementStatistics.class);

        for (Field field : MergeStatistics.class.getDeclaredFields()) {
            serializer.useAttributeFor(MergeStatistics.class, field.getName());
        }
        serializer.alias(MergeStatistics.class.getSimpleName().toLowerCase(), MergeStatistics.class);

        serializer.registerConverter(new Converter() {

            private static final String TYPE_ATTR = "type";

            private ImplicitCollectionMapper mapper = new ImplicitCollectionMapper(serializer.getMapper(),
                                                                                   serializer.getReflectionProvider());
            private CollectionConverter c = new CollectionConverter(mapper);

            @Override
            public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
                MergeScenario<?> mScenario = (MergeScenario<?>) source;

                writer.addAttribute(TYPE_ATTR, mScenario.getMergeType().toString());
                c.marshal(mScenario.asList(), writer, context);
            }

            @Override
            public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
                String name = XStream.class.getSimpleName();
                String name2 = Statistics.class.getSimpleName();
                String msg = String.format("The %s in the %s class can not be used for deserialization.", name, name2);
                throw new RuntimeException(msg);
            }

            @Override
            public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
                return type.equals(MergeScenario.class);
            }
        });

        serializer.registerConverter(new Converter() {

            private static final String SUBCLASS_ATTR = "subclass";
            private static final String TYPE_ATTR = "type";
            private static final String ID_ATTR = "id";

            @Override
            public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
                Artifact<?> artifact = (Artifact<?>) source;

                writer.addAttribute(SUBCLASS_ATTR, artifact.getClass().getSimpleName());
                writer.addAttribute(TYPE_ATTR, artifact.getType().name());
                writer.addAttribute(ID_ATTR, artifact.getId());
            }

            @Override
            public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
                String name = XStream.class.getSimpleName();
                String name2 = Statistics.class.getSimpleName();
                String msg = String.format("The %s in the %s class can not be used for deserialization.", name, name2);
                throw new RuntimeException(msg);
            }

            @Override
            public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
                return Artifact.class.isAssignableFrom(type);
            }
        });

        return serializer;
    }
}
