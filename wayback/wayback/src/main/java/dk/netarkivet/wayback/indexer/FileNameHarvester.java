package dk.netarkivet.wayback.indexer;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Date;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.PreservationArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.utils.batch.DatedFileListJob;
import dk.netarkivet.common.utils.batch.FileListJob;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.wayback.WaybackSettings;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileNameHarvester {

    /** Logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(FileNameHarvester.class);

    /**
     * This method harvests a list of all the files currently in the
     * arcrepository and appends any new ones found to the ArchiveFile
     * object store.
     */
    public static synchronized void harvestAllFilenames() {
        ArchiveFileDAO dao = new ArchiveFileDAO();
        PreservationArcRepositoryClient client = ArcRepositoryClientFactory
                .getPreservationInstance();
        BatchStatus status = client.batch(new FileListJob(),
                Settings.get(WaybackSettings.WAYBACK_REPLICA));
        RemoteFile results = status.getResultFile();
        InputStream is = results.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        try {
            while ((line = reader.readLine()) != null){
                if (!dao.exists(line.trim())) {
                    ArchiveFile file = new ArchiveFile();
                    file.setFilename(line.trim());
                    file.setIndexed(false);
                    log.info("Creating object store entry for '{}'", file.getFilename());
                    dao.create(file);
                } // If the file is already known in the persistent store, no
                // action needs to be taken.
            }
        } catch (IOException e) {
            throw new IOFailure("Error reading remote file", e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }


    /**
     * This method harvests a list of all the recently added files
     * in the archive.
     */
    public static synchronized void harvestRecentFilenames() {
        ArchiveFileDAO dao = new ArchiveFileDAO();
        PreservationArcRepositoryClient client = ArcRepositoryClientFactory.getPreservationInstance();
        long timeAgo = Settings.getLong(WaybackSettings.WAYBACK_INDEXER_RECENT_PRODUCER_SINCE);
        Date since = new Date(System.currentTimeMillis() - timeAgo);
        BatchStatus status = client.batch(new DatedFileListJob(since),
                Settings.get(WaybackSettings.WAYBACK_REPLICA));
        RemoteFile results = status.getResultFile();
        InputStream is = results.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        try {
            while ((line = reader.readLine()) != null){
                if (!dao.exists(line.trim())) {
                    ArchiveFile file = new ArchiveFile();
                    file.setFilename(line.trim());
                    file.setIndexed(false);
                    log.info("Creating object store entry for '{}'", file.getFilename());
                    dao.create(file);
                } // If the file is already known in the persistent store, no
                // action needs to be taken.
            }
        } catch (IOException e) {
            throw new IOFailure("Error reading remote file", e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }
}
