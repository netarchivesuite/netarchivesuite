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

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton class which maintains the basic data structure and methods for the indexer.
 */
public class IndexerQueue {

    /** The logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(IndexerQueue.class);

    /** The unique instance of this class. */
    private static IndexerQueue instance;

    /** This is the basic underlying datastructure of the indexer - a queue of files waiting to be indexed. */
    private static LinkedBlockingQueue<ArchiveFile> queue;

    /**
     * Factory method for obtaining the unique instance of this class.
     *
     * @return the instance.
     */
    public static synchronized IndexerQueue getInstance() {
        if (instance == null) {
            instance = new IndexerQueue();
        }
        return instance;
    }

    /**
     * Private constructor for this method. Initialises an empty queue.
     */
    private IndexerQueue() {
        queue = new LinkedBlockingQueue<ArchiveFile>();
    }

    /**
     * Check the database for any new ArchiveFile objects and add them to the queue.
     */
    public synchronized void populate() {
        List<ArchiveFile> files = (new ArchiveFileDAO()).getFilesAwaitingIndexing();
        if (!files.isEmpty()) {
            log.info("Will now add '{}' unindexed files from object store to queue (if they are not already queued).",
                    files.size());
        }
        for (ArchiveFile file : files) {
            if (!queue.contains(file)) {
                log.info("Adding file '{}' to indexing queue.", file.getFilename());
                queue.add(file);
                log.info("Files in queue: '{}'", queue.size());
            }
        }
    }

    /**
     * Sequentially take objects from the queue and index them, blocking indefinitely while waiting for new objects to
     * be added to the queue. It is intended that multiple threads should run this method simultaneously.
     */
    public void consume() {
        while (true) {
            try {
                ArchiveFile file = null;
                try {
                    file = queue.take();
                    log.info("Taken file '{}' from indexing queue.", file.getFilename());
                    log.info("Files in queue: '{}'", queue.size());
                } catch (InterruptedException e) {
                    log.error("Unexpected interrupt in indexer while waiting for new elements", e);
                }
                file.index();
            } catch (Exception e) { // Fault Barrier
                log.warn("Caught exception at fault barrier for {}", Thread.currentThread().getName(), e);
            }
        }
    }

    /**
     * Convenience method for use in unit tests.
     */
    protected static void resestSingleton() {
        instance = null;
        if (queue != null) {
            queue.clear();
        }
    }
}
