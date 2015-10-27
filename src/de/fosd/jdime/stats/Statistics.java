package de.fosd.jdime.stats;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.ImplicitCollectionMapper;
import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeScenario;
import de.fosd.jdime.common.Revision;

/**
 * A collection of <code>MergeScenarioStatistics</code> containing collected statistics about
 * <code>MergeScenario</code>s that were merged during a run of JDime.
 */
public class Statistics {

    private static final XStream serializer;

    static {
        serializer = new XStream();
        serializer.setMode(XStream.NO_REFERENCES);

        serializer.aliasType(Artifact.class.getSimpleName().toLowerCase(), Artifact.class);

        serializer.alias(Revision.class.getSimpleName().toLowerCase(), Revision.class);
        serializer.useAttributeFor(Revision.class, "name");

        serializer.alias(Statistics.class.getSimpleName().toLowerCase(), Statistics.class);
        serializer.addImplicitMap(Statistics.class, "scenarioStatistics", MergeScenarioStatistics.class, "mergeScenario");

        serializer.alias(KeyEnums.Type.class.getSimpleName().toLowerCase(), KeyEnums.Type.class);
        serializer.alias(KeyEnums.Level.class.getSimpleName().toLowerCase(), KeyEnums.Level.class);

        serializer.alias(MergeScenarioStatistics.class.getSimpleName().toLowerCase(), MergeScenarioStatistics.class);

        for (Field field : ElementStatistics.class.getDeclaredFields()) {
            serializer.useAttributeFor(ElementStatistics.class, field.getName());
        }
        serializer.alias(ElementStatistics.class.getSimpleName().toLowerCase(), ElementStatistics.class);

        for (Field field : MergeStatistics.class.getDeclaredFields()) {
            serializer.useAttributeFor(MergeStatistics.class, field.getName());
        }
        serializer.alias(MergeStatistics.class.getSimpleName().toLowerCase(), MergeStatistics.class);

        serializer.registerConverter(new Converter() {

            private ImplicitCollectionMapper mapper = new ImplicitCollectionMapper(serializer.getMapper());
            private CollectionConverter c = new CollectionConverter(mapper);

            @Override
            public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
                MergeScenario mScenario = (MergeScenario) source;

                writer.startNode("type");
                context.convertAnother(mScenario.getMergeType());
                writer.endNode();

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
            public boolean canConvert(Class type) {
                return type.equals(MergeScenario.class);
            }
        });

        serializer.registerConverter(new Converter() {
            @Override
            public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
                Artifact<?> artifact = (Artifact<?>) source;
                writer.addAttribute("id", artifact.getId());
            }

            @Override
            public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
                String name = XStream.class.getSimpleName();
                String name2 = Statistics.class.getSimpleName();
                String msg = String.format("The %s in the %s class can not be used for deserialization.", name, name2);
                throw new RuntimeException(msg);
            }

            @Override
            public boolean canConvert(Class type) {
                return Artifact.class.isAssignableFrom(type);
            }
        });
    }

    private Map<MergeScenario<?>, MergeScenarioStatistics> scenarioStatistics;

    /**
     * Constructs a new <code>Statistics</code> object.
     */
    public Statistics() {
        this.scenarioStatistics = new HashMap<>();
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
     * be replaced.
     *
     * @param statistics
     *         the <code>MergeScenarioStatistics</code> to be adde
     */
    public void addScenarioStatistics(MergeScenarioStatistics statistics) {
        scenarioStatistics.put(statistics.getMergeScenario(), statistics);
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
     * @throws IOException
     *         if an <code>IOException</code> occurs accessing the <code>File</code>
     */
    public void storeXML(File file) throws IOException {
        try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
            storeXML(os);
        }
    }

    /**
     * Writes the collected statistics in XML format to the given <code>OutputStream</code>.
     *
     * @param os
     *         the <code>OutputStream</code> to write to
     */
    public void storeXML(OutputStream os) {
        serializer.toXML(this, os);
    }

    /**
     * Prints a human readable representation of the collected statistics to the given <code>PrintStream</code>.
     *
     * @param os
     *         the <code>PrintStream</code> to write to
     */
    public void print(PrintStream os) {
        getScenarioStatistics().forEach(statistics -> statistics.print(os));
    }
}
