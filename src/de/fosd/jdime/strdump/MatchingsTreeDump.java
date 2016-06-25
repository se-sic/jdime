package de.fosd.jdime.strdump;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.matcher.matching.Matching;
import de.fosd.jdime.matcher.matching.Matchings;

public class MatchingsTreeDump {

    public <T extends Artifact<T>> void dump(Matchings<T> matchings, OutputStream os) {

        if (matchings.isEmpty()) {
            return;
        }

        Artifact<T> lRoot, rRoot;

        {
            Matching<T> first = matchings.iterator().next();

            lRoot = first.getLeft().findRoot();
            rRoot = first.getRight().findRoot();
        }

        PrintWriter out = new PrintWriter(new BufferedOutputStream(os));

        out.println("graph \"TollerGraph\" {%n");



        out.print("}");
    }
}
