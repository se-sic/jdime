package de.fosd.jdime.strdump;

/**
 * Enumeration of the currently configurable dump modes.
 */
public enum DumpMode {

    /**
     * Do not dump.
     */
    NONE,

    /**
     * Dump the tree in plaintext format (for shell output).
     */
    PLAINTEXT_TREE,

    /**
     * Dump the tree in graphviz format.
     */
    GRAPHVIZ_TREE,

    /**
     * Dump the tree in TGF format.
     */
    TGF_TREE,

    /**
     * Read the file and dump its contents.
     */
    FILE_DUMP,

    /**
     * Parse the file to an AST and pretty-print it.
     */
    PRETTY_PRINT_DUMP
}
