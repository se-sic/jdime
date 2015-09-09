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
     * enum.
     */
    public enum TYPE {

        /**
         * A <code>FileArtifact</code> representing a file (not a directory).
         */
        FILE,

        /**
         * A <code>FileArtifact</code> representing a directory.
         */
        DIRECTORY,

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
    public enum LEVEL {

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
