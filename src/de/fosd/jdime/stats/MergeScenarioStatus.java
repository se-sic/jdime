package de.fosd.jdime.stats;

import de.fosd.jdime.config.merge.MergeScenario;

/**
 * Enumeration of the possible execution results of a {@link MergeScenario}.
 */
public enum MergeScenarioStatus {

    /**
     * Indicates that the merge was successfully executed.
     */
    OK,

    /**
     * Indicates that there was an exception while executing the {@link MergeScenario}.
     */
    FAILED
}
