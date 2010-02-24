/* File:    $Id$
 * Revision:$Revision$
 * Author:  $Author$
 * Date:    $Date$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */

package dk.netarkivet.archive.indexserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import is.hi.bok.deduplicator.CrawlDataItem;
import is.hi.bok.deduplicator.CrawlLogIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.cdx.CDXRecord;

/** This subclass of CrawlLogIterator adds the layer of digging an origin of
 * the form "arcfile,offset" out of a corresponding CDX index.  This may
 * cause some of the entries in the crawl log to be skipped.  The two files
 * are read in parallel.
 */
public class CDXOriginCrawlLogIterator extends CrawlLogIterator {
    /** The reader of the (sorted) CDX index. */
    protected BufferedReader reader;
    /** The last record we read from the reader.  We may overshoot on the
     * CDX reading if there are entries not in CDX, so we hang onto this
     * until the reading of the crawl.log catches up. */
    protected CDXRecord lastRecord;
    /** The log. */
    private Log log = LogFactory.getLog(
            CDXOriginCrawlLogIterator.class.getName());
    /** The constant prefixed checksums in newer versions of Heritrix 
     * indicating the digest method.  The deduplicator currently doesn't use 
     * the equivalent prefix, so we need to strip it off (see bug #1004).
     */
    private static final String SHA1_PREFIX = "sha1:";

    /** Create a new CDXOriginCrawlLogIterator from crawl.log and CDX sources.
     *
     * @param source File containing a crawl.log sorted by URL
     * (LANG=C sort -k 4b)
     * @param cdx A reader of a sorted CDX file.  This is given as a reader
     * so that it may be closed after use (CrawlLogIterator provides no close())
     * @throws IOException If the underlying CrawlLogIterator fails, e.g.
     * due to missing files.
     */
    public CDXOriginCrawlLogIterator(File source, BufferedReader cdx)
            throws IOException {
        super(source.getAbsolutePath());
        ArgumentNotValid.checkNotNull(cdx, "BufferedReader cdx");
        reader = cdx;
    }

    /** Parse a crawl.log line into a valid CrawlDataItem.
     *
     * If CrawlLogIterator is ok with this line, we must make sure that it
     * has an origin by finding missing ones in the CDX file.
     * If multiple origins are found in the CDX files, the one that was
     * harvested last is chosen.
     * If no origin can be found, the item is rejected.
     *
     * We assume that super.parseLine() delivers us the items in the crawl.log
     * in the given (sorted) order with non-null URLs, though we admit that
     * some undeclared exceptions can be thrown by it.
     *
     * @param line A crawl.log line to parse.
     * @return A CrawlDataItem with a valid origin field, or null if we could
     * not determine an appropriate origin.
     * @throws IOFailure if there is an error reading the files.
     */
    protected CrawlDataItem parseLine(String line) throws IOFailure {
        CrawlDataItem item;
        log.trace("Processing crawl-log line: " + line);
        try {
            item = super.parseLine(line);
        } catch (RuntimeException e) {
            log.debug("Skipping over bad crawl-log line '" + line + "'", e);
            return null;
        }

        // Hack that works around bug #1004: sha1: prefix not accounted for
        if (item != null && item.getContentDigest() != null 
                && item.getContentDigest().toLowerCase().startsWith(
                        SHA1_PREFIX)) {
            item.setContentDigest(item.getContentDigest().substring(
                    SHA1_PREFIX.length()));
        }

        //If a origin was found in the crawl log, we accept that as correct.
        //Otherwise we must find the origin in the CDX file.
        if (item != null && item.getOrigin() == null) {
            // Iterate through the sorted CDX file until lastRecord is not null
            // and lastRecord.getURL() is lexicographically higher than
            // item.getURL(), indicating that there are no more matches.
            CDXRecord foundRecord = null;
            while (lastRecord == null
                    || lastRecord.getURL().compareTo(item.getURL()) <= 0) {
                // If the cdx URL is the one we are looking for, we have a
                // potential origin.
                if (lastRecord != null
                    && lastRecord.getURL().equals(item.getURL())) {
                    // If this is our first potential origin, or if it is better
                    // than the one we currently consider best, we remember this
                    // entry. A better origin is defined as one with a later
                    // date than the current choice.
                    if (foundRecord == null
                        || lastRecord.getDate().compareTo(
                                foundRecord.getDate()) > 0) {
                        foundRecord = lastRecord;
                      log.trace("Foundrecord set to '"
                                + foundRecord.getArcfile() + ","
                                + foundRecord.getOffset() + "'");  
                    }
                }

                //Read the next line
                try {
                    String record = reader.readLine();
                    if (record == null) {
                        break; // EOF, nothing to do
                    }
                    if  (record.length() == 0) {
                        continue; // skip empty lines
                    }
                    try {
                        lastRecord = new CDXRecord(record);
                    } catch (ArgumentNotValid e) {
                        log.debug("Skipping over bad CDX line '" 
                                + record + "'", e);
                        continue;
                    }
                    log.trace("lastrecord is '" + record + "'");
                } catch (IOException e) {
                    throw new IOFailure("Error reading CDX record", e);
                }
            }
            if (foundRecord == null) {
                if(lastRecord == null) {
                    log.trace("No matchin CDX for URL '" + item.getURL() 
                            + "'. No last CDX was found.");
                } else {
                    log.trace("No matching CDX for URL '" + item.getURL()
                            + "'. Last CDX was for URL '" + lastRecord.getURL()
                            + "'");
                }
                
                return null;
            }

            String origin = foundRecord.getArcfile()
            + "," + foundRecord.getOffset();
            item.setOrigin(origin);
            log.trace("URL '" +  item.getURL() + "' combined with origin '"
                    +  origin + "'.");
        }
        return item;
    }
}
