/* DeDupFetchHTTP
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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermRangeFilter;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.fetcher.FetchHTTP;
import org.archive.crawler.frontier.AdaptiveRevisitAttributeConstants;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;
import org.archive.httpclient.HttpRecorderMethod;
import org.archive.util.ArchiveUtils;

import dk.netarkivet.common.utils.AllDocsCollector;

/**
 * An extension of Heritrix's {@link org.archive.crawler.fetcher.FetchHTTP}
 * processor for downloading HTTP documents. This extension adds a check after
 * the content header has been downloaded that compares the 'last-modified' and
 * or 'last-etag' values from the header against information stored in an 
 * appropriate index.
 * 
 * @author Kristinn Sigur&eth;sson
 * @author Søren Vejrup Carlsen
 * @see is.hi.bok.deduplicator.DigestIndexer
 * @see org.archive.crawler.fetcher.FetchHTTP
 */

public class DeDupFetchHTTP extends FetchHTTP 
implements AdaptiveRevisitAttributeConstants {

    private static final long serialVersionUID =
        ArchiveUtils.classnameBasedUID(DeDupFetchHTTP.class,1);
    
    private static Logger logger = Logger.getLogger(FetchHTTP.class.getName());

    protected IndexSearcher index;
    protected IndexReader indexReader;    
    protected String mimefilter = DEFAULT_MIME_FILTER;
    protected boolean blacklist = true;

    SimpleDateFormat sdfLastModified;
    SimpleDateFormat sdfIndexDate;
    
    protected long processedURLs = 0;
    protected long unchangedURLs = 0;

    protected boolean useSparseRangeFilter = DEFAULT_USE_SPARSE_RANGE_FILTER;
    
    // Settings.
    public static final String ATTR_DECISION_SCHEME = "decision-scheme";
    public static final String SCHEME_TIMESTAMP = "Timestamp only";
    public static final String SCHEME_ETAG = "Etag only";
    public static final String SCHEME_TIMESTAMP_AND_ETAG = "Timestamp AND Etag";
    public static final String SCHEME_TIMESTAMP_OR_ETAG = "Timestamp OR Etag";
    public static final String[] AVAILABLE_DECISION_SCHEMES = {
        SCHEME_TIMESTAMP,
        SCHEME_ETAG,
        SCHEME_TIMESTAMP_AND_ETAG,
        SCHEME_TIMESTAMP_OR_ETAG
        };
    public static final String DEFAULT_DECISION_SCHEME = 
        SCHEME_TIMESTAMP;

    public static final String ATTR_INDEX_LOCATION = "index-location";
    public static final String DEFAULT_INDEX_LOCATION = "";

    /** The filter on mime types. This is either a blacklist or whitelist
     *  depending on ATTR_FILTER_MODE.
     */
    public final static String ATTR_MIME_FILTER = "mime-filter";
    public final static String DEFAULT_MIME_FILTER = "^text/.*";

    /** Is the mime filter a blacklist (do not apply processor to what matches) 
     *  or whitelist (apply processor only to what matches).
     */
    public final static String ATTR_FILTER_MODE = "filter-mode";
    public final static String[] AVAILABLE_FILTER_MODES = {
        "Blacklist",
        "Whitelist"
    };
    public final static String DEFAULT_FILTER_MODE = AVAILABLE_FILTER_MODES[0];

    /** Should we use sparse queries (uses less memory at a cost to performance? **/
    public final static String ATTR_USE_SPARSE_RANGE_FILTER = "use-sparse-range-filter";
    public final static Boolean DEFAULT_USE_SPARSE_RANGE_FILTER = new Boolean(false);
    
    public DeDupFetchHTTP(String name){
        super(name);
        setDescription("Fetch HTTP processor that aborts downloading of " +
                "unchanged documents. This processor extends the standard " +
                "FetchHTTP processor, adding a check after the header is " +
                "downloaded where the header information for 'last-modified' " +
                "and 'etag' is compared against values stored in a Lucene " +
                "index built using the DigestIndexer.\n Note that the index " +
                "must have been built indexed by URL and the Timestamp " +
                "and/or Etag info must have been included in the index!");
        Type t;
        t = new SimpleType(
                ATTR_DECISION_SCHEME,
                "The different schmes for deciding when to re-download a " +
                "page given an old version of the same page (or rather " +
                "meta-data on it)\n " +
                "Timestamp only: Download when a datestamp is missing " +
                "in either the downloaded header or index or if the header " +
                "datestamp is newer then the one in the index.\n " +
                "Etag only: Download when the Etag is missing in either the" +
                "header download or the index or the header Etag and the one " +
                "in the index differ.\n " +
                "Timestamp AND Etag: When both datestamp and Etag are " +
                "available in both the header download and the index, " +
                "download if EITHER of them indicates change." +
                "Timestamp OR Etag: When both datestamp and Etag are " +
                "available in both the header download and the index, " +
                "download only if BOTH of them indicate change.",
                DEFAULT_DECISION_SCHEME,AVAILABLE_DECISION_SCHEMES);
        addElementToDefinition(t);
        t = new SimpleType(
                ATTR_INDEX_LOCATION,
                "Location of index (full path). Can not be changed at run " +
                "time.",
                DEFAULT_INDEX_LOCATION);
        t.setOverrideable(false);
        addElementToDefinition(t);
        t = new SimpleType(
                ATTR_MIME_FILTER,
                "A regular expression that the mimetype of all documents " +
                "will be compared against. Only those that pass will be " +
                "considered. Others are given a pass. " +
                "\nIf the attribute filter-mode is " +
                "set to 'Blacklist' then all the documents whose mimetype " +
                "matches will be ignored by this processor. If the filter-" +
                "mode is set to 'Whitelist' only those documents whose " +
                "mimetype matches will be processed.",
                DEFAULT_MIME_FILTER);
        t.setOverrideable(false);
        t.setExpertSetting(true);
        addElementToDefinition(t);
        t = new SimpleType(
                ATTR_FILTER_MODE,
                "Determines if the mime-filter acts as a blacklist (declares " +
                "what should be ignored) or whitelist (declares what should " +
                "be processed).",
                DEFAULT_FILTER_MODE,AVAILABLE_FILTER_MODES);
        t.setOverrideable(false);
        t.setExpertSetting(true);
        addElementToDefinition(t);

        t = new SimpleType(
                ATTR_USE_SPARSE_RANGE_FILTER,
                "If set to true, then Lucene queries use a custom 'sparse' " +
                "range filter. This uses less memory at the cost of some " +
                "lost performance. Suitable for very large indexes.",
                DEFAULT_USE_SPARSE_RANGE_FILTER);
        t.setOverrideable(false);
        t.setExpertSetting(true);
        addElementToDefinition(t);
    }

    protected boolean checkMidfetchAbort(
            CrawlURI curi, HttpRecorderMethod method, HttpConnection conn) {
        // We'll check for prerequisites here since there is no way to know
        // if the super method returns false because of a prereq or because
        // all filters accepeted.
        if(curi.isPrerequisite()){
            return false;
        }
        
        // Run super to allow filters to also abort. Also this method has 
        // been pressed into service as a general 'stuff to do at this point'
        boolean ret = super.checkMidfetchAbort(curi, method, conn);
        
        // Ok, now check for duplicates.
        if(isDuplicate(curi)){
            ret = true;
            unchangedURLs++;
            curi.putInt(A_CONTENT_STATE_KEY, CONTENT_UNCHANGED);
            curi.addAnnotation("header-duplicate");

        }
        
        return ret;
    }

    /**
     * Compare the header infomation for 'last-modified' and/or 'etag' against
     * data in the index.
     * @param curi The Crawl URI being processed.
     * @return True if header infomation indicates that the document has not
     *         changed since the crawl that the index is based on was performed.
     */
    protected boolean isDuplicate(CrawlURI curi) {
        boolean ret = false;
        if(curi.getContentType() != null && 
                curi.getContentType().matches(mimefilter) != blacklist){
            processedURLs++;
            // Ok, passes mime-filter
            HttpMethod method = (HttpMethod)curi.getObject(A_HTTP_TRANSACTION);
            // Check the decision scheme.
            String scheme = (String)getUncheckedAttribute(
                    curi,ATTR_DECISION_SCHEME);
            
            Document doc = lookup(curi);
            
            if(doc != null){
                // Found a hit. Do the necessary evalution.
                if(scheme.equals(SCHEME_TIMESTAMP)){
                    ret = datestampIndicatesNonChange(method,doc);
                } else if(scheme.equals(SCHEME_ETAG)){
                    ret = etagIndicatesNonChange(method,doc);
                } else {
                    
                    if(scheme.equals(SCHEME_TIMESTAMP_AND_ETAG)){
                        ret = datestampIndicatesNonChange(method,doc) 
                            && etagIndicatesNonChange(method,doc);
                    } else if(scheme.equals(SCHEME_TIMESTAMP_OR_ETAG)){
                        ret = datestampIndicatesNonChange(method,doc) 
                            || etagIndicatesNonChange(method,doc);
                    } else {
                        logger.log(Level.SEVERE, "Unknown decision sceme: " + scheme);
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Checks if the 'last-modified' in the HTTP header and compares it against
     * the timestamp in the supplied Lucene document. If both dates are found
     * and the header's date is older then the datestamp indicates non-change. 
     * Otherwise a change must be assumed.
     * @param method HTTPMethod that allows access to the relevant HTTP header
     * @param doc The Lucene document to compare against
     * @return True if a the header and document data indicates a non-change. 
     *         False otherwise.
     */
    protected boolean datestampIndicatesNonChange(
            HttpMethod method, Document doc) {
        String headerDate = null;
        if (method.getResponseHeader("last-modified") != null) {
            headerDate = method.getResponseHeader("last-modified").getValue();
        }
        String indexDate = doc.get(DigestIndexer.FIELD_TIMESTAMP);
        
        if(headerDate != null && indexDate != null){
            try {
                // If both dates exist and last-modified is before the index
                // date then we assume no change has occured.
                return (sdfLastModified.parse(headerDate)).before(
                        sdfIndexDate.parse(indexDate));
            } catch (Exception e) {
                // Any exceptions parsing the date should be interpreted as
                // missing date information.
                // ParseException and NumberFormatException are the most 
                // likely exceptions to occur.
                return false;
            }
        }
        return false;
    }

    /**
     * Checks if the 'etag' in the HTTP header and compares it against
     * the etag in the supplied Lucene document. If both dates are found
     * and match then the datestamp indicate non-change. 
     * Otherwise a change must be assumed.
     * @param method HTTPMethod that allows access to the relevant HTTP header
     * @param doc The Lucene document to compare against
     * @return True if a the header and document data indicates a non-change. 
     *         False otherwise.
     */
    protected boolean etagIndicatesNonChange(
            HttpMethod method, Document doc) {
        String headerEtag = null;
        if (method.getResponseHeader("last-etag") != null) {
            headerEtag = method.getResponseHeader("last-etag").getValue();
        }
        String indexEtag = doc.get(DigestIndexer.FIELD_ETAG);
        
        if(headerEtag != null && indexEtag != null){
            // If both etags exist and are identical then we assume no 
            // change has occured.
            return headerEtag.equals(indexEtag);
        }
        return false;
    }

    /**
     * Searches the index for the URL of the given CrawlURI. If multiple hits
     * are found the most recent one is returned if the index included the 
     * timestamp, otherwise a random one is returned. 
     * If no hit is found null is returned.
     * @param curi The CrawlURI to search for
     * @return the index Document matching the URI or null if none was found
     */
    protected Document lookup(CrawlURI curi) {
        try{
            Query query = null;

            /** The least memory demanding query. */
            BytesRef curiStringRef = new BytesRef(curi.toString().getBytes());
            query = new ConstantScoreQuery(
                  new TermRangeFilter(DigestIndexer.FIELD_URL, curiStringRef, curiStringRef, true, true));
            
            /** The preferred solution, but it seems also more memory demanding */
            //query = new ConstantScoreQuery(new FieldCacheTermsFilter(fieldName,
            //        value));
            
            AllDocsCollector collectAllCollector = new AllDocsCollector();
            index.search(query, collectAllCollector);
            
            List<ScoreDoc> hits = collectAllCollector.getHits();
            Document doc = null;
            if(hits != null && hits.size() > 0){
                // If there are multiple hits, use the one with the most
                // recent date.
                Document docToEval = null;
                for (ScoreDoc hit: hits) {
                    int docId = hit.doc;
                    doc = index.doc(docId);
                    // The format of the timestamp ("yyyyMMddHHmmssSSS") allows
                    // us to do a greater then (later) or lesser than (earlier)
                    // comparison of the strings.
                    String timestamp = doc.get(DigestIndexer.FIELD_TIMESTAMP);
                    if(docToEval == null || timestamp == null 
                            || docToEval.get(DigestIndexer.FIELD_TIMESTAMP)
                                .compareTo(timestamp)>0){
                        // Found a more recent hit or timestamp is null
                        // NOTE: Either all hits should have a timestamp or 
                        // none. This implementation will cause the last 
                        // URI in the hit list to be returned if there is no
                        // timestamp.
                        docToEval = doc;
                    }
                }
                return docToEval;
            }
        } catch(IOException e){
            logger.log(Level.SEVERE,"Error accessing index.",e);
        }
        return null;
    }
    @Override
    public void finalTasks() {
        super.finalTasks();
    }
    
    @Override
    public void initialTasks() {
        super.initialTasks();
        // Index location
        try {
            String indexLocation = (String)getAttribute(ATTR_INDEX_LOCATION);
            FSDirectory indexDir = FSDirectory.open(new File(indexLocation));
            //https://issues.apache.org/jira/browse/LUCENE-1566
            // Reduce chunksize to avoid OOM to half the size of the default (=100 MB)
            int chunksize = indexDir.getReadChunkSize();
            indexDir.setReadChunkSize(chunksize / 2);
            IndexReader reader = DirectoryReader.open(indexDir);
            index = new IndexSearcher(reader);
        } catch (Exception e) {
            logger.log(Level.SEVERE,"Unable to find/open index.",e);
        } 
        
        // Mime filter
        try {
            mimefilter = (String)getAttribute(ATTR_MIME_FILTER);
        } catch (Exception e) {
            logger.log(Level.SEVERE,"Unable to get attribute " + 
                    ATTR_MIME_FILTER,e);
        }
        
        // Filter mode (blacklist (default) or whitelist)
        try {
            blacklist = ((String)getAttribute(ATTR_FILTER_MODE)).equals(
                            DEFAULT_FILTER_MODE);
        } catch (Exception e) {
            logger.log(Level.SEVERE,"Unable to get attribute " + 
                    ATTR_FILTER_MODE,e);
        }
        
        // Date format of last-modified is EEE, dd MMM yyyy HH:mm:ss z
        sdfLastModified = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        // Date format of indexDate is yyyyMMddHHmmssSSS
        sdfIndexDate = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        
        // Range Filter type
        try {
            useSparseRangeFilter = ((Boolean)getAttribute(
			        ATTR_USE_SPARSE_RANGE_FILTER)).booleanValue();
        } catch (Exception e) {
            logger.log(Level.SEVERE,"Unable to get attribute " + 
                    ATTR_USE_SPARSE_RANGE_FILTER,e);
            useSparseRangeFilter = DEFAULT_USE_SPARSE_RANGE_FILTER;
        }
    }
    
    @Override
    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: is.hi.bok.deduplicator.DeDupFetchHTTP\n");
        ret.append("  URLs compared against index: " + processedURLs + "\n");
        ret.append("  URLs judged unchanged:       " + unchangedURLs + "\n");
        ret.append("  processor extends (parent report)\n");
        ret.append(super.report());
        return ret.toString();
    }

}
