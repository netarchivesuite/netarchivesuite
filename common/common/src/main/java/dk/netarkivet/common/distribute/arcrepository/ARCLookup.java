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

package dk.netarkivet.common.distribute.arcrepository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import is.hi.bok.deduplicator.DigestIndexer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RangeFilter;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.arc.ARCKey;

/**
 * This class allows lookup of URLs in the ArcRepository, using full Lucene
 * indexes to find offsets.  The input takes the form of a directory
 * containing a Lucene index.
 */

public class ARCLookup {
    /** The ArcRepositoryClient we use to retrieve records */
    private final ViewerArcRepositoryClient arcRepositoryClient;

    /** The currently active lucene search engine. */
    private IndexSearcher luceneSearcher;

    /** Logger for this class. */
    private final Log log = LogFactory.getLog(getClass().getName());

    /** Create a new ARCLookup object.
     *
     * @param arcRepositoryClient The interface to the ArcRepository
     * @throws ArgumentNotValid if arcRepositoryClient is null.
     */
    public ARCLookup(ViewerArcRepositoryClient arcRepositoryClient) {
        ArgumentNotValid.checkNotNull(
                arcRepositoryClient, "ArcRepositoryClient arcRepositoryClient");
        this.arcRepositoryClient = arcRepositoryClient;
        luceneSearcher = null;
    }

    /** This method sets the current Lucene index this object works
     * on, replacing and closing the current index if one is already set.
     *
     * @param indexDir The new index, a directory containing Lucene files.
     * @throws ArgumentNotValid If argument is null
     */
    public void setIndex(File indexDir) {
        ArgumentNotValid.checkNotNull(indexDir, "File indexDir");
        ArgumentNotValid.checkTrue(indexDir.isDirectory(),
                                   "indexDir '" + indexDir + "' should be a directory");
        if (luceneSearcher != null) {
            try {
                // Existing lucene indices must be shut down
                luceneSearcher.close();
            } catch (IOException e) {
                throw new IOFailure("Unable to close index " + luceneSearcher,
                        e);
            } finally {
                // Must be careful to shut down only once.
                luceneSearcher = null;
            }
        }
        try {
            luceneSearcher = new IndexSearcher(indexDir.getAbsolutePath());
        } catch (IOException e) {
            throw new IOFailure("Unable to find/open index " + indexDir, e);
        }
    }

    /** Look up a given URI and return the contents as an InputStream.
     * @param uri The URI to find in the archive.  If the URI does not
     * match any entries in the archive, IOFailure is thrown.
     * @return An InputStream Containing all the data in the entry, or
     * null if the entry was not found
     * @throws IOFailure If the ARC file was found in the Lucene index but not
     * in the bit archive, or if some other failure happened while finding
     * the file.
     */
    public InputStream lookup(URI uri) {
        ArgumentNotValid.checkNotNull(uri, "uri");
        ARCKey key = luceneLookup(uri.toString());
        if (key == null) {
            return null; // key not found
        } else {
            final BitarchiveRecord bitarchiveRecord =
                    arcRepositoryClient.get(key.getFile().getName(), key.getOffset());
            if (bitarchiveRecord == null) {
                String message = "ARC file '" + key.getFile().getName()
                        + "' mentioned in index file was not found by"
                        + " arc repository. This may mean we have a"
                        + " timeout, or that the index is wrong; or"
                        + " it may mean we have lost a record in"
                        + " the bitarchives.";
                log.debug(message);
                throw new IOFailure(message);
            }
            return bitarchiveRecord.getData();
        }
    }

    /** Looks up a URI in our lucene index and extracts a key.
     *
     * @param uri A URI to look for.
     * @return The file and offset where that URI can be found, or null if it
     * doesn't exist.
     * @throws IllegalState If a URL is found with a malformed origin field.
     * @throws IOFailure if no index is set or Lucene gives problems.
     */
    private ARCKey luceneLookup(String uri) {
        if (luceneSearcher == null) {
            throw new IOFailure("No index set while searching for '"
                    + uri + "'");
        }
        // RangeFilter + ConstantScoreQuery means we ignore norms and other
        // memory-eating things we don't need that TermQuery would imply.
        Query query = new ConstantScoreQuery(
                new RangeFilter(DigestIndexer.FIELD_URL, uri, uri, true, true));
        try {
            Hits hits = luceneSearcher.search(query);
            Document doc = null;
            if (hits != null) {
                log.debug("Found " + hits.length() + " hits for uri: " +  uri);
                for (int i = 0 ; i < hits.length(); i++) {
                    doc = hits.doc(i);
                    String origin = doc.get(DigestIndexer.FIELD_ORIGIN);
                    // Here is where we will handle multiple hits in the future
                    if (origin == null) {
                        log.debug("No origin for URL '" + uri
                                  + "' hit " + i);
                        continue;
                    }
                    String[] originParts = origin.split(",");
                    if (originParts.length != 2) {
                        throw new IllegalState("Bad origin for URL '"
                                + uri + "': '" + origin + "'");
                    }
                    log.debug("Found document with origin: " + origin);
                    return new ARCKey(originParts[0],
                            Long.parseLong(originParts[1]));
                }
            }
        } catch (IOException e) {
            throw new IOFailure("Fatal error looking up '" + uri + "'", e);
        }
        return null;
    }
}
