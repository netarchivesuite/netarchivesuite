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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import dk.netarkivet.common.utils.InputStreamUtils;

/**
 * This is the connector between netarchivesuite and wayback. And is based on
 * PrototypeNetarchiveResourceStore.java which was made as a prototype
 * connector.
 *
 */
public class NetarchiveResourceStore implements ResourceStore {

    /* JMS ArcRepositoryClient. */
    private ViewerArcRepositoryClient client;

    /* Pattern for matching http version header. */
    private static final Pattern HTTP_HEADER_PATTERN =
        Pattern.compile("^HTTP/1\\.[01] (\\d+) (.*)$");

    /* Logger. */
    private Log logger = LogFactory.getLog(getClass().getName());

    /**
     *  Constuctor.
     */
    public NetarchiveResourceStore() {
        client = ArcRepositoryClientFactory.getViewerInstance();
    }

    /**
     * Transforms search result into a reasource, acording to ResourceStore
     * interface.
     * @param captureSearchResult the search result.
     * @return a valid resource containing metadata and a link to the ARC
     * record.
     * @throws ResourceNotAvailableException if something went wrong fetching
     * record.
     */
    public Resource retrieveResource(CaptureSearchResult captureSearchResult)
            throws ResourceNotAvailableException {
        long offset;
        String responseCode = null;
        Map<String, String> metadata = new HashMap<String, String>();
        ARCRecord arcRecord;
        ArchiveRecordHeader header;

        String arcfile = captureSearchResult.getFile();
        try {
            offset = captureSearchResult.getOffset();
        } catch(NumberFormatException e) {
            logger.error("Error looking for non existing resource", e);
            throw new ResourceNotAvailableException("NetarchiveResourceStore "
                       + "thows NumberFormatException when reading offset.");
        } catch (NullPointerException e) {
            logger.error("Error looking for non existing resource", e);
            throw new ResourceNotAvailableException("NetarchiveResourceStore "
                       + "throws NullPointerException when accessing "
                       + "CaptureResult given from Wayback.");
        }
        logger.info("Received request for resource from file '" + arcfile
                    + "' at offset '" + offset + "'");
        BitarchiveRecord bitarchiveRecord = client.get(arcfile, offset);
        if (bitarchiveRecord == null) {
            throw new ResourceNotAvailableException("NetarchiveResourceStore: "
                    + "Bitarchive didn't return requested record.");
        }
        logger.info("Retrieved resource from file '" + arcfile + "' at offset '"
                    + offset + "'");

        InputStream is = bitarchiveRecord.getData();
        // Match header-lines (until empty line).
        try {
            for (String line = InputStreamUtils.readLine(is);
                        line != null && line.length() > 0;
                        line=InputStreamUtils.readLine(is)) {
                Matcher m = HTTP_HEADER_PATTERN.matcher(line);
                if (m.matches()) {
                    responseCode = m.group(1);
                    logger.debug("Setting response code '" + responseCode + "'");

                } else {
                     String[] parts = line.split(":", 2);
                   if (parts.length != 2) {
                       logger.debug("Malformed header line '" + line + "'");
                   } else {
                       String name = parts[0];
                       String contents = parts[1].trim();
                       if (contents != null) {
                           if (name.equals("Content-Length")) {
                               logger.info("Setting length header to '" + contents + "'");
                               metadata.put(ARCRecordMetaData.LENGTH_FIELD_KEY, contents);
                           } else if (name.equals("Content-Type")) {
                               logger.info("Setting Content-Type header to '" + contents + "'");
                               metadata.put(ARCRecordMetaData.MIMETYPE_FIELD_KEY, contents);
                           } else if (name.equals("Location")) {
                               logger.info("Setting redirect Location header to '" + contents + "'");
                               metadata.put("Location", contents);
                           }
                       }
                   }
                }
            }
        } catch (IOException e) {
            logger.error("Error looking for empty line", e);
            throw new ResourceNotAvailableException(e.getMessage());
        }
        // fill metedata for ARC record.
        metadata.put(ARCRecordMetaData.URL_FIELD_KEY,
                     captureSearchResult.getUrlKey());
        //TODO the following is the correct way to set the URL. If we do
        //things this way then we should be able to get arcrecord to parse
        //the headers for us.
       /* metadata.put(ARCRecordMetaData.URL_FIELD_KEY,
                     captureSearchResult.getOriginalUrl());*/
        try {
            metadata.put(ARCRecordMetaData.IP_HEADER_FIELD_KEY,
                         captureSearchResult.getOriginalHost());
        } catch (NullPointerException ex) {
            metadata.put(ARCRecordMetaData.IP_HEADER_FIELD_KEY, "");
        }
        metadata.put(ARCRecordMetaData.DATE_FIELD_KEY,
                        captureSearchResult.getCaptureDate().toString());
        metadata.put(ARCRecordMetaData.MIMETYPE_FIELD_KEY,
                        captureSearchResult.getMimeType());
        metadata.put(ARCRecordMetaData.VERSION_FIELD_KEY,
                        captureSearchResult.getHttpCode());
        metadata.put(ARCRecordMetaData.ABSOLUTE_OFFSET_KEY, "" + offset);
        metadata.put(ARCRecordMetaData.LENGTH_FIELD_KEY,
                        ""+bitarchiveRecord.getLength());
        if(responseCode != null) {
            metadata.put(ARCRecordMetaData.STATUSCODE_FIELD_KEY, responseCode);
        }

        // create header.
        try {
            header = new ARCRecordMetaData(arcfile, metadata);
        } catch (IOException e) {
            logger.error("Could not create header", e);
            throw new ResourceNotAvailableException(e.getMessage());
        }

        // create ARCRecord.
        try {
            arcRecord = new ARCRecord(is, header, 0, false, false, true);
            int code = arcRecord.getStatusCode();
            logger.debug("ARCRecord created with code '" + code + "'");
            arcRecord.skipHttpHeader();
        } catch(NullPointerException e) {
            logger.error("Could not create ARCRecord", e);
            throw new ResourceNotAvailableException("ARC record doesn't contain"
                                                    +" valid http URL");
        } catch (IOException e) {
            logger.error("Could not create ARCRecord", e);
            throw new ResourceNotAvailableException(e.getMessage());
        }
        final String statusCode = responseCode;
        final Map<String, String> metadataF = metadata;
        //TODO This the sleaziest thing in this class. Why does the
        //ARCRecord give the wrong status code if we don't override this method?
        Resource resource = new ArcResource(arcRecord, null)  {
            public int getStatusCode() {
                return Integer.parseInt(statusCode);
            }
             @Override
                public Map<String, String> getHttpHeaders() {
                    return metadataF;
                }
        };
        logger.info("Returning resouce '" + resource + "'");
        return resource;
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
