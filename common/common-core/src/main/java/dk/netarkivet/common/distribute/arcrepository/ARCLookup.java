/*
 * #%L
 * Netarchivesuite - common
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

package dk.netarkivet.common.distribute.arcrepository;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermRangeFilter;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.AllDocsCollector;
import dk.netarkivet.common.utils.arc.ARCKey;
import is.hi.bok.deduplicator.DigestIndexer;

/**
 * This class allows lookup of URLs in the ArcRepository, using full Lucene indexes to find offsets. The input takes the
 * form of a directory containing a Lucene index.
 */
public class ARCLookup {

    /** Logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(ARCLookup.class);

    /** The ArcRepositoryClient we use to retrieve records. */
    private final ViewerArcRepositoryClient arcRepositoryClient;

    /** The currently active lucene search engine. */
    private IndexSearcher luceneSearcher;
    /** The Indexreader used by the index-searcher. */
    private IndexReader luceneReader;

    /** If the value is true, we will try to lookup w/ ftp instead of http, if we don't get a hit in the index. */
    private boolean tryToLookupUriAsFtp;

    /**
     * Create a new ARCLookup object.
     *
     * @param arcRepositoryClient The interface to the ArcRepository
     * @throws ArgumentNotValid if arcRepositoryClient is null.
     */
    public ARCLookup(ViewerArcRepositoryClient arcRepositoryClient) {
        ArgumentNotValid.checkNotNull(arcRepositoryClient, "ArcRepositoryClient arcRepositoryClient");
        this.arcRepositoryClient = arcRepositoryClient;
        luceneSearcher = null;
    }

    /**
     * The setter for the option to search for URIs with ftp instead of http as the scheme. 
     * Note that The scheme information is absent from the original URI request, when the request arrives here
     * 
     * @param searchForFtpUri if true, we replace the http schema with ftp and try again, if unsuccessful with http as
     * the schema
     */
    public void setTryToLookupUriAsFtp(boolean searchForFtpUri) {
        this.tryToLookupUriAsFtp = searchForFtpUri;
    }

    /**
     * This method sets the current Lucene index this object works on, replacing and closing the current index if one is
     * already set.
     *
     * @param indexDir The new index, a directory containing Lucene files.
     * @throws ArgumentNotValid If argument is null
     */
    public void setIndex(File indexDir) {
        ArgumentNotValid.checkNotNull(indexDir, "File indexDir");
        ArgumentNotValid.checkTrue(indexDir.isDirectory(), "indexDir '" + indexDir + "' should be a directory");
        if (luceneSearcher != null) {
            try {
                // Existing lucene indices must be shut down
                luceneReader.close();
            } catch (IOException e) {
                throw new IOFailure("Unable to close index " + luceneSearcher, e);
            } finally {
                // Must be careful to shut down only once.
                luceneSearcher = null;
            }
        }
        try {
            luceneReader = org.apache.lucene.index.DirectoryReader.open(FSDirectory.open(indexDir));
            luceneSearcher = new IndexSearcher(luceneReader);
        } catch (IOException e) {
            throw new IOFailure("Unable to find/open index " + indexDir, e);
        }
    }

    /**
     * Look up a given URI and return the contents as an InputStream. The uri is first checked using url-decoding (e.g.
     * "," in the argument is converted to "%2C"). If this returns no match, the method then searches for a
     * non-url-decoded match. If neither returns a match the method returns null.
     * <p>
     * If the tryToLookupUriAsFtp field is set to true, we will try exchanging the schema with ftp, whenever we can't
     * lookup the uri with the original schema.
     *
     * @param uri The URI to find in the archive. If the URI does not match any entries in the archive, null is
     * returned.
     * @return An InputStream Containing all the data in the entry, or null if the entry was not found
     * @throws IOFailure If the ARC file was found in the Lucene index but not in the bit archive, or if some other
     * failure happened while finding the file.
     */
    public ResultStream lookup(URI uri) {
        ArgumentNotValid.checkNotNull(uri, "uri");
        log.debug("Doing lookup of {}", uri);
        boolean containsHeader = true;
        // the URI.getSchemeSpecificPart() carries out the url-decoding
        ARCKey key = luceneLookup(uri.getScheme() + ":" + uri.getSchemeSpecificPart());
        if (key == null) {
            // the URI.getRawSchemeSpecificPart() returns the uri in non-decoded form
            key = luceneLookup(uri.getScheme() + ":" + uri.getRawSchemeSpecificPart());
        }

        if (key == null && tryToLookupUriAsFtp) {
            log.debug("Url not found with the schema '{}'. Now trying with 'ftp' as the schema", uri.getScheme());
            final String ftpSchema = "ftp";
            key = luceneLookup(ftpSchema + ":" + uri.getSchemeSpecificPart());
            if (key == null) {
                key = luceneLookup(ftpSchema + ":" + uri.getRawSchemeSpecificPart());
                if (key != null) {
                    // Remember, that the found ftp-records don't have any HTTP
                    // Header
                    containsHeader = false;
                }
            } else {
                // Remember, that the found ftp-record don't have any HTTP
                // Header
                containsHeader = false;
            }
        }

        if (key == null) {
        	log.debug("Lookup failed for uri '{}'");
            return null; // key not found
        } else {
        	log.debug("Retrieving record {},{} from archive", key.getFile().getName(), key.getOffset());
            final BitarchiveRecord bitarchiveRecord = arcRepositoryClient.get(key.getFile().getName(), key.getOffset());
            if (bitarchiveRecord == null) {
                String message = "ARC file '" + key.getFile().getName() + "' mentioned in index file was not found by"
                        + " arc repository. This may mean we have a timeout, or that the index is wrong; or"
                        + " it may mean we have lost a record in the bitarchives.";
                log.debug(message);
                throw new IOFailure(message);
            }
            log.debug("Retrieved record {},{} from archive and returning it as ResultStream", key.getFile().getName(), key.getOffset());
            return new ResultStream(bitarchiveRecord.getData(), containsHeader);
        }
    }

    /**
     * Looks up a URI in our lucene index and extracts a key.
     *
     * @param uri A URI to look for.
     * @return The file and offset where that URI can be found, or null if it doesn't exist.
     * @throws IllegalState If a URL is found with a malformed origin field.
     * @throws IOFailure if no index is set or Lucene gives problems.
     */
    private ARCKey luceneLookup(String uri) {
        if (luceneSearcher == null) {
            throw new IOFailure("No index set while searching for '" + uri + "'");
        }
        return luceneLookUp(uri);
    }

    /**
     * Lucene Lookup. 
     *
     * @param uri A URI to look for.
     * @return The file and offset where that URI can be found, or null if it doesn't exist. 
     */
    private ARCKey luceneLookUp(String uri) {
        BytesRef uriRef = new BytesRef(uri.getBytes()); // Should we decide which charset?

        Query query = new ConstantScoreQuery(new TermRangeFilter(DigestIndexer.FIELD_URL, uriRef, uriRef, true, true));

        try {
            AllDocsCollector allResultsCollector = new AllDocsCollector();
            luceneSearcher.search(query, allResultsCollector);
            Document doc = null;
            List<ScoreDoc> hits = allResultsCollector.getHits();
            if (hits != null) {
                log.debug("Found {} hits for uri: {}", hits.size(), uri);
                int i = 0;
                for (ScoreDoc hit : hits) {
                    int docId = hit.doc;
                    doc = luceneSearcher.doc(docId);
                    String origin = doc.get(DigestIndexer.FIELD_ORIGIN);
                    // Here is where we will handle multiple hits in the future
                    if (origin == null) {
                        log.debug("No origin for URL '{}' hit {}", uri, i++);
                        continue;
                    }
                    String[] originParts = origin.split(",");
                    if (originParts.length < 2) {
                        throw new IllegalState("Bad origin for URL '" + uri + "': '" + origin + "'");
                    }
                    log.debug("Found document with origin: {}", origin);
                    return new ARCKey(originParts[0], Long.parseLong(originParts[1]));
                }
            }
        } catch (IOException e) {
            throw new IOFailure("Fatal error looking up '" + uri + "'", e);
        }
        return null;
    }

}
