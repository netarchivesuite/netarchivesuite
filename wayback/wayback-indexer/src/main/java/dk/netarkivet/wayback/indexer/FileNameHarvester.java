/*
 * #%L
 * Netarchivesuite - wayback
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.wayback.indexer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.PreservationArcRepositoryClient;
import dk.netarkivet.common.distribute.bitrepository.Bitrepository;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.BitmagUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.DatedFileListJob;
import dk.netarkivet.common.utils.batch.FileListJob;
import dk.netarkivet.wayback.WaybackSettings;

public class FileNameHarvester {

    /** Logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(FileNameHarvester.class);


    /**
     * This method harvests a list of all the files currently in the arcrepository and appends any new ones found to the
     * ArchiveFile object store.
     */
    public static synchronized void harvestAllFilenames() {
        ArchiveFileDAO dao = new ArchiveFileDAO();
        //TODO I believe that by default this only fetches max 10000 files. Look at the query interface
        //in bitmag
        if (Settings.getBoolean(CommonSettings.USING_HADOOP)) {
            // Initialize connection to the bitrepository
            Bitrepository bitrep = BitmagUtils.initBitrep();
            String collection = Settings.get(dk.netarkivet.common.distribute.bitrepository.BitmagUtils.BITREPOSITORY_COLLECTIONID);
            List<String> fileNames = bitrep.getFileIds(collection, Settings.get(
                    dk.netarkivet.common.distribute.bitrepository.BitmagUtils.BITREPOSITORY_USEPILLAR));
            if (fileNames != null) {
                for (String fileName : fileNames) {
                    if (!dao.exists(fileName)) {
                        createArchiveFileInDB(fileName, dao);
                    }
                }
            } else {
                log.info("No files found in collection '{}'", collection);
            }
        } else {
            PreservationArcRepositoryClient client = ArcRepositoryClientFactory.getPreservationInstance();
            BatchStatus status = client.batch(new FileListJob(), Settings.get(WaybackSettings.WAYBACK_REPLICA));
            getResultFileAndCreateInDB(status, dao);
        }
    }

    /**
     * This method harvests a list of all the recently added files in the archive.
     */
    public static synchronized void harvestRecentFilenames() {
        ArchiveFileDAO dao = new ArchiveFileDAO();
        long timeAgo = Settings.getLong(WaybackSettings.WAYBACK_INDEXER_RECENT_PRODUCER_SINCE);
        Date since = new Date(System.currentTimeMillis() - timeAgo);

        if (Settings.getBoolean(CommonSettings.USING_HADOOP)) {
            log.info("Harvesting of recent files not yet implemented with bitmag.");
            //TODO can this be implemented using queries in bitmag

/*            Bitrepository bitrep = BitmagUtils.initBitrep();
            String collection = Settings.get(BitmagArcRepositoryClient.BITREPOSITORY_COLLECTIONID);
            List<String> fileNames = bitrep.getFileIds(collection);
            if (fileNames != null) {
                for (String fileName : fileNames) {
                    File file = bitrep.getFile(fileName, collection, null);
                    if (file.lastModified() > since.getTime()) {
                        createArchiveFileInDB(fileName, dao);
                    }
                }
            } else {
                log.info("No files found in collection '{}'", collection);
            }*/
        } else {
            PreservationArcRepositoryClient client = ArcRepositoryClientFactory.getPreservationInstance();
            BatchStatus status = client
                    .batch(new DatedFileListJob(since), Settings.get(WaybackSettings.WAYBACK_REPLICA));
            getResultFileAndCreateInDB(status, dao);
        }
    }

    /**
     * Helper method for handling results from BatchStatus and putting it in the database.
     * @param status The BatchStatus with results from batch()
     * @param dao The DAO through which the database is accessed.
     */
    private static void getResultFileAndCreateInDB(BatchStatus status, ArchiveFileDAO dao) {
        RemoteFile results = status.getResultFile();
        InputStream is = results.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if (!dao.exists(line.trim())) {
                    createArchiveFileInDB(line, dao);
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
     * Helper method to create an ArchiveFile from a given filename and put it in the database.
     * @param fileName The filename to create.
     * @param dao The DAO through which the database is accessed.
     */
    private static void createArchiveFileInDB(String fileName, ArchiveFileDAO dao) {
        ArchiveFile file = new ArchiveFile();
        file.setFilename(fileName.trim());
        file.setIndexed(false);
        log.info("Creating object store entry for '{}'", file.getFilename());
        dao.create(file);
    }
}
