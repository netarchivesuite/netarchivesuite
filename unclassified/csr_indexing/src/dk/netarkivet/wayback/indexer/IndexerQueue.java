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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import dk.netarkivet.common.exceptions.NotImplementedException;

/**
 * Singleton class which maintains the basic data structure and methods for
 * the indexer.
 */
public class IndexerQueue {

    /**
     * The unique instance of this class.
     */
    private static IndexerQueue instance;

    /**
     * This is the basic underlying datastructure of the indexer - a queue of
     * files waiting to be indexed.
     */
    private LinkedBlockingQueue<ArchiveFile> queue;

    /**
     * Factory method for obtaining the unique instance of this class.
     * @return the instance.
     */
    public static synchronized IndexerQueue getInstance() {
        if (instance == null) {
            return new IndexerQueue();
        } else {
            return instance;
        }
    }


    private IndexerQueue() {
        queue = new LinkedBlockingQueue<ArchiveFile>();
    }

    /**
     * Check the database for any new ArchiveFile objects and add them to the
     * queue.
     */
    public void populate() {
        throw new NotImplementedException("not yet implemented");
    }

    /**
     * Sequentially take objects from the queue and index them, blocking
     * indefinitely while waiting for new objects to be added to the queue.
     * It is intended 
     * that multiple threads should run this method simultaneously.
     */
    public void consume() {
        throw new NotImplementedException("not yet implemented");
    }
}
