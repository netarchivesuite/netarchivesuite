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

import dk.netarkivet.archive.arcrepository.distribute.JMSArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.utils.InputStreamUtils;

public class NetarchiveResourceStore implements ResourceStore {

    // JMS Arch Repository Client.
    private ArcRepositoryClient client;

    // http header pattern.
    private static final Pattern HTTP_HEADER_PATTERN =
        Pattern.compile("^HTTP/1\\.[01] (\\d+) (.*)$");

    // logger.
    private Log logger = LogFactory.getLog(getClass().getName());

    /**
     *  Constuctor.
     */
    public NetarchiveResourceStore() {
        client = JMSArcRepositoryClient.getInstance();
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
    public Resource retrieveResource(CaptureSearchResult captureSearchResult) throws ResourceNotAvailableException {
        long offset;
        String responseCode = null;
        Map<String, Object> metadata = new HashMap<String, Object>();
        ARCRecord arcRecord;
        ArchiveRecordHeader header;

        String arcfile = captureSearchResult.getFile();
        try {
            offset = captureSearchResult.getOffset();
        } catch(NumberFormatException e) {
            logger.error("Error looking for non existing resource", e);
            throw new ResourceNotAvailableException("NumberFormatException");
        } catch (NullPointerException e) {
            logger.error("Error looking for non existing resource", e);
            throw new ResourceNotAvailableException("NullPointerException");
        }
        logger.info("Received request for resource from file '" + arcfile
                    + "' at offset '" + offset + "'");
        BitarchiveRecord bitarchiveRecord = client.get(arcfile, offset);
        if (bitarchiveRecord == null) {
            throw new ResourceNotAvailableException("Resource not in archive");
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

                }
            }
        } catch (IOException e) {
            logger.error("Error looking for empty line", e);
            throw new ResourceNotAvailableException(e.getMessage());
        }
        // fill metedata for ARC record.
        metadata.put(ARCRecordMetaData.URL_FIELD_KEY,
                     captureSearchResult.getUrlKey());
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
        metadata.put(ARCRecordMetaData.VERSION_FIELD_KEY, "HTTP/1.1");
        metadata.put(ARCRecordMetaData.ABSOLUTE_OFFSET_KEY, offset);
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
            arcRecord = new ARCRecord(is,header, 0,false,false,true);
            int code = arcRecord.getStatusCode();
            logger.debug("ARCRecord created with code '" + code + "'");
        } catch(NullPointerException e) {
            logger.error("Could not create ARCRecord", e);
            throw new ResourceNotAvailableException("ARC record doesn't contain"
                                                    +" valid http URL");
        } catch (IOException e) {
            logger.error("Could not create ARCRecord", e);
            throw new ResourceNotAvailableException(e.getMessage());
        }
        final String statusCode = responseCode;

        //TODO This the sleaziest thing in this prototype. Why does the
        //ARCRecord give the wrong status code if we don't override this method?
        Resource resource = new ArcResource(arcRecord, null)  {
            public int getStatusCode() {
                return Integer.parseInt(statusCode);
            }
        };
        logger.info("Returning resouce '" + resource + "'");
        return resource;
    }

    public void shutdown() throws IOException {
        // TODO: should this method do something?
    }
}
