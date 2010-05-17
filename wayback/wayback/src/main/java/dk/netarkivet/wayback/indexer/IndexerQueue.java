/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *   USA
 */
package dk.netarkivet.wayback.indexer;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import dk.netarkivet.common.exceptions.IllegalState;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Singleton class which maintains the basic data structure and methods for
 * the indexer.
 */
public class IndexerQueue {

    private static Log log = LogFactory.getLog(IndexerQueue.class);

    /**
     * The unique instance of this class.
     */
    private static IndexerQueue instance;

    /**
     * This is the basic underlying datastructure of the indexer - a queue of
     * files waiting to be indexed.
     */
    private static LinkedBlockingQueue<ArchiveFile> queue;

    /**
     * Factory method for obtaining the unique instance of this class.
     * @return the instance.
     */
    public static synchronized IndexerQueue getInstance() {
        if (instance == null) {
            instance = new IndexerQueue();
        }
        return instance;
    }


    private IndexerQueue() {
        queue = new LinkedBlockingQueue<ArchiveFile>();
    }

    /**
     * Check the database for any new ArchiveFile objects and add them to the
     * queue.
     */
    public synchronized void populate() {
        List<ArchiveFile> files = (new ArchiveFileDAO()).getFilesAwaitingIndexing();
        for (ArchiveFile file: files) {
            if (!queue.contains(file)) {
                log.debug("Adding file '" + file.getFilename() + "' to indexing queue.");
                queue.add(file);
                log.debug("Files in queue: '" + queue.size() + "'");
            }
        }
    }

    /**
     * Sequentially take objects from the queue and index them, blocking
     * indefinitely while waiting for new objects to be added to the queue.
     * It is intended 
     * that multiple threads should run this method simultaneously.
     */
    public void consume() {
        while (true) {
            ArchiveFile file = null;
            try {
                file = queue.take();
                log.debug("Taken file '" + file.getFilename() + "' from indexing queue.");
                log.debug("Files in queue: '" + queue.size() + "'");                
            } catch (InterruptedException e) {
                String message = "Unexpected interrupt in indexer while waiting "
                                 + "for new elements";
                log.error(message, e);
                throw new IllegalState(message, e);
            }
            file.index();
        }
    }

    protected static void resestSingleton() {
        instance = null;
        if (queue != null) {
            queue.clear();
        }
    }
}
