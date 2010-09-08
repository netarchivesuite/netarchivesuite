/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package dk.netarkivet.wayback.indexer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.wayback.WaybackSettings;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The WaybackIndexer starts threads to find new files to be indexed and indexes
 * them.
 *
 * There is 1 producer thread which runs as a timer thread, for example once a
 * day, and runs first a FileNameHarvester to get a list of all files in the
 * archive after which it fills the indexer queue with any new files found.
 *
 * Simultaneously there is a family of consumer threads which wait for the
 * queue to be populated and take elements from it and index them.
 *
 */
public class WaybackIndexer implements CleanupIF {

    /**
     * The logger for this class.
     */
    final static Log log = LogFactory.getLog(WaybackIndexer.class);

    /**
     * The singleton instance of this class.
     */
    private static WaybackIndexer instance;

    /**
     * Factory method which creates a singleton wayback indexer and sets it
     * running. It has the side effect of creating the output directories
     * for the indexer if these do not already exist. It also reads files for
     * the initial ingest if necessary.
     * @return the indexer.
     */
    public static synchronized WaybackIndexer getInstance() {
          if (instance == null) {
              instance = new WaybackIndexer();
          }
          return instance;
    }

    /**
     * Private contructor.
     */
    private WaybackIndexer() {
        File temporaryBatchDir = Settings.getFile(
                WaybackSettings.WAYBACK_INDEX_TEMPDIR);
        File batchOutputDir = Settings.getFile(
                WaybackSettings.WAYBACK_BATCH_OUTPUTDIR);
        FileUtils.createDir(temporaryBatchDir);
        FileUtils.createDir(batchOutputDir);
        ingestInitialFiles();
        startProducerThread();
        startConsumerThreads();
    }

    /**
     * The file represented by WAYBACK_INDEXER_INITIAL_FILES
     * is read line by line and each line is ingested as an
     * already-indexed archive file
     */
    private static void ingestInitialFiles() {
        String initialFileString = Settings.
                    get(WaybackSettings.WAYBACK_INDEXER_INITIAL_FILES);
        if ("".equals(initialFileString)) {
            log.info("No initial list of indexed files is set");
            return;
        }
        File initialFile = null;
        try {
            initialFile = Settings.
                    getFile(WaybackSettings.WAYBACK_INDEXER_INITIAL_FILES);
        } catch (UnknownID e) {
            log.info("No initial list of indexed files is set");
            return;
        }
        if (!initialFile.isFile()) {
            throw new ArgumentNotValid("The file '" +
                                       initialFile.getAbsolutePath()
                                       + "' does not exist or is not a file");
        }
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(initialFile));
        } catch (FileNotFoundException e) {
            throw new IOFailure("Could not find file '" + initialFile + "'", e);
        }
        String fileName = null;
        ArchiveFileDAO dao = new ArchiveFileDAO();
        Date today = new Date();
        try {
            while ((fileName = br.readLine()) != null) {
                ArchiveFile archiveFile = new ArchiveFile();
                archiveFile.setFilename(fileName);
                archiveFile.setIndexed(true);
                archiveFile.setIndexedDate(today);
                if (!dao.exists(fileName)) {
                    log.info("Ingesting '" + fileName + "'");
                    dao.create(archiveFile);
                }
            }
        } catch (IOException e) {
            throw new IOFailure("Error reading file", e);
        }
    }

    /**
     * Starts the consumer threads which do the indexing by sending
     * concurrent batch jobs to the arcrepository.
     */
    private static void startConsumerThreads() {
        int consumerThreads = Settings.getInt(
                WaybackSettings.WAYBACK_INDEXER_CONSUMER_THREADS);
        for (int threadNumber = 0; threadNumber < consumerThreads;
             threadNumber++) {
             new Thread("ConsumerThread-" + threadNumber) {

                 @Override
                 public void run() {
                     super.run();
                     log.info("Started thread '" +
                               Thread.currentThread().getName() + "'");
                     IndexerQueue.getInstance().consume();
                     log.info("Ending thread '" +
                               Thread.currentThread().getName() + "'");

                 }
             }.start();
        }
    }

    /**
     * Starts the producer thread. This thread runs on a timer. It downloads a
     * list of all files in the archive and adds any new ones to the database. It
     * then checks the database for unindexed files and adds them to the queue.
     */
    private static void startProducerThread() {
        Long producerDelay =
            Settings.getLong(WaybackSettings.WAYBACK_INDEXER_PRODUCER_DELAY);
        Long producerInterval =
            Settings.getLong(WaybackSettings.WAYBACK_INDEXER_PRODUCER_INTERVAL);
        TimerTask producerTask = new TimerTask() {
            @Override
            public void run() {
                log.info("Starting producer thread");
                FileNameHarvester.harvest();
                IndexerQueue.getInstance().populate();
            }
        };
        Timer producerThreadTimer = new Timer("ProducerThread");
        producerThreadTimer.schedule
                (producerTask, producerDelay, producerInterval);
    }

    /**
     * Performs any necessary cleanup functions. These include cleaning any
     * partial batch output from the temporary batch output file and closing
     * the hibernate session factory.
     */
    public void cleanup() {
        log.info("Cleaning up WaybackIndexer");
        File temporaryBatchDir = Settings.getFile(
                WaybackSettings.WAYBACK_INDEX_TEMPDIR);
        FileUtils.removeRecursively(temporaryBatchDir);
        HibernateUtil.getSession().getSessionFactory().close();
    }
}
