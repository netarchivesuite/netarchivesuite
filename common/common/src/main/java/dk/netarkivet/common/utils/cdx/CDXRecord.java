/* File:        $Id$
 * Date:        $Date$
 * Revision:    $Revision$
 * Author:      $Author$
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
package dk.netarkivet.common.utils.cdx;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.StringUtils;


/**
 * Represents a line i a CDX-file. A CDX-file is an index over arcfiles, with
 * fields for uri, ip, date, mimetype, length, arcfile, and offset in the file.
 */
public class CDXRecord {
    /** The logger for this class. */
    private static Log log = LogFactory.getLog(CDXRecord.class.getName());
    /** The uri information in a CDX entry. */
    private String url;
    /** The ip information in a CDX entry. */
    private String ip;
    /** The date information in a CDX entry. */
    private String date;
    /** The mimetype information in a CDX entry. */
    private String mimetype;
    /** The length information in a CDX entry. */
    private long length;
    /** The arcfile information in a CDX entry. */
    private String arcfile;
    /** The offset information in a CDX entry. */
    private long offset;

    /**
     * Helper method to avoid exception in URL decoding.
     * @param s The string to unescape.
     * @return the unescaped string.
     */
    private static String unescape(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new ArgumentNotValid(
                    "UTF-8 is an unknown encoding. This should never happen!");
        }
    }

    /**
     * Compare two URLs for equality; first URL-unescaping (in UTF-8) all
     * arguments in any query part.
     *
     * @param url1 The first URL
     * @param url2 The second URL
     * @return A boolean indicating whether the URLs are equal
     */
    public static boolean URLsEqual(String url1, String url2) {
        ArgumentNotValid.checkNotNull(url1, "String uri1");
        ArgumentNotValid.checkNotNull(url2, "String uri2");
        boolean result = url1.equals(url2);
        if (!result && url1.contains("?") && url2.contains("?")) {
            // split at ? and compare prefix
            String pre1 = url1.substring(0, url1.indexOf('?') + 1);
            String post1 = url1.substring(url1.indexOf('?') + 1);
            String pre2 = url2.substring(0, url2.indexOf('?') + 1);
            String post2 = url2.substring(url2.indexOf('?') + 1);
            if (pre1.equals(pre2)) {
                String postdecode1 = unescape(post1);
                String postdecode2 = unescape(post2);
                result = (post1.equals(post2) || postdecode1
                        .equals(postdecode2));
            }
        }
        return result;
    }

    /**
     * Constructor for class CDXRecord.
     *
     * @param fields the given fields of a line i CDX-format.
     * @throws ArgumentNotValid if argument is null or number of fields is less
     *                          than 7 or if length or offset does not contain
     *                          long values.
     */
    public CDXRecord(String[] fields) {
        ArgumentNotValid.checkNotNull(fields, "String[] fields");
        if (fields.length >= 7) {
            try {
                this.url = fields[0];
                this.ip = fields[1];
                this.date = fields[2];
                this.mimetype = fields[3];
                this.length = Long.parseLong(fields[4]);
                this.arcfile = fields[5];
                this.offset = Long.parseLong(fields[6]);
            } catch (NumberFormatException e) {
                String message = "Could not make CDXRecord - out of fields "
                                 + StringUtils.conjoin(",", fields)
                                 + ". Length or offset was not a parsable"
                                 + " long value.";
                log.debug(message);
                throw new ArgumentNotValid(message);
            }
        } else {
            String message = "Could not make CDXRecord - out of "
                             + fields.length + " fields: "
                             + StringUtils.conjoin(",", fields);
            log.debug(message);
            throw new ArgumentNotValid(message);
        }
    }
    
    /**
     * Constructor, which tries to parse the given string as a CDXRecord.
     * @param line a CDXline
     */
    public CDXRecord(String line) {
        this(line.split(CDXReader.SEPARATOR_REGEX));
    }

    /**
     * Get the given URL.
     * @return the URL
     */
    public String getURL() {
        return url;
    }

    /**
     * Get the given IP.
     * @return the IP
     */
    public String getIP() {
        return ip;
    }

    /**
     * Get the given date.
     * @return the date
     */
    public String getDate() {
        return date;
    }

    /**
     * Get the given mimetype.
     * @return The given mimetype
     */
    public String getMimetype() {
        return mimetype;
    }

    /**
     * Get the given length.
     * @return The given length
     */
    public long getLength() {
        return length;
    }

    /**
     * Get the given arcfile.
     * @return The given arcfile
     */
    public String getArcfile() {
        return arcfile;
    }

    /**
     * Get the given offset.
     * @return The given offset
     */
    public long getOffset() {
        return offset;
    }
}
