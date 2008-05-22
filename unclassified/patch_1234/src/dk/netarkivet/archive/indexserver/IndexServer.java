/* File:                $Id$
 * Revision:            $Revision$
 * Author:              $Author$
 * Date:                $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.archive.indexserver;

import dk.netarkivet.archive.indexserver.distribute.IndexRequestServer;
import dk.netarkivet.common.distribute.indexserver.RequestType;
import dk.netarkivet.common.utils.CleanupIF;

import java.util.Set;

/** Index server.
 * Handles request for lucene indexes of crawl logs and cdx indexes of jobs,
 * using two multifilebasedcache objects as handlers.
 *
 * The server sets up handlers for three kinds of indexes (as defined by
 * RequestType):
 * A CDX index, where each index is one file, gzip-compressed.
 * A DEDUP_CRAWL_LOG index, where each index is multiple files, gzip-compressed,
 * making up a Lucene index of non-HTML files.
 * A FULL_CRAWL_LOG index, where each index is multiple files, gzip-compressed,
 * making up a Lucene index of all files.
 * */
public class IndexServer implements CleanupIF{
    /** The remote server that hands us indexes. */
    private IndexRequestServer remoteServer;
    /** The singleton instance of this class. */
    private static IndexServer instance;

    /** Instantiates the two handlers, and starts listening for requests. */
    public IndexServer() {
        FileBasedCache<Set<Long>> cdxCache = new CDXIndexCache();
        FileBasedCache<Set<Long>> dedupCrawlLogCache
                = new DedupCrawlLogIndexCache();
        FileBasedCache<Set<Long>> fullCrawlLogCache
                = new FullCrawlLogIndexCache();
        remoteServer = IndexRequestServer.getInstance();
        remoteServer.setHandler(RequestType.CDX, cdxCache);
        remoteServer.setHandler(RequestType.DEDUP_CRAWL_LOG,
                dedupCrawlLogCache);
        remoteServer.setHandler(RequestType.FULL_CRAWL_LOG,
                fullCrawlLogCache);
    }

    /** Get the unique index server instance.
     *
     * @return The instance;
     */
    public static IndexServer getInstance() {
        if (instance == null) {
            instance = new IndexServer();
        }
        return instance;
    }

    /** Close the server.
     */
    public void cleanup() {
        remoteServer.close();
    }
}
