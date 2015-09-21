package de.fosd.jdime.stats;

/**
 * Container class to hold enums used as categories for the <code>Statistics</code> class.
 */
public final class KeyEnums {

    /**
     * Utility class.
     */
    private KeyEnums() {}

    /**
     * <code>Artifact</code>s in a tree to be merged (be it an AST or a directory tree) are of one of the types in this
     * enum. <code>FILE</code>, <code>DIRECTORY</code> and <code>LINE</code> are used to address corresponding
     * statistics container in the <code>Statistics</code> class.
     */
    public enum Type {

        /**
         * A <code>FileArtifact</code> representing a file (not a directory).
         */
        FILE,

        /**
         * A <code>FileArtifact</code> representing a directory.
         */
        DIRECTORY,

        /**
         * A line in the output of JDime.
         */
        LINE,

        /**
         * Any AST node.
         */
        NODE,

        /**
         * An AST node representing a class declaration.
         */
        CLASS,

        /**
         * An AST node representing a method declaration.
         */
        METHOD
    }

    /**
     * AST nodes occur on one of the levels represented by this enum.
     */
    public enum Level {

        /**
         * Everything above class declaration, e.g. import statements.
         */
        TOP,

        /**
         * Everything within class declaration but outside methods.
         */
        CLASS,

        /**
         * Everything inside methods.
         */
        METHOD
    }
}
