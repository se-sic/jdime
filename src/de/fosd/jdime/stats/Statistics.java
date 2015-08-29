package de.fosd.jdime.stats;

import de.fosd.jdime.common.LangElem;
import de.fosd.jdime.common.Revision;

import java.util.HashMap;
import java.util.Map;

public class Statistics {

    private Map<Revision, Map<LangElem, ASTStatistics>> astStats;
    private Map<Revision, Map<LangElem, ElementStatistics>> elemStats;
    private Map<Revision, MergeStatistics> mergeStats;

    public Statistics() {
        this.astStats = new HashMap<>();
        this.elemStats = new HashMap<>();
        this.mergeStats = new HashMap<>();
    }
}
