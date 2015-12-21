package de.fosd.jdime.strdump;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.fosd.jdime.common.FileArtifact;
import org.apache.commons.io.FileUtils;

/**
 * Dumps the given <code>FileArtifacts</code> content to a <code>String</code>.
 */
public class FileDump extends StringDumper<FileArtifact> {

    private static final Logger LOG = Logger.getLogger(FileDump.class.getCanonicalName());

    /**
     * Constructs a new <code>StringDumper</code> for the given <code>Artifact</code>.
     *
     * @param artifact
     *         the <code>Artifact</code> to dump to a <code>String</code>
     */
    public FileDump(FileArtifact artifact) {
        super(artifact);
    }

    @Override
    protected void buildString() {

        try {
            builder.append(FileUtils.readFileToString(artifact.getFile(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOG.log(Level.WARNING, e, () -> "Can not dump the contents of " + artifact);
        }
    }
}
