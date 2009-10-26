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

package dk.netarkivet.wayback;

import java.io.ByteArrayOutputStream;
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

/**
 * This is the prototype connector between netarchivesuite and wayback. It is not
 * QA'd and exists only for reference. Once it has been replaced with some decent
 * code it can be deleted.
 *
 * //TODO replace this class with one that satisfies our QA controls
 *
 * @author csr
 * @since Dec 19, 2008
 */


/**
 * @deprecated from version 3.9.2.
 */
public class PrototypeNetarchiveResourceStore implements ResourceStore {
   /** Matches HTTP header lines like
     * HTTP/1.1 404 Page has gone south
     * Groups:  111 2222222222222222222. */
    private static final Pattern HTTP_HEADER_PATTERN =
        Pattern.compile("^HTTP/1\\.[01] (\\d+) (.*)$");

    private Log logger = LogFactory.getLog(getClass().getName());

    private ArcRepositoryClient client;

    public PrototypeNetarchiveResourceStore() {
        client = JMSArcRepositoryClient.getInstance();
    }

    public Resource retrieveResource(CaptureSearchResult captureSearchResult) throws ResourceNotAvailableException {

        String capture_result_string = "Retrieving \n"+
        "Date = '" + captureSearchResult.getCaptureDate() + "'\n" +
        "Timstamp = '" + captureSearchResult.getCaptureTimestamp() + "'\n" +
        "File = '" + captureSearchResult.getFile() + "'\n" +
        "Http code = '" + captureSearchResult.getHttpCode() + "'\n" +
        "Mime Type = '" + captureSearchResult.getMimeType() + "'\n" +
        "Offset ='" + captureSearchResult.getOffset() + "'\n" +
        "Original host = '" + captureSearchResult.getOriginalHost() + "'\n" +
        "Original url = '" + captureSearchResult.getOriginalUrl() + "'\n" +
        "Redirect url = '" + captureSearchResult.getRedirectUrl() + "'\n" +
        "url key = '" + captureSearchResult.getUrlKey();
        logger.info(capture_result_string);

        String arcfile = captureSearchResult.getFile();
        long offset = captureSearchResult.getOffset();
        Map metadata = new HashMap();
        //final String statuscode = captureSearchResult.getHttpCode();
        //logger.info("Retrieving result with status code '" + statuscode + "'");

        //metadata.put(ARCRecordMetaData.)
        /*List<String> required_fields = ARCRecordMetaData.REQUIRED_VERSION_1_HEADER_FIELDS;
        for (String field:required_fields) {
            logger.info("Required field: '" + field + "'");
        }*/
				logger.info(client.toString());
        BitarchiveRecord bitarchive_record = client.get(arcfile, offset);
        if (bitarchive_record == null) {
            //log here because we don't trust wayback not to swallow our log messages
            logger.info("Resource not in archive");
            throw new ResourceNotAvailableException("Resource not in archive");
        }
        //metadata.put(ARCRecordMetaData.STATUSCODE_FIELD_KEY, statuscode);
        metadata.put(ARCRecordMetaData.URL_FIELD_KEY, captureSearchResult.getUrlKey());
        metadata.put(ARCRecordMetaData.IP_HEADER_FIELD_KEY, captureSearchResult.getOriginalHost());
        metadata.put(ARCRecordMetaData.DATE_FIELD_KEY, captureSearchResult.getCaptureDate().toString());
        metadata.put(ARCRecordMetaData.MIMETYPE_FIELD_KEY, captureSearchResult.getMimeType());
        metadata.put(ARCRecordMetaData.VERSION_FIELD_KEY, "HTTP/1.1");
        metadata.put(ARCRecordMetaData.ABSOLUTE_OFFSET_KEY, "0");
        metadata.put(ARCRecordMetaData.LENGTH_FIELD_KEY, ""+bitarchive_record.getLength());
        logger.info("Retrieved resource from file '" + arcfile + "' at offset '" + offset + "'");
        InputStream is = bitarchive_record.getData();
        ARCRecord arc_record;
        String responsecode = null;
        try {
            for (String line = readLine(is); line != null && line.length()>0; line=readLine(is)  ) {
                logger.info("Header line: '" + line + "'");
                Matcher m = HTTP_HEADER_PATTERN.matcher(line);
                if (m.matches()) {
                    responsecode = m.group(1);
                    String responsetext = m.group(2);
                    logger.info("Setting response code '" + responsecode + "'");
                    metadata.put(ARCRecordMetaData.STATUSCODE_FIELD_KEY, responsecode);
                } {
                // try to match header-lines containing colon,
                // like "Content-Type: text/html"
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
                        }
                    }
                }
            }

            }
        } catch (IOException e) {
            logger.info("Error looking for empty line", e);
            throw new ResourceNotAvailableException(e.getMessage());
        }
        final String statuscode = responsecode;
        ArchiveRecordHeader header;
        try {
            header = new ARCRecordMetaData(arcfile, metadata);
        } catch (IOException e) {
            logger.info("Could not create header", e);
            throw new ResourceNotAvailableException(e.getMessage());
        }
        //TODO fix the logic of the following. If the response code is 302 and the
        //redirect url is identical to this url, then change the response code to 404
        //print the warning
       /* if (responsecode.equals("302")) {
            logger.info("Reseting redirect to 404");
            responsecode = "404";
            is = new ByteArrayInputStream("This record was redirected. Please try a later harvest result".getBytes()) ;
        }*/
        try {
            arc_record = new ARCRecord(is,header,0,false,false,true);
            int code = arc_record.getStatusCode();
            logger.info("ARCRecord created with code '" + code + "'");
            arc_record.skipHttpHeader();
            logger.info("ARCRecord now has code '" + arc_record.getStatusCode() + "'");
        } catch (IOException e) {
            logger.info("Could not create ARCRecord", e);
             throw new ResourceNotAvailableException(e.getMessage());
        }

        //TODO This the sleaziest thing in this prototype. Why does the ARCRecord give the wrong status code if we don't override this method?
        Resource resource = new ArcResource(arc_record, null) {
            public int getStatusCode() {
                return Integer.parseInt(statuscode);
            }
        };
        //ArcResource resource = new ArcResource(arc_record, null);
        logger.info("Returning resouce '" + resource + "'");
        return resource;
    }

    public void shutdown() throws IOException {

    }


    //TODO the following lines are a cut-and-paste from somewhere else in the
    //code. Replace these with public static utility methods in the util 
    //package (with unit tests, code review etc.)


    /** Read a line of bytes from an InputStream.  Useful when an InputStream
     * may contain both text and binary data.
     * @param inputStream A source of data
     * @return A line of text read from inputStream, with terminating
     * \r\n or \n removed, or null if no data is available.
     * @throws IOException on trouble reading from input stream
     */
    private static String readLine(InputStream inputStream) throws IOException {
        byte[] rawdata = readRawLine(inputStream);
        if (rawdata == null) {
            return null;
        }
        int len = rawdata.length;
        if (len > 0) {
            if (rawdata[len - 1] == '\n') {
                len--;
                if (len > 0) {
                    if (rawdata[len - 1] == '\r') {
                        len--;
                    }
                }
            }
        }
        return new String(rawdata, 0, len);
    }

     /** Reads a raw line from an InputStream, up till \n.
     * Since HTTP allows \r\n and \n as terminators, this gets the whole line.
     * This code is adapted from org.apache.commons.httpclient.HttpParser
     *
     * @param inputStream A stream to read from.
     * @return Array of bytes read or null if none are available.
     * @throws IOException if the underlying reads fail
     */
    private  static byte[] readRawLine(InputStream inputStream)
        throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int ch;
        while ((ch = inputStream.read()) >= 0) {
            buf.write(ch);
            if (ch == '\n') {
                break;
            }
        }
        if (buf.size() == 0) {
            return null;
        }
        return buf.toByteArray();
    }
}
