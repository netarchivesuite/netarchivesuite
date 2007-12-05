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
import java.util.Enumeration;

import is.hi.bok.deduplicator.DigestIndexer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

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
    /** The ArcRepositoryClient we use to retrieve records. */
    private final ViewerArcRepositoryClient arcRepositoryClient;

    /** The currently active lucene search engine. */
    private IndexSearcher luceneSearcher;

    /** Logger for this class. */
    private final Log log = LogFactory.getLog(getClass().getName());

    /** Create a new ARCLookup object.
     *
     * @param arcRepositoryClient The interface to the ArcRepository
     * @throws ArgumentNotValid if argument arcRepositoryClient is null.
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
     * @throws ArgumentNotValid If argument is null, or the indexDir argument
     * isn't a directory
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
        Query query = new TermQuery(
                new Term(DigestIndexer.FIELD_URL, uri));
        try {
            Hits hits = luceneSearcher.search(query);
            Document doc = null;
            if (hits != null) { // TODO Remove this test for null, because hits will never be null
                log.debug("Found " + hits.length() + " hits for the URL '"
                        +  uri + "'");              
                // Find the document with the latest timestamp if any             
                for (int i = 0 ; i < hits.length(); i++) {
                    Document nextDoc = hits.doc(i);
//                  Enumeration fieldsInDoc = nextDoc.fields();
//                    while  (fieldsInDoc.hasMoreElements()) {
//                        System.out.println(fieldsInDoc.nextElement());
//                    }
                    String origin = nextDoc.get(DigestIndexer.FIELD_ORIGIN);                    
                    // Here is where we will handle multiple hits in the future                   
                    if (origin == null) {
                        log.debug("No origin for URL '" + uri
                                  + "' hit " + i);
                        continue;
                    } else { // potential candidate
                        doc = chooseBestPotentialCandidate(doc, nextDoc, uri);
                    }
                }
                if (doc != null) { // found a document for the uri
                    return makeARCKey(doc.get(DigestIndexer.FIELD_ORIGIN), uri);
                }
            }
        } catch (IOException e) {
            throw new IOFailure("Fatal error looking up '" + uri + "'", e);
        }
        log.debug("URL '" + uri + "' not found in index");
        return null;
    }
    
    /**
     * Choose which document 
     * @param currentdoc
     * @param newDoc
     * @param uri
     * @return
     */
    private Document chooseBestPotentialCandidate(final Document currentdoc, final Document newDoc, final String uri) {
        if (currentdoc == null) { // Found 1st potential candidate
            return newDoc;
        }
        // Compare the timestamp of the last document with the current document
        String timestamp = currentdoc.get(DigestIndexer.FIELD_TIMESTAMP);
        String newTimestamp = newDoc.get(DigestIndexer.FIELD_TIMESTAMP);
        if (timestamp == null || newTimestamp == null) {
                throw new IllegalState("Timestamps missing for URL '"
                        + uri +  "'.");
        }
        int comparevalue = newTimestamp.compareTo(timestamp); 
        if (comparevalue < 0) { 
                return newDoc;
        } else if (comparevalue == 0) { // the dates are identical!
                log.debug("the timestamps are identical, we now look at the filename");
                // If timestamps are identical, look at the arcfile:
                //1-1-20071203154021-00000-kb-test-har-002.kb.dk.arc
                ARCKey currentARCKey = makeARCKey(currentdoc.get(DigestIndexer.FIELD_ORIGIN), uri);
                ARCKey newDocARCKey = makeARCKey(newDoc.get(DigestIndexer.FIELD_ORIGIN), uri);
                if (currentARCKey.getFile().equals(newDocARCKey.getFile())) {
                    if (currentARCKey.getOffset() < newDocARCKey.getOffset()) {
                        return newDoc;
                    }
                } else { // The two documents with equal timestamp comes from different files
                    // We will look at the timestamp embedded in the filename
                    String[] currentFilenameComponents = currentARCKey.getFile().getName().split("-");
                    String[] newFilenameComponents = newDocARCKey.getFile().getName().split("-");
                    //1-1-20071203154021-00000-kb-test-har-002.kb.dk.arc
                    final int ARC_TIMESTAMP_INDEX = 2;
                    final int RUNNING_NUMBER_INDEX = 3;
                    if (currentFilenameComponents[ARC_TIMESTAMP_INDEX].compareTo(newFilenameComponents[ARC_TIMESTAMP_INDEX]) > 0) {
                        return newDoc;
                    } else if (currentFilenameComponents[ARC_TIMESTAMP_INDEX].compareTo(newFilenameComponents[ARC_TIMESTAMP_INDEX]) == 0) {
                        // equal arcstamps.
                        if (currentFilenameComponents[RUNNING_NUMBER_INDEX].compareTo(newFilenameComponents[RUNNING_NUMBER_INDEX]) > 0) {
                            return newDoc;
                        }
                    }
                }
            } 
        return currentdoc;
    }
    
    
    /**
     * Make an ARCKey from a origin found in index.
     * @param origin the given origin
     * @param uri the given uri (only used in exception)
     * @return ARCKey contained in origin
     * @throws IllegalState If origin is badly formatted
     */
    private ARCKey makeARCKey(String origin, String uri) {
        String[] originParts = origin.split(",");
        if (originParts.length != 2) {
            throw new IllegalState("Bad origin for URL '"
                    + uri + "': '" + origin + "'");
        }
        return new ARCKey(originParts[0],
                Long.parseLong(originParts[1]));
        
    }
    
    
    
}
