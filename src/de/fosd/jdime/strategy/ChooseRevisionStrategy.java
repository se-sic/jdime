package de.fosd.jdime.strategy;

import de.fosd.jdime.artifact.file.FileArtifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.config.merge.MergeScenario;
import de.fosd.jdime.config.merge.Revision;
import de.fosd.jdime.execption.AbortException;
import de.fosd.jdime.operations.MergeOperation;
import de.fosd.jdime.stats.MergeScenarioStatistics;
import de.fosd.jdime.stats.Runtime;
import de.fosd.jdime.stats.Statistics;
import de.fosd.jdime.util.parser.ParseResult;

import java.util.Map;
import java.util.logging.Logger;

import static de.fosd.jdime.stats.Runtime.MERGE_LABEL;

/**
 * A merge strategy that, instead of merging, outputs the contents of one of the input revision (i.e., files)
 * to the target file.
 */
public class ChooseRevisionStrategy extends MergeStrategy<FileArtifact> {

    private static final Logger LOG = Logger.getLogger(ChooseRevisionStrategy.class.getCanonicalName());

    private final Revision revToChoose;

    /**
     * Constructs a new {@link ChooseRevisionStrategy}
     *
     * @param revToChoose the {@link Revision} to choose when 'merging'
     */
    public ChooseRevisionStrategy(Revision revToChoose) {
        this.revToChoose = revToChoose;
    }

    @Override @SuppressWarnings("try")
    public void merge(MergeOperation<FileArtifact> operation, MergeContext context) {
        LOG.fine(() -> "Instead of merging, JDime is configure to choose revision " + revToChoose);

        MergeScenario<FileArtifact> mergeScenario = operation.getMergeScenario();
        Map<Revision, FileArtifact> opArtifacts = mergeScenario.getArtifacts();

        if (!opArtifacts.containsKey(revToChoose)) {
            LOG.severe("Can no choose revision " + revToChoose + " as it was not found in the input artifacts.");
            throw new AbortException("Revision " + revToChoose + " not found.");
        }

        FileArtifact faToChoose = opArtifacts.get(revToChoose);
        String mergeResult;

        FileArtifact target = operation.getTarget();

        Runtime merge = new Runtime(MERGE_LABEL);

        try (Runtime.Measurement m = merge.time()) {
            // I suppose this is what "merging" is for this strategy...
            mergeResult = faToChoose.getContent();

            if (!context.isDiffOnly()) {
                target.setContent(faToChoose.getContent());
            }
        }

        LOG.fine(() -> String.format("%s merge time was %d ms.", getClass().getSimpleName(), merge.getTimeMS()));

        if (context.hasStatistics()) {
            Statistics statistics = context.getStatistics();
            MergeScenarioStatistics scenarioStatistics = statistics.getScenarioStatistics(operation.getMergeScenario());
            scenarioStatistics.setStrategy(getClass());

            ParseResult res = scenarioStatistics.setLineStatistics(mergeResult);

            if (res.getStats().getConflicts() > 0) {
                scenarioStatistics.getFileStatistics().incrementNumOccurInConflict();
            }

            scenarioStatistics.putRuntime(merge);
        }
    }
}
