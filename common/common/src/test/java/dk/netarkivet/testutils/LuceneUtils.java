/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.testutils;

import dk.netarkivet.archive.indexserver.CDXOriginCrawlLogIterator;
import dk.netarkivet.common.exceptions.IOFailure;

import is.hi.bok.deduplicator.CrawlLogIterator;
import is.hi.bok.deduplicator.DigestIndexer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class LuceneUtils {

    static final File ORIGINALS_DIR = new File("tests/dk/netarkivet/externalsoftware/data/launcher/originals");
    static final File EMPTY_CRAWLLOG_FILE = new File(ORIGINALS_DIR, "empty_crawl.log");

    /**
     * Create Dummy Lucene index.
     * uses an empty file as basis for the lucene-index.
     * @param indexLocation location of index
     * @throws IOFailure
     */
    public static void makeDummyIndex(File indexLocation) throws IOFailure  {
        try {
        // use empty crawl.log to generate default lucene index
        generateIndex(EMPTY_CRAWLLOG_FILE, new BufferedReader
                (new InputStreamReader(new ByteArrayInputStream(new byte[0]))),
                indexLocation);
        } catch (IOFailure e) {
            throw new IOFailure("Unable to create dummy lucene index", e);
        }
    }

    public static void generateIndex(File CrawlLog, BufferedReader cdxreader, File indexDir) {
        try {
            // Setup Lucene for indexing our crawllogs
            final boolean optimizeIndex = true;
            String indexLocation = indexDir.getAbsolutePath();
            // MODE_BOTH: Both URL's and Hash are indexed: Alternatives:
            // DigestIndexer.MODE_HASH or DigestIndexer.MODE_URL
            String indexingMode = DigestIndexer.MODE_BOTH;
            boolean includeNormalizedURL = false; // used to be 'equivalent' setting
            boolean includeTimestamp = true;     // used to be 'timestamp' setting
            boolean includeEtag = true;           // used to be 'etag' setting
            boolean addToExistingIndex = false;
            DigestIndexer indexer =
                new DigestIndexer(indexLocation,
                        indexingMode,
                        includeNormalizedURL,
                        includeTimestamp,
                        includeEtag,
                        addToExistingIndex);

            /** the blacklist set to true results in docs matching the mimefilter being ignored. */
            boolean blacklist = true;
            final String mimefilter = "^text/.*";
            final boolean verbose = false; //Avoids System.out.println's
            String defaultOrigin = "defaultOrigin";

                CrawlLogIterator reader = null;
                try {
                    reader = new CDXOriginCrawlLogIterator(CrawlLog, cdxreader);
                    indexer.writeToIndex(reader, mimefilter, blacklist, defaultOrigin, verbose);
                } finally {
                    if (reader != null) {
                        reader.close();
                    }
                }
            indexer.close(optimizeIndex);
        } catch (IOException e) {
            throw new IOFailure("Error setting up craw.log index framework for "
                    + indexDir, e);
        }
    }
}
