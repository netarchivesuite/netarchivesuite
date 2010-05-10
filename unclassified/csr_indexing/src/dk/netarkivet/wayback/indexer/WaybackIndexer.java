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

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.wayback.WaybackSettings;

/**
 * The WaybackIndexer starts threads to find new files to be indexed and index
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

    private static WaybackIndexer instance;

    /**
     * Factory method which creates a singleton wayback indexer and sets it
     * running.
     * @return the indexer.
     */
    public static synchronized WaybackIndexer getInstance() {
          if (instance == null) {
              instance = new WaybackIndexer();
          }
          return instance;
    }

    private WaybackIndexer() {
        File temporaryBatchDir = Settings.getFile(
                WaybackSettings.WAYBACK_INDEX_TEMPDIR);
        File batchOutputDir = Settings.getFile(
                WaybackSettings.WAYBACK_BATCH_OUTPUTDIR);
        FileUtils.createDir(temporaryBatchDir);
        FileUtils.createDir(batchOutputDir);
        startProducerThread();
        startConsumerThreads();
    }

    private static void startConsumerThreads() {
        int consumerThreads = Settings.getInt(
                WaybackSettings.WAYBACK_INDEXER_CONSUMER_THREADS);
        for (int threadNumber = 0; threadNumber < consumerThreads;
             threadNumber++) {
             new Thread("ConsumerThread-" + threadNumber) {
                 @Override
                 public void run() {
                     super.run();
                     IndexerQueue.getInstance().consume();
                 }
             }.start();
        }
    }

    private static void startProducerThread() {
        Long producerDelay =
               Settings.getLong(WaybackSettings.WAYBACK_INDEXER_PRODUCER_DELAY);
        Long producerInterval =
           Settings.getLong(WaybackSettings.WAYBACK_INDEXER_PRODUCER_INTERVAL);
        TimerTask producerTask = new TimerTask() {
            @Override
            public void run() {
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
        File temporaryBatchDir = Settings.getFile(
                WaybackSettings.WAYBACK_INDEX_TEMPDIR);
        FileUtils.removeRecursively(temporaryBatchDir);
        HibernateUtil.getSession().getSessionFactory().close();
    }
}
