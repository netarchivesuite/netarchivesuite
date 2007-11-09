/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
package dk.netarkivet.viewerproxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.arcrepository.ARCLookup;
import dk.netarkivet.common.distribute.arcrepository.ViewerArcRepositoryClient;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * The ARCArchiveAccess class implements reading of ARC indexes and files.
 * It builds on the Java ARC utils and Lucene indexes, and handles using
 * these in an HTTP context.
 */
public class ARCArchiveAccess implements URIResolver {
    //Class constants
    /** Transfer encoding header. */
    private static final String TRANSFER_ENCODING_HTTP_HEADER =
        "Transfer-encoding";

    /** HTTP status code for page not found. */
    private static final int HTTP_NOTFOUND_VALUE = 404;
    /** HTTP header for page not found. */
    private static final String NOTFOUND_HEADER = "HTTP/1.1 404 Not found";
    /** Content-type header used for page not found. */
    private static final String CONTENT_TYPE_STRING = "Content-type: text/html";
    /** Inserted before page not found response. */
    private static final String HTML_HEADER = "<html><head><title>"
            + "Not found</title></head><body>";
    /** Inserted after page not found response. */
    private static final String HTML_FOOTER = "</body></html>";

    /** Matches HTTP header lines like
     * HTTP/1.1 404 Page has gone south
     * Groups:  111 2222222222222222222. */
    private static final Pattern HTTP_HEADER_PATTERN =
        Pattern.compile("^HTTP/1\\.[01] (\\d+) (.*)$");

    /** The underlying ARC record lookup object */
    private ARCLookup lookup;

    /** Logger for this class. */
    private final Log log = LogFactory.getLog(getClass().getName());

    /** The name of the Lucene index dir under the settings-given viewerproxy
     * dir.
     */
    private static final String LUCENE_INDEX_DIR_NAME = "luceneIndexDir";

    /** Initialise new ARCArchiveAcces with no index file.
     *
     * @param arcRepositoryClient The arcRepositoryClient to use when retrieving
     * @throws ArgumentNotValid if arcRepositoryClient is null.
     */
    public ARCArchiveAccess(ViewerArcRepositoryClient arcRepositoryClient) {
        ArgumentNotValid.checkNotNull(
                arcRepositoryClient, "ArcRepositoryClient arcRepositoryClient");
        File luceneIndexDir =
                new File(new File(Settings.get(Settings.VIEWERPROXY_DIR)),
                        LUCENE_INDEX_DIR_NAME);
        lookup = new ARCLookup(arcRepositoryClient, luceneIndexDir);
    }

    /**
     * This method resets the Lucene index this object works on, and replaces
     * it with the given index.
     *
     * @param index The new index file, a directory containing gzipped files.
     * @throws ArgumentNotValid If argument is null
     * @throws IOFailure if the file cannot be read
     */
    public void setIndex(File index) {
        lookup.setIndex(index);
    }

    /** Look up a given URI and add its contents to the Response given.
     * @param request The request to look up record for
     * @return The response code for this page if found, or
     * URIResolver.NOT_FOUND otherwise.
     * @see URIResolver#lookup(Request, Response)
     * @throws IOFailure on trouble looking up the request (timeout, i/o, etc.)
     */
    public int lookup(Request request, Response response) {
        ArgumentNotValid.checkNotNull(request, "Request request");
        ArgumentNotValid.checkNotNull(response, "Response response");
        URI uri = request.getURI();
        InputStream content = null;
        try {
            content = lookup.lookup(uri);
            if (content == null) {
                // If the object wasn't found, return an appropriate message.
                log.debug("Missing URL: '" + uri + "'");
                createNotFoundResponse(uri, response);
                return URIResolver.NOT_FOUND;
            }
            // First write the original header.
            writeHeader(content, response);
            // Now flush the content to the browser.
            readPage(content, response.getOutputStream());
        } finally {
            if (content != null) {
                try {
                    content.close();
                } catch (IOException e) {
                    log.debug(
                                  "Error writing response to browser "
                                  + "for '" + uri + "'. Giving up!", e);
                }
            }
        }
        return response.getStatus();
    }

    /** Generate an appropriate response when a URI is not found.
     * If this fails, it is logged, but otherwise ignored.
     *
     * @param uri The URI attempted read that could not be found
     * @param response The Response object to write the error response into.
     */
    protected void createNotFoundResponse(URI uri, Response response) {
        try {
            // first write a header telling the browser to expect text/html
            response.setStatus(HTTP_NOTFOUND_VALUE);
            writeHeader(new ByteArrayInputStream(
                    (NOTFOUND_HEADER + '\n' + CONTENT_TYPE_STRING).getBytes()),
                        response);
            // Now flush an errorscreen to the browser
            OutputStream browserOut = response.getOutputStream();
            browserOut.write((HTML_HEADER + "Can't find URL: " + uri
                              + HTML_FOOTER).getBytes());
            browserOut.flush();
        } catch (IOFailure e) {
            log.debug(
                          "Error writing error response to browser "
                          + "for '" + uri + "'. Giving up!", e);
        } catch (IOException e) {
            log.debug(
                          "Error writing error response to browser "
                          + "for '" + uri + "'. Giving up!", e);
        }
        //Do not close stream! That is left to the servlet.
    }

    /** Apply filters to HTTP headers.
     * Can be overridden in subclasses.
     * Currently only removes Transfer-encoding headers.
     *
     * @param headername The name of the header field, e.g. Content-Type
     * Remember that this is not case sensitive
     * @param headercontents The contents of the header field, e.g. text/html
     * @return A (possibly modified) header contents string, or null if the
     * header should be skipped.
     */
    protected String filterHeader(String headername, String headercontents) {
        // Cannot get chunked output to work, so we must remove
        // any chunked encoding lines
        if (headername.equalsIgnoreCase(TRANSFER_ENCODING_HTTP_HEADER)) {
            return null;
        }
        return headercontents;
    }

    /** Write HTTP header, including status and status reason.
     *
     * @param is A stream to read the header from.
     * @param response A Response to write the header, status and reason to.
     * @throws IOFailure If the underlying reads or writes fail.
     */
    private void writeHeader(InputStream is, Response response) {
        // Reads until the end of the header (indicated by an empty line)
        try {
            for (String line = readLine(is);
                 (line != null) && (line.length() > 0);
                 line = readLine(is)) {
                //  Try to match lines like "HTTP/1.0 200 OK"
                Matcher m = HTTP_HEADER_PATTERN.matcher(line);
                if (m.matches()) {
                    String responsecode = m.group(1);
                    String responsetext = m.group(2);
                    //Note: Always parsable int, due to the regexp, so no reason
                    //to check for parse errors
                    response.setStatus(Integer.parseInt(responsecode),
                                       responsetext);
                } else {
                    // try to match header-lines containing colon,
                    // like "Content-Type: text/html"
                    String[] parts = line.split(":", 2);
                    if (parts.length != 2) {
                        log.debug("Malformed header line '" + line + "'");
                    } else {
                        String name = parts[0];
                        String contents = filterHeader(name,
                                                       parts[1].trim());
                        if (contents != null) {
                            //filter out unwanted headers
                            response.addHeaderField(name, contents);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new IOFailure("Trouble reading from input stream or writing"
                                + " to output stream", e);
        }
    }

    /** Read an entire page body into some stream.
     *
     * @param content The stream to read the page from. Not closed afterwards.
     * @param out The stream to write the results to. Not closed afterwards.
     * @throws IOFailure If the underlying reads or writes fail
     */
    private void readPage(InputStream content, OutputStream out) {
        BufferedInputStream page = new BufferedInputStream(content);
        BufferedOutputStream responseOut = new BufferedOutputStream(out);
        try {
            byte[] buffer = new byte[Constants.IO_BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = page.read(buffer)) != -1) {
                responseOut.write(buffer, 0, bytesRead);
            }
            responseOut.flush();
        } catch (IOException e) {
             throw new IOFailure("Could not read or write data", e);
        }
    }

    /** Read a line of bytes from an InputStream.  Useful when an InputStream
     * may contain both text and binary data.
     * @param inputStream A source of data
     * @return A line of text read from inputStream, with terminating
     * \r\n or \n removed, or null if no data is available.
     * @throws IOException on trouble reading from input stream
     */
    private String readLine(InputStream inputStream) throws IOException {
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
    private static byte[] readRawLine(InputStream inputStream)
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
