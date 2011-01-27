/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.wayback;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.wayback.ResourceStore;
import org.archive.wayback.core.CaptureSearchResult;
import org.archive.wayback.core.Resource;
import org.archive.wayback.exception.ResourceNotAvailableException;
import org.archive.wayback.resourcestore.resourcefile.ResourceFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ViewerArcRepositoryClient;
import dk.netarkivet.common.utils.Settings;

/**
 * This is the connector between netarchivesuite and wayback. And is based 
 * on the NetarchiveResourceStore, and the implementations of ResourceStore
 * distributed with wayback-1.4.2.
 *
 */
public class NetarchiveCacheResourceStore implements ResourceStore {

    /** JMS ArcRepositoryClient. */
    private ViewerArcRepositoryClient client;

    /** Logger. */
    private Log logger = LogFactory.getLog(getClass().getName());

    private LRUCache fileCache;
    
    private Replica replicaUsed;
    
    /**
     *  Constructor.
     *  TODO initiate the caching mechanism or get singleton of Caching class.
     */
    public NetarchiveCacheResourceStore() {
        fileCache = LRUCache.getInstance();
        client = ArcRepositoryClientFactory.getViewerInstance();
        replicaUsed = Replica.getReplicaFromId(Settings.get(
                CommonSettings.USE_REPLICA_ID));
    }

    /**
     * Transforms search result into a resource, according to the ResourceStore
     * interface.
     * @param captureSearchResult the search result.
     * @return a valid resource containing metadata and a link to the ARC
     * or warc-record 
     * @throws ResourceNotAvailableException if something went wrong fetching
     * the record.
     */
    public Resource retrieveResource(CaptureSearchResult captureSearchResult)
            throws ResourceNotAvailableException {
        long offset;
 
        String arcfile = captureSearchResult.getFile();
        offset = captureSearchResult.getOffset();
    
        logger.info("Received request for resource from file '" + arcfile
                + "' at offset '" + offset + "'");
    
        // Try to lookup the file in the cache
        File wantedFile = fileCache.get(arcfile);
        try {
            if (wantedFile != null && wantedFile.exists()) {
                logger.debug("Found the file '" + arcfile + "' in the cache. ");
                return ResourceFactory.getResource(wantedFile, offset);
            } else {
                logger.debug("The file '" + arcfile + "' was not found in the cache. ");
                // Get file from bitarchive, and place it in the cachedir directory
                File fileFromBitarchive = new File(fileCache.getCacheDir(), arcfile);
                client.getFile(arcfile, replicaUsed, fileFromBitarchive);
                // put into the cache
                fileCache.put(arcfile, fileFromBitarchive);
                return ResourceFactory.getResource(fileFromBitarchive, offset);
            }
        } catch (IOException e) {
            logger.error("Error looking for non existing resource", e);
            throw new ResourceNotAvailableException(this.getClass().getName()
                       + "Throws Exception when accessing "
                       + "CaptureResult given from Wayback.");
        }
    }

    /**
     * Shuts down this resource store, closing the arcrepository client.
     * @throws IOException if an exception ocurred while closing the client.
     */
    public void shutdown() throws IOException {
        // Close JMS connection.
        client.close();
    }
}
