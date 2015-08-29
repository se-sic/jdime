package de.fosd.jdime.stats;

import de.fosd.jdime.common.LangElem;
import de.fosd.jdime.common.Revision;

public class ElementStatistics {

    /**
     * The type of AST nodes the statistics are for. May be one of {@link LangElem#CLASS} or {@link LangElem#METHOD}.
     */
    private LangElem forElem;

    /**
     * The revision the statistics for the above <code>LangElem</code> are for.
     */
    private Revision revision;

    private int total;
    private int numAdded;
    private int numDeleted;
    private int numChanged; // TODO remove changed or unchanged (whats easier to compute)
    private int numUnchanged;
    private int numOccurInConflict;

    public ElementStatistics(LangElem forElem, Revision revision) {
        this.forElem = forElem;
        this.revision = revision;
        this.total = 0;
        this.numAdded = 0;
        this.numDeleted = 0;
        this.numChanged = 0;
        this.numUnchanged = 0;
        this.numOccurInConflict = 0;
    }
}
