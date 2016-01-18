package de.fosd.jdime.strdump;

import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.fosd.jdime.common.Artifact;

/**
 * Enumeration of the currently configurable dump modes.
 */
public enum DumpMode {

    /**
     * Do not dump.
     */
    NONE(new NoDump()),

    /**
     * Dump the tree in plaintext format (for shell output).
     */
    PLAINTEXT_TREE(new PlaintextTreeDump()),

    /**
     * Dump the tree in graphviz format.
     */
    GRAPHVIZ_TREE(new GraphvizTreeDump()),

    /**
     * Dump the tree in TGF format.
     */
    TGF_TREE(new TGFTreeDump()),

    /**
     * Read the file and dump its contents.
     */
    FILE_DUMP(new PrettyPrintDump()),

    /**
     * Parse the file to an AST and pretty-print it.
     */
    PRETTY_PRINT_DUMP(new PrettyPrintDump());

    /**
     * A <code>StringDumper</code> that returns an empty <code>String</code> and logs improper use.
     */
    private static class NoDump implements StringDumper {

        private static final Logger LOG = Logger.getLogger(DumpMode.class.getCanonicalName());

        @Override
        public <T extends Artifact<T>> String dump(Artifact<T> artifact, Function<Artifact<T>, String> getLabel) {
            LOG.log(Level.WARNING, () -> String.format("Attempted to dump using %s %s. Returning an empty String.",
                    DumpMode.class.getSimpleName(), NONE.name()));

            return "";
        }
    }

    private StringDumper dumper;

    /**
     * Constructs a new <code>DumpMode</code> variant representing the given <code>StringDumper</code>.
     *
     * @param dumper
     *         the <code>StringDumper</code> to represent
     */
    DumpMode(StringDumper dumper) {
        this.dumper = dumper;
    }

    /**
     * Returns the <code>StringDumper</code> for the <code>DumpMode</code>.
     *
     * @return the <code>StringDumper</code>
     */
    public StringDumper getDumper() {
        return dumper;
    }
}
