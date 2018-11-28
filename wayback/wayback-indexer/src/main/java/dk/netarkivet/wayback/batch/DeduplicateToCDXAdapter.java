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
package dk.netarkivet.wayback.batch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.time.FastDateFormat;
import org.archive.wayback.UrlCanonicalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Class containing methods for turning duplicate entries in a crawl log into lines in a CDX index file.
 */
public class DeduplicateToCDXAdapter implements DeduplicateToCDXAdapterInterface {

    /** Logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(DeduplicateToCDXAdapter.class);

    /** Define SimpleDateFormat objects for the representation of timestamps in crawl logs and cdx files respectively. */
    private static final String crawlDateFormatString = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final String cdxDateFormatString = "yyyyMMddHHmmss";
    private static final FastDateFormat crawlDateFormat = FastDateFormat.getInstance(crawlDateFormatString);
    private static final FastDateFormat cdxDateFormat = FastDateFormat.getInstance(cdxDateFormatString);

    /** Pattern representing the part of a crawl log entry describing a duplicate record. */
    private static final String duplicateRecordPatternString = "duplicate:\"([^,]*),([^,]*)\",(.*)";	//e.g. duplicate:"arcfile,offset"
    // The extended format is made to preserve the date of the record pointed to by arcfile,offset argument
    private static final String extendedDuplicateRecordPatternString = "duplicate:\"([^,]*),([^,]*),([^,]*)\",(.*)"; //e.g. duplicate:"arcfile,offset,timestamp"
    
    private static final Pattern duplicateRecordPattern = Pattern.compile(duplicateRecordPatternString);
    private static final Pattern extendedDuplicateRecordPattern = Pattern.compile(extendedDuplicateRecordPatternString);

    /** canonicalizer used to canonicalize urls. */
    UrlCanonicalizer canonicalizer;

    /** String for identifying crawl-log entries representing duplicates. */
    private static final String DUPLICATE_MATCHING_STRING = "duplicate:";

    /**
     * Default constructor. Initializes the canonicalizer.
     */
    public DeduplicateToCDXAdapter() {
        canonicalizer = UrlCanonicalizerFactory.getDefaultUrlCanonicalizer();
    }

    /**
     * If the input line is a crawl log entry representing a duplicate then a CDX entry is written to the output.
     * Otherwise returns null. In the event of an error returns null.
     *
     * @param line the crawl-log line to be analysed
     * @return a CDX line (without newline) or null
     */
    @Override
    public String adaptLine(String line) {
        if (line != null && line.contains(DUPLICATE_MATCHING_STRING)) {
            try {
                String[] crawlElements = line.split("\\s+");
                StringBuffer result = new StringBuffer();
                String originalUrl = crawlElements[3];
                String canonicalUrl = canonicalizer.urlStringToKey(originalUrl);
                result.append(canonicalUrl).append(' ');
                String cdxDate = cdxDateFormat.format(crawlDateFormat.parse(crawlElements[0]));
                result.append(cdxDate).append(' ').append(originalUrl).append(' ');
                String mimetype = crawlElements[6];
                result.append(mimetype).append(' ');
                String httpCode = crawlElements[1];
                result.append(httpCode).append(' ');
                String digest = crawlElements[9].replaceAll("sha1:", "");
                result.append(digest).append(" - ");
                String duplicateRecord = crawlElements[11];
                if (!duplicateRecord.startsWith(DUPLICATE_MATCHING_STRING)) {
                    // Probably an Exception starting with "le:" is injected before the
                    // DUPLICATE_MATCHING_STRING, Try splitting on duplicate:
                    String[] parts = duplicateRecord.split(DUPLICATE_MATCHING_STRING);
                    if (parts.length == 2) {
                        String newDuplicateRecord = DUPLICATE_MATCHING_STRING + parts[1];
                        log.debug("Duplicate-record changed from '{}' to '{}'", duplicateRecord, newDuplicateRecord);
                        duplicateRecord = newDuplicateRecord;
                    }
                }
                Matcher m = duplicateRecordPattern.matcher(duplicateRecord);
                Matcher m1 = extendedDuplicateRecordPattern.matcher(duplicateRecord);
                if (m.matches()) {
                    String arcfile = m.group(1);
                    String offset = m.group(2);
                    result.append(offset).append(' ').append(arcfile);
                } else if (m1.matches()) {
                	String arcfile = m1.group(1);
                	String offset = m1.group(2);
                	result.append(offset).append(' ').append(arcfile);
                } else {
                    throw new ArgumentNotValid("crawl record did not match " + "expected pattern for duplicate"
                            + " record: '" + duplicateRecord + "'");
                }
                return result.toString();
            } catch (Exception e) {
                log.error("Could not adapt deduplicate record to CDX line: '{}'", line, e);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Reads an input stream representing a crawl log line by line and converts any lines representing duplicate entries
     * to wayback-compliant cdx lines.
     *
     * @param is The input stream from which data is read.
     * @param os The output stream to which the cdx lines are written.
     */
    public void adaptStream(InputStream is, OutputStream os) {
        ArgumentNotValid.checkNotNull(is, "is");
        ArgumentNotValid.checkNotNull(os, "os");
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                String cdxLine = adaptLine(line);
                if (cdxLine != null) {
                    os.write((cdxLine + "\n").getBytes());
                }
            }
        } catch (IOException e) {
            log.error("Exception reading crawl log;", e);
        }
    }
}
