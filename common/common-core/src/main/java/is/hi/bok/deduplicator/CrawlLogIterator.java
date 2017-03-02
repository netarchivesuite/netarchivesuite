/* CrawlLogIterator
 * 
 * Created on 10.04.2006
 *
 * Copyright (C) 2006 National and University Library of Iceland
 * 
 * This file is part of the DeDuplicator (Heritrix add-on module).
 * 
 * DeDuplicator is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 * 
 * DeDuplicator is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with DeDuplicator; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package is.hi.bok.deduplicator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An implementation of a {@link is.hi.bok.deduplicator.CrawlDataIterator} capable of iterating over a Heritrix's style
 * <code>crawl.log</code>.
 *
 * @author Kristinn Sigur&eth;sson
 * @author Lars Clausen
 */
public class CrawlLogIterator extends CrawlDataIterator {

    private Log logger = LogFactory.getLog(getClass().getName());

    protected final String crawlDateFormatStr = "yyyyMMddHHmmss";
    protected final String fallbackCrawlDateFormatStr = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    /**
     * The date format used in crawl.log files.
     */
    protected final SimpleDateFormat crawlDateFormat = new SimpleDateFormat(crawlDateFormatStr);
    protected final SimpleDateFormat fallbackCrawlDateFormat = new SimpleDateFormat(fallbackCrawlDateFormatStr);

    /**
     * The date format specified by the {@link CrawlDataItem} for dates entered into it (and eventually into the index)
     */
    protected final SimpleDateFormat crawlDataItemFormat = new SimpleDateFormat(CrawlDataItem.dateFormat);

    /**
     * A reader for the crawl.log file being processed
     */
    protected BufferedReader in;

    /**
     * The next item to be issued (if ready) or null if the next item has not been prepared or there are no more
     * elements
     */
    protected CrawlDataItem next;

    /**
     * Create a new CrawlLogIterator that reads items from a Heritrix crawl.log
     *
     * @param source The path of a Heritrix crawl.log file.
     * @throws IOException If errors were found reading the log.
     */
    public CrawlLogIterator(String source) throws IOException {
        super(source);
        in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(source))));
    }

    /**
     * Returns true if there are more items available.
     *
     * @return True if at least one more item can be fetched with next().
     */
    public boolean hasNext() throws IOException {
        if (next == null) {
            prepareNext();
        }
        return next != null;
    }

    /**
     * Returns the next valid item from the crawl log.
     *
     * @return An item from the crawl log. Note that unlike the Iterator interface, this method returns null if there
     * are no more items to fetch.
     * @throws IOException If there is an error reading the item *after* the item to be returned from the crawl.log.
     * @throws NoSuchElementException If there are no more items
     */
    public CrawlDataItem next() throws IOException {
        if (hasNext()) {
            CrawlDataItem tmp = next;
            this.next = null;
            return tmp;
        }
        throw new NoSuchElementException("No more items");
    }

    /**
     * Ready the next item. This method will skip over items that getNextItem() rejects. When the method returns, either
     * next is non-null or there are no more items in the crawl log.
     * <p>
     * Note: This method should only be called when <code>next==null<code>
     */
    protected void prepareNext() throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            next = parseLine(line);
            if (next != null) {
                return;
            }
        }
    }

    /**
     * Parse the a line in the crawl log.
     * <p>
     * Override this method to change how individual crawl log items are processed and accepted/rejected. This method is
     * called from within the loop in prepareNext().
     *
     * @param line A line from the crawl log. Must not be null.
     * @return A {@link CrawlDataItem} if the next line in the crawl log yielded a usable item, null otherwise.
     */
    protected CrawlDataItem parseLine(String line) {
        if (line != null && line.length() > 42) {
            // Split the line up by whitespaces.
            // Limit to 12 parts (annotations may contain spaces, but will
            // always be at the end of each line.
            String[] lineParts = line.split("\\s+", 12);

            if (lineParts.length < 10) {
                // If the lineParts are fewer then 10 then the line is
                // malformed.
                return null;
            }

            // Index 0: Timestamp
            String timestamp;
            try {
                // Convert from crawl.log format to the format specified by
                // CrawlDataItem
                // the 8th item, for example 20170116161421526+52
                // -> we keep the numbers until the seconds : 20170116161421
                String timestampTrunc = lineParts[8].substring(0, crawlDateFormatStr.length());
                timestamp = crawlDataItemFormat.format(crawlDateFormat.parse(timestampTrunc));
            } catch (Exception e) {
                try {
                    timestamp = crawlDataItemFormat.format(fallbackCrawlDateFormat.parse(lineParts[0]));
                } catch (ParseException e1) {
                    logger.debug("Error parsing date for crawl log entry: " + line);
                    return null;
                }

            }

            // Index 1: status return code (ignore)
            // Index 2: File size (ignore)

            // Index 3: URL
            String url = lineParts[3];

            // Index 4: Hop path (ignore)
            // Index 5: Parent URL (ignore)

            // Index 6: Mime type
            String mime = lineParts[6];

            // Index 7: ToeThread number (ignore)
            // Index 8: ArcTimeAndDuration (ignore)

            // Index 9: Digest
            String digest = lineParts[9];
            // The digest may contain a prefix.
            // The prefix will be terminated by a : which is immediately
            // followed by the actual digest
            if (digest.lastIndexOf(":") >= 0) {
                digest = digest.substring(digest.lastIndexOf(":") + 1);
            }

            // Index 10: Source tag (ignore)

            // Index 11: Annotations (may be missing)
            String origin = null;
            boolean duplicate = false;
            if (lineParts.length == 12) {
                // Have an annotation field. Look for origin inside it.
                // Origin can be found in the 'annotations' field, preceeded by
                // 'deduplicate:' (no quotes) and contained within a pair of
                // double quotes. Example: deduplicate:"origin".
                // Can very possibly be missing.
                String annotation = lineParts[11];

                int startIndex = annotation.indexOf("duplicate:\"");
                if (startIndex >= 0) {
                    // The annotation field contains origin info. Extract it.
                    startIndex += 11; // Skip over the ]deduplicate:"' part
                    int endIndex = annotation.indexOf('"', startIndex + 1);
                    origin = annotation.substring(startIndex, endIndex);
                    // That also means this is a duplicate of an URL from an
                    // earlier crawl
                    duplicate = true;
                } else if (annotation.contains("duplicate")) {
                    // Is a duplicate of an URL from an earlier crawl but
                    // no origin information was recorded
                    duplicate = true;
                }
            }
            // Got a valid item.
            return new CrawlDataItem(url, digest, timestamp, null, mime, origin, duplicate);
        }
        return null;
    }

    /**
     * Closes the crawl.log file.
     */
    public void close() throws IOException {
        in.close();
    }

    /*
     * (non-Javadoc)
     * 
     * @see is.hi.bok.deduplicator.CrawlDataIterator#getSourceType()
     */
    public String getSourceType() {
        return "Handles Heritrix style crawl.log files";
    }

}
