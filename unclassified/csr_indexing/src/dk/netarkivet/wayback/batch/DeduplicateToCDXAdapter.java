/*$Id$
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
package dk.netarkivet.wayback.batch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.wayback.UrlCanonicalizer;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Class containing methods for turning duplicate entries in a crawl log into
 * lines in a CDX index file.
 */
public class DeduplicateToCDXAdapter implements
                                     DeduplicateToCDXAdapterInterface {

    /**
     * Logger for this class
     */
    private final Log log = LogFactory.getLog(DeduplicateToCDXAdapter.class);

    /**
     * Define SimpleDateFormat objects for the representation of timestamps
     * in crawl logs and cdx files respectively.
     */
    private static final String crawlDateFormatString = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final String cdxDateFormatString = "yyyyMMddHHmmss";
    private static final SimpleDateFormat crawlDateFormat
            = new SimpleDateFormat(crawlDateFormatString);
    private static final SimpleDateFormat cdxDateFormat
            = new SimpleDateFormat(cdxDateFormatString);

    /**
     * Pattern representing the part of a crawl log entry describing a
     * duplicate record
     */
    private static final String duplicateRecordPatternString
            = "duplicate:\"(.*),(.*)\",(.*)";
    private static final Pattern duplicateRecordPattern
            = Pattern.compile(duplicateRecordPatternString);

    /**
     * canonicalizer used to canonicalize urls
     */
    UrlCanonicalizer canonicalizer;

    /**
     * String for identifying crawl-log entries representing duplicates
     */
    private static final String DUPLICATE_MATCHING_STRING = "duplicate:";

    /**
     * Default constructor. Initializes the canonicalizer.
     */
    public DeduplicateToCDXAdapter() {
        canonicalizer = UrlCanonicalizerFactory.getDefaultUrlCanonicalizer();
    }

    /**
     * If the input line is a crawl log entry representing a duplicate then a
     * CDX entry is written to the output. Otherwise returns null. In the event
     * of an error returns null.
     * @param line the crawl-log line to be analysed
     * @return a CDX line (without newline) or null
     */
    @Override
    public String adaptLine(String line) {
        if (line != null && line.contains(DUPLICATE_MATCHING_STRING)) {
            try {
                String[] crawl_elements = line.split("\\s+");
                StringBuffer result = new StringBuffer();
                String original_url = crawl_elements[3];
                String canonical_url =
                        canonicalizer.urlStringToKey(original_url);
                result.append(canonical_url).append(' ');
                String cdx_date = cdxDateFormat.format(crawlDateFormat.parse(crawl_elements[0]));
                result.append(cdx_date).append(' ').append(original_url).append(' ');
                String mimetype = crawl_elements[6];
                result.append(mimetype).append(' ');
                String http_code = crawl_elements[1];
                result.append(http_code).append(' ');
                String digest = crawl_elements[9].replaceAll("sha1:","");
                result.append(digest).append(" - ");
                String duplicate_record = crawl_elements[11];
                Matcher m = duplicateRecordPattern.matcher(duplicate_record);
                if (m.matches()) {
                    String arcfile = m.group(1);
                    String offset = m.group(2);
                    result.append(offset).append(' ').append(arcfile);
                } else {
                    throw new ArgumentNotValid("crawl record did not match "
                                               + "expected pattern for duplicate"
                                               + " record: '" + duplicate_record
                                               + "'");
                }
                return result.toString();
            } catch (Exception e) {
                log.warn("Could not adapt deduplicate record to CDX line: '"
                         + line + "'", e);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Reads an input stream representing a crawl log line by line and converts
     * any lines representing duplicate entries to wayback-compliant cdx lines.
     * @param is The input stream from which data is read.
     * @param os The output stream to which the cdx lines are written.
     */
    public void adaptStream(InputStream is, OutputStream os) {
        ArgumentNotValid.checkNotNull(is, "is");
        ArgumentNotValid.checkNotNull(os, "os");
        try {
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                String cdx_line = adaptLine(line);
                if (cdx_line != null) {
                    os.write((cdx_line + "\n").getBytes());
                }
            }
        } catch (IOException e) {
            log.warn(e);
        }
    }
}
