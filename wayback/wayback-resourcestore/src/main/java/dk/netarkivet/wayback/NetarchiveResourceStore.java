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
package dk.netarkivet.wayback;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.format.ArchiveFileConstants;
import org.archive.io.ArchiveRecordHeader;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;
import org.archive.wayback.ResourceStore;
import org.archive.wayback.core.CaptureSearchResult;
import org.archive.wayback.core.Resource;
import org.archive.wayback.exception.ResourceNotAvailableException;
import org.archive.wayback.resourcestore.resourcefile.ArcResource;

import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.distribute.arcrepository.ViewerArcRepositoryClient;

/**
 * This is the connector between netarchivesuite and wayback. And is based on PrototypeNetarchiveResourceStore.java
 * which was made as a prototype connector.
 */
public class NetarchiveResourceStore implements ResourceStore {

    /** JMS ArcRepositoryClient. */
    protected ViewerArcRepositoryClient client;
    
    /** Logger. */
    private Log logger = LogFactory.getLog(getClass().getName());

    /**
     * Constructor.
     */
    public NetarchiveResourceStore() {
        client = ArcRepositoryClientFactory.getViewerInstance();
    }

    /**
     * Transforms search result into a resource, according to the ResourceStore interface.
     *
     * @param captureSearchResult the search result.
     * @return a valid resource containing metadata and a link to the ARC record.
     * @throws ResourceNotAvailableException if something went wrong fetching record.
     */
    public Resource retrieveResource(CaptureSearchResult captureSearchResult) throws ResourceNotAvailableException {
        long offset;
        Map<String, Object> metadata = new HashMap<String, Object>();
        ARCRecord arcRecord;
        ArchiveRecordHeader arcRecordMetaData;

        String filename = captureSearchResult.getFile();
        try {
            offset = captureSearchResult.getOffset();
        } catch (NumberFormatException e) {
            logger.error("Error looking for non existing resource", e);
            throw new ResourceNotAvailableException("NetarchiveResourceStore "
                    + "thows NumberFormatException when reading offset.");
        } catch (NullPointerException e) {
            logger.error("Error looking for non existing resource", e);
            throw new ResourceNotAvailableException("NetarchiveResourceStore "
                    + "throws NullPointerException when accessing " + "CaptureResult given from Wayback.");
        }
        logger.info("Received request for resource from file '" + filename + "' at offset '" + offset + "'");
        BitarchiveRecord bitarchiveRecord = client.get(filename, offset);
        if (bitarchiveRecord == null) {
            throw new ResourceNotAvailableException("NetarchiveResourceStore: "
                    + "Bitarchive didn't return the requested record.");
        }
        logger.info("Retrieved resource from file '" + filename + "' at offset '" + offset + "'");

        // This InputStream is just the http-response, starting with the HTTP arcRecordMetaData.
        InputStream is = bitarchiveRecord.getData();

        metadata.put(ARCRecordMetaData.URL_FIELD_KEY, captureSearchResult.getOriginalUrl());
        try {
            metadata.put(ARCRecordMetaData.IP_HEADER_FIELD_KEY, captureSearchResult.getOriginalHost());
        } catch (NullPointerException ex) {
            metadata.put(ARCRecordMetaData.IP_HEADER_FIELD_KEY, "");
        }
        metadata.put(ARCRecordMetaData.DATE_FIELD_KEY, captureSearchResult.getCaptureDate().toString());
        metadata.put(ARCRecordMetaData.MIMETYPE_FIELD_KEY, captureSearchResult.getMimeType());
        metadata.put(ARCRecordMetaData.VERSION_FIELD_KEY, captureSearchResult.getHttpCode());
        metadata.put(ARCRecordMetaData.ABSOLUTE_OFFSET_KEY, "" + offset);
        metadata.put(ARCRecordMetaData.LENGTH_FIELD_KEY, "" + bitarchiveRecord.getLength());
        metadata.put(ARCRecordMetaData.STATUSCODE_FIELD_KEY, captureSearchResult.getHttpCode());
        metadata.put(ArchiveFileConstants.ORIGIN_FIELD_KEY, captureSearchResult.getOriginalUrl());
        // create arcRecordMetaData.
        try {
            arcRecordMetaData = new ARCRecordMetaData(filename, metadata);
        } catch (IOException e) {
            logger.error("Could not create arcRecordMetaData", e);
            throw new ResourceNotAvailableException(e.getMessage());
        }

        // create ARCRecord.
        try {
            arcRecord = new ARCRecord(is, arcRecordMetaData, 0, false, false, true);
            //arcRecord.getHttpHeaders();
            //arcRecord.skipHttpHeader();
            logger.debug("ARCRecord created with code '" + arcRecord.getStatusCode() + "'");
            logger.debug("Headers: " + arcRecord.getHeaderString());
        } catch (NullPointerException e) {
            logger.error("Could not create ARCRecord", e);
            throw new ResourceNotAvailableException("ARC record doesn't contain" + " valid http URL");
        } catch (IOException e) {
            logger.error("Could not create ARCRecord", e);
            throw new ResourceNotAvailableException(e.getMessage());
        }
        Resource resource = new ArcResource(arcRecord, null);
        try {
            //This call has the side-effect of queueing up the resource at the start of the response-body, after the http headers.
            resource.parseHeaders();
        } catch (IOException e) {
            logger.debug(e);
        }
        logger.info("Returning resource '" + resource + "'");
        return resource;
    }

    /**
     * Shuts down this resource store, closing the arcrepository client.
     *
     * @throws IOException if an exception occurred while closing the client.
     */
    public void shutdown() throws IOException {
        // Close JMS connection.
        client.close();
    }
}
