package dk.netarkivet.wayback.indexer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to enable retry of indexing for selected files after they have reached maxFailedAttempts.
 */
public class ResetFailedFiles {

    /** The logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(ResetFailedFiles.class);

    /**
     * Usage: java -cp dk.netarkivet.wayback.jar
     * -Ddk.netarkivet.settings.file=/home/test/TEST12/conf/settings_WaybackIndexerApplication.xml
     * -Dsettings.common.applicationInstanceId=RESET_FILES
     * dk.netarkivet.wayback.indexer.ResetFailedFiles file1 file2 ...
     *
     * The given files are reset so that they appear never to have failed an indexing attempt. They will therefore
     * be placed in the index queue the next time the indexer runs.
     * @param args  the file names
     */
    public static void main(String[] args) {
        ArchiveFileDAO dao = new ArchiveFileDAO();
        for (String filename: args) {
            ArchiveFile archiveFile = dao.read(filename);
            if (archiveFile != null) {
                log.info("Resetting to 0 failures for '{}'", archiveFile.getFilename());
                archiveFile.setIndexingFailedAttempts(0);
                dao.update(archiveFile);
            } else {
                log.warn("Attempt to process unknown file '{}'", filename);
            }
        }
    }

}
