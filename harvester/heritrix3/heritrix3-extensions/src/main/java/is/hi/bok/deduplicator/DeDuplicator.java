/* DeDuplicator
 * 
 * Created on 10.04.2006
 *
 * Copyright (C) 2006-2010 National and University Library of Iceland
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

import static is.hi.bok.deduplicator.DedupAttributeConstants.A_CONTENT_STATE_KEY;
import static is.hi.bok.deduplicator.DedupAttributeConstants.CONTENT_UNCHANGED;
import static org.archive.modules.recrawl.RecrawlAttributeConstants.A_CONTENT_DIGEST;
import static org.archive.modules.recrawl.RecrawlAttributeConstants.A_FETCH_HISTORY;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import org.archive.modules.CrawlURI;
import org.archive.modules.ProcessResult;
import org.archive.modules.Processor;
import org.archive.modules.net.ServerCache;
import org.archive.modules.revisit.IdenticalPayloadDigestRevisit;
import org.archive.util.ArchiveUtils;
import org.archive.util.Base32;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import dk.netarkivet.common.utils.AllDocsCollector;

/**
 * Heritrix compatible processor.
 * <p>
 * Will determine if CrawlURIs are <i>duplicates</i>. 
 * <p>
 * Duplicate detection can only be performed <i>after</i> the fetch processors
 * have run.
 * Modified by SVC to use Lucene 4.X
 * 
 * @author Kristinn Sigur&eth;sson
 * @author Søren Vejrup Carlsen
 * 
 * <bean id="DeDuplicator" class="is.hi.bok.deduplicator.DeDuplicator">
 * <!-- DEDUPLICATION_INDEX_LOCATION is replaced by path on harvest-server -->
 * <property name="indexLocation" value="/home/svc/dedupcrawllogindex/empty-cache"/> 
	<property name="matchingMethod" value="URL"/>  other option: DIGEST
    <property name="tryEquivalent" value="true"/> 
       <property name="changeContentSize" value="false"/>
        <property name="mimeFilter" value="^text/.*"/>
         
        <property name="filterMode" value="BLACKLIST"/> Other option:	 WHITELIST 
        <property name="analysisMode" value="TIMESTAMP"/> Other options: NONE, TIMESTAMP_AND_ETAG
        
        <property name="origin" value=""/>
        <property name="originHandling" value="INDEX"/> Other options: NONE,PROCESSOR
        <property name="statsPerHost" value="true"/>
        <property name="revisitInWarcs" value="true"/>

//          	/**
//					(FROM deduplicator-commons/src/main/java/is/landsbokasafn/deduplicator/IndexFields.java)
//        	       * These enums correspond to the names of fields in the Lucene index
//        	     */
//        	public enum IndexFields {
//        	    /** The URL 
//        	     *  This value is suitable for use in warc/revisit records as the WARC-Refers-To-Target-URI
//        	     **/
//        	        URL,
//        	    /** The content digest as String **/
//        	        DIGEST,
//        	    /** The URLs timestamp (time of fetch). Suitable for use in WARC-Refers-To-Date. Encoded according to
//        	     *  w3c-iso8601  
//        	     */
//        	    DATE,
//        	    /** The document's etag **/
//        	    ETAG,
//        	    /** A canonicalized version of the URL **/
//        	        URL_CANONICALIZED,
//        	    /** WARC Record ID of original payload capture. Suitable for WARC-Refers-To field. **/
//        	    ORIGINAL_RECORD_ID;
//
//        	}
 
@SuppressWarnings({"unchecked"})
public class DeDuplicator extends Processor implements InitializingBean {

    @Override public boolean getEnabled() {
        return super.getEnabled();
    }

    @Override public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
    }

    private static Logger logger =
        Logger.getLogger(DeDuplicator.class.getName());

    // Spring configurable parameters
    
    /* Location of Lucene Index to use for lookups */
    private final static String ATTR_INDEX_LOCATION = "index-location";

    public String getIndexLocation() {
        return (String) kp.get(ATTR_INDEX_LOCATION);
    }
    /** SETTER used by Spring */
    public void setIndexLocation(String indexLocation) {
        kp.put(ATTR_INDEX_LOCATION,indexLocation);
    }

    /* The matching method in use (by url or content digest) */
    private final static String ATTR_MATCHING_METHOD = "matching-method";
    
    public enum MatchingMethod {
    	URL,
    	DIGEST
    }
    
    private final static MatchingMethod DEFAULT_MATCHING_METHOD = MatchingMethod.URL; 
    {
        setMatchingMethod(DEFAULT_MATCHING_METHOD);
    }
    public MatchingMethod getMatchingMethod() {
        return (MatchingMethod) kp.get(ATTR_MATCHING_METHOD);
    }
    
    /** SETTER used by Spring */
    public void setMatchingMethod(MatchingMethod method) {
    	kp.put(ATTR_MATCHING_METHOD, method);
    }
    
    /* On duplicate, should jump to which part of processing chain? 
     *  If not set, nothing is skipped. Otherwise this should be the identity of the processor to jump to.
     */
    public final static String ATTR_JUMP_TO = "jump-to";
    public String getJumpTo(){
    	return (String)kp.get(ATTR_JUMP_TO);
    }
    /** SPRING SETTER. 
     * TODO Are we using this property??  The netarkivet are not
     */
    public void setJumpTo(String jumpTo){
    	kp.put(ATTR_JUMP_TO, jumpTo);
    }

    /* Origin of duplicate URLs. May be overridden by info from index */
    public final static String ATTR_ORIGIN = "origin";
    {
        setOrigin("");
    }
    public String getOrigin() {
        return (String) kp.get(ATTR_ORIGIN);
    }
    
    /** SPRING SETTER */
    public void setOrigin(String origin) {
        kp.put(ATTR_ORIGIN,origin);
    }

    /* If an exact match is not made, should the processor try 
     *  to find an equivalent match?  
     */
    public final static String ATTR_EQUIVALENT = "try-equivalent";
    {
    	setTryEquivalent(false);
    }
    public Boolean getTryEquivalent(){
    	return (Boolean)kp.get(ATTR_EQUIVALENT);
    }
    /** SPRING SETTER */
    public void setTryEquivalent(Boolean tryEquivalent){
    	kp.put(ATTR_EQUIVALENT, tryEquivalent);
    }

    /* The filter on mime types. This is either a blacklist or whitelist
     *  depending on ATTR_FILTER_MODE.
     */
    public final static String ATTR_MIME_FILTER = "mime-filter";
    public final static String DEFAULT_MIME_FILTER = "^text/.*";
    {
    	setMimeFilter(DEFAULT_MIME_FILTER);
    }
    public String getMimeFilter(){
    	return (String)kp.get(ATTR_MIME_FILTER);
    }
    // USED by SPRING
    public void setMimeFilter(String mimeFilter){
    	kp.put(ATTR_MIME_FILTER, mimeFilter);
    }

    /* Is the mime filter a blacklist (do not apply processor to what matches) 
     *  or whitelist (apply processor only to what matches).
     */
    public final static String ATTR_FILTER_MODE = "filter-mode";
    {
    	setfilterMode(FilterMode.BLACKLIST);
    }
    
    public FilterMode getFilterMode() {
    	return (FilterMode) kp.get(ATTR_FILTER_MODE);
    }
    
    
    public enum FilterMode {
    	BLACKLIST, WHITELIST
    };
    
    
    public Boolean getBlacklist(){
    	FilterMode fMode = (FilterMode) kp.get(ATTR_FILTER_MODE);
    	return fMode.equals(FilterMode.BLACKLIST);
    }
    /** SPRING SETTER method */
    public void setfilterMode(FilterMode filterMode){
    	kp.put(ATTR_FILTER_MODE, filterMode);
    }
   
    
    public enum AnalysisMode {
    	NONE, TIMESTAMP, TIMESTAMP_AND_ETAG
    };
    
    
    /* Analysis mode. */
    public final static String ATTR_ANALYZE_MODE = "analyze-modes";
    {
    	setAnalysisMode(AnalysisMode.TIMESTAMP);
    }
    
    public boolean getAnalyzeTimestamp() {
    	AnalysisMode analysisMode = (AnalysisMode) kp.get(ATTR_ANALYZE_MODE);
        return analysisMode.equals(AnalysisMode.TIMESTAMP);
    }
    
    public void setAnalysisMode(AnalysisMode analyzeMode) {	
		kp.put(ATTR_ANALYZE_MODE, analyzeMode);
    }
    
    public AnalysisMode getAnalysisMode()  {	
    	return (AnalysisMode) kp.get(ATTR_ANALYZE_MODE);
    }
    

    /* Should the content size information be set to zero when a duplicate is found? */
    public final static String ATTR_CHANGE_CONTENT_SIZE = "change-content-size";
    {
    	setChangeContentSize(false);
    }
    public Boolean getChangeContentSize(){
    	return (Boolean)kp.get(ATTR_CHANGE_CONTENT_SIZE);
    }
    /** SPRING SETTER */
    public void setChangeContentSize(Boolean changeContentSize){
    	kp.put(ATTR_CHANGE_CONTENT_SIZE, changeContentSize);
    }

    /* Should statistics be tracked per host? **/
    public final static String ATTR_STATS_PER_HOST = "stats-per-host";
    {
    	setStatsPerHost(false);
    }
    public Boolean getStatsPerHost(){
    	return (Boolean)kp.get(ATTR_STATS_PER_HOST);
    }
    public void setStatsPerHost(Boolean statsPerHost){
    	kp.put(ATTR_STATS_PER_HOST, statsPerHost);
    }
    
    /* How should 'origin' be handled */
    public final static String ATTR_ORIGIN_HANDLING = "origin-handling";
    public enum OriginHandling {
    	NONE,  		// No origin information
    	PROCESSOR,  // Use processor setting -- ATTR_ORIGIN
    	INDEX       // Use index information, each hit on index should contain origin
    }
    public final static OriginHandling DEFAULT_ORIGIN_HANDLING = OriginHandling.NONE;
    {
        setOriginHandling(DEFAULT_ORIGIN_HANDLING);
    }
    public OriginHandling getOriginHandling() {
        return (OriginHandling) kp.get(ATTR_ORIGIN_HANDLING);
    }
    public void setOriginHandling(OriginHandling originHandling) {
    	kp.put(ATTR_ORIGIN_HANDLING, originHandling);
    }

    public final static String ATTR_REVISIT_IN_WARCS = "revisit-in-warcs";
    {
    	setRevisitInWarcs(Boolean.TRUE); // the default is true
    }   
    
    public void setRevisitInWarcs(Boolean revisitOn) {
    	kp.put(ATTR_REVISIT_IN_WARCS, revisitOn);
	}
    public Boolean getRevisitInWarcs() {
        return (Boolean) kp.get(ATTR_REVISIT_IN_WARCS);
    }
    
    // Spring configured access to Heritrix resources
    
    // Gain access to the ServerCache for host based statistics.
    protected ServerCache serverCache;
    public ServerCache getServerCache() {
        return this.serverCache;
    }
    
	@Autowired
    public void setServerCache(ServerCache serverCache) {
        this.serverCache = serverCache;
    }

    
    // Member variables.
    protected IndexSearcher indexSearcher = null;
    protected IndexReader indexReader = null;
    
    
    protected boolean lookupByURL = true;
    protected boolean statsPerHost = false;
    
    
    protected boolean useOrigin = false;
    protected boolean useOriginFromIndex = false;

    protected Statistics stats = null;
    protected HashMap<String, Statistics> perHostStats = null;


    public void afterPropertiesSet() throws Exception {
        if (!getEnabled()) {
            logger.info(this.getClass().getName() + " disabled.");
            return;
        }
        // Index location
        String indexLocation = getIndexLocation();
        try {
        	FSDirectory indexDir = FSDirectory.open(new File(indexLocation));
            // https://issues.apache.org/jira/browse/LUCENE-1566
            // Reduce chunksize to avoid OOM to half the size of the default (=100 MB)
            int chunksize = indexDir.getReadChunkSize();
            indexDir.setReadChunkSize(chunksize / 2);
            indexReader = DirectoryReader.open(indexDir);
            indexSearcher = new IndexSearcher(indexReader);    
        } catch (Exception e) {
        	throw new IllegalArgumentException("Unable to find/open index at " + indexLocation,e);
        } 
        
        // Matching method
        MatchingMethod matchingMethod = getMatchingMethod();
        lookupByURL = matchingMethod == MatchingMethod.URL;

        // Track per host stats
        statsPerHost = getStatsPerHost();
        
        // Origin handling.
        OriginHandling originHandling = getOriginHandling();
        if (originHandling != OriginHandling.NONE) {
            useOrigin = true;
            logger.fine("Use origin");        
            if (originHandling == OriginHandling.INDEX) {
                useOriginFromIndex = true;
                logger.fine("Use origin from index");
            }
        }
        
        // Initialize some internal variables:
        stats = new Statistics();
        if (statsPerHost) {
            perHostStats = new HashMap<String, Statistics>();
        }
    }
    

	@Override
	protected boolean shouldProcess(CrawlURI curi) {
        if (!getEnabled()) {
            logger.finest("Not handling " + curi.toString() + ", deduplication disabled.");
            return false;
        }
        if (curi.isSuccess() == false) {
            // Early return. No point in doing comparison on failed downloads.
            logger.finest("Not handling " + curi.toString()
                    + ", did not succeed.");
            return false;
        }
        if (curi.isPrerequisite()) {
            // Early return. Prerequisites are exempt from checking.
            logger.finest("Not handling " + curi.toString()
                    + ", prerequisite.");
            return false;
        }
        if (curi.toString().startsWith("http")==false) {
            // Early return. Non-http documents are not handled at present
            logger.finest("Not handling " + curi.toString()
                        + ", non-http.");
            return false;
        }
        if(curi.getContentType() == null){
            // No content type means we can not handle it.
            logger.finest("Not handling " + curi.toString()
                    + ", missing content (mime) type");
            return false;
        }
        if(curi.getContentType().matches(getMimeFilter()) == getBlacklist()){
            // Early return. Does not pass the mime filter
            logger.finest("Not handling " + curi.toString()
                    + ", excluded by mimefilter (" + 
                    curi.getContentType() + ").");
            return false;
        }
        
        if(curi.isRevisit()){
            // A previous processor or filter has judged this CrawlURI to be a revisit
            logger.finest("Not handling " + curi.toString()
                    + ", already flagged as revisit.");
            return false;
        }
        return true;
	}

    @Override
    protected void innerProcess(CrawlURI puri) {
    	throw new AssertionError();
    }

    /**
     * Return date from 'date' field if date absent in 'origin' field or origin-field-absent
     * @param duplicate
     * @return
     */
    private String getRefersToDate(Document duplicate) {
    	String indexedDate = duplicate.get("date"); // DATE.name()
    	// look for the indexeddate of the revisit date in the origin "arcfile,offset,timestamp" 
    	String duplicateOrigin = duplicate.get(DigestIndexer.FIELD_ORIGIN);
    	if (duplicateOrigin != null && !duplicateOrigin.isEmpty()) {
    		String[] parts = duplicateOrigin.split(",");
    		if (parts.length == 3)  { // Detect new field-origin format 
    			indexedDate = parts[2];
    		}
    	}
    	Date readDate = null;
    	try {
    		readDate = ArchiveDateConverter.getHeritrixDateFormat().parse(indexedDate);
    	} catch (ParseException e) {
    		logger.warning("Unable to parse the indexed date '" + indexedDate 
    				+ "' as a 17-digit date: " + e); 
    	}
    	String refersToDateString = indexedDate;
    	if (readDate != null) {
    		refersToDateString = ArchiveDateConverter.getWarcDateFormat().format(readDate); 
    	}
    	return refersToDateString;
    }



	@Override
	protected ProcessResult innerProcessResult(CrawlURI curi) throws InterruptedException {

        ProcessResult processResult = ProcessResult.PROCEED; // Default. Continue as normal

        logger.finest("Processing " + curi.toString() + "(" + 
                curi.getContentType() + ")");

        stats.handledNumber++;
        stats.totalAmount += curi.getContentSize();
        Statistics currHostStats = null;
        if(statsPerHost){
            synchronized (perHostStats) {
                String host = getServerCache().getHostFor(curi.getUURI()).getHostName();
                currHostStats = perHostStats.get(host);
                if(currHostStats==null){
                    currHostStats = new Statistics();
                    perHostStats.put(host,currHostStats);
                }
            }
            currHostStats.handledNumber++;
            currHostStats.totalAmount += curi.getContentSize();
        }
        
        Document duplicate = null; 
        
        if(lookupByURL){
            duplicate = lookupByURL(curi,currHostStats);
        } else {
            duplicate = lookupByDigest(curi,currHostStats);
        }

        if (duplicate != null){
            // Perform tasks common to when a duplicate is found.

        	//// Code taken from LuceneIndexSearcher.wrap() method //////////////////////////
        	IdenticalPayloadDigestRevisit duplicateRevisit = new IdenticalPayloadDigestRevisit(
        			duplicate.get("digest")); //DIGEST.name()));

        	duplicateRevisit.setRefersToTargetURI(
        			duplicate.get("url"));  // URL.name()

        	
        	duplicateRevisit.setRefersToDate(getRefersToDate(duplicate));
        			
        	
        	//Check if the record ID information is available in the index.
        	// This requires that record information is available during indexing
        	String refersToRecordID = duplicate.get("orig_record_id"); // ORIGINAL_RECORD_ID.name()); 
        
        	if (refersToRecordID!=null && !refersToRecordID.isEmpty()) {
        		duplicateRevisit.setRefersToRecordID(refersToRecordID);
        	}        	


            // Increment statistics counters
            stats.duplicateAmount += curi.getContentSize();
            stats.duplicateNumber++;
            if(statsPerHost){ 
                currHostStats.duplicateAmount+=curi.getContentSize();
                currHostStats.duplicateNumber++;
            }

            String jumpTo = getJumpTo(); 
            // Duplicate. Skip part of processing chain?
            if(jumpTo!=null){
            	processResult = ProcessResult.jump(jumpTo);
            } 
            
            // Record origin?
            String annotation = "duplicate";
            if(useOrigin){
                // TODO: Save origin in the CrawlURI so that other processors
                //       can make use of it. (Future: WARC)
                if(useOriginFromIndex && 
                    duplicate.get(DigestIndexer.FIELD_ORIGIN)!=null){
                    // Index contains origin, use it.
                    annotation += ":\"" + duplicate.get(DigestIndexer.FIELD_ORIGIN) + "\""; // If 
                } else {
                    String tmp = getOrigin();
                    // Check if an origin value is actually available
                    if(tmp != null && tmp.trim().length() > 0){
                        // It is available, add it to the log line.
                        annotation += ":\"" + tmp + "\""; 
                    }
                }
            } 
            // Make duplicate-note in crawl-log
            curi.getAnnotations().add(annotation);
            // Notify Heritrix that this is a revisit if we want revisit records to be written
            if (getRevisitInWarcs()) {
            	curi.setRevisitProfile(duplicateRevisit);
            }
            
            /* TODO enable this when moving to indexing based on this data
            // Add annotation to crawl.log 
            curi.getAnnotations().add(REVISIT_ANNOTATION_MARKER);
                        
            // Write extra logging information (needs to be enabled in CrawlerLoggerModule)
            curi.addExtraInfo(EXTRA_REVISIT_PROFILE, duplicateRevisit.getProfileName());
            curi.addExtraInfo(EXTRA_REVISIT_URI, duplicateRevisit.getRefersToTargetURI());
            curi.addExtraInfo(EXTRA_REVISIT_DATE, duplicateRevisit.getRefersToDate());
            */
            
        }
        if(getAnalyzeTimestamp()){
            doAnalysis(curi,currHostStats, duplicate!=null);
        }
        return processResult;
	}

	/**
     * Process a CrawlURI looking up in the index by URL
     *
     * @param curi The CrawlURI to process
     * @param currHostStats A statistics object for the current host. If per host statistics tracking is enabled this
     * must be non null and the method will increment appropriate counters on it.
     * @return The result of the lookup (a Lucene document). If a duplicate is not found null is returned.
     */
    protected Document lookupByURL(CrawlURI curi, Statistics currHostStats) {
        // Look the CrawlURI's URL up in the index.
        try {
            Query query = queryField(DigestIndexer.FIELD_URL, curi.toString());
            AllDocsCollector collectAllCollector = new AllDocsCollector();
            indexSearcher.search(query, collectAllCollector);

            List<ScoreDoc> hits = collectAllCollector.getHits();
            Document doc = null;
            String currentDigest = getDigestAsString(curi);
            if (hits != null && hits.size() > 0) {
                // Typically there should only be one it, but we'll allow for
                // multiple hits.
                for (ScoreDoc hit : hits) {
                    // for(int i=0 ; i < hits.size() ; i++){
                    // Multiple hits on same exact URL should be rare
                    // See if any have matching content digests
                    int docId = hit.doc;
                    doc = indexSearcher.doc(docId);
                    String oldDigest = doc.get(DigestIndexer.FIELD_DIGEST);

                    if (oldDigest.equalsIgnoreCase(currentDigest)) {
                        stats.exactURLDuplicates++;
                        if (statsPerHost) {
                            currHostStats.exactURLDuplicates++;
                        }

                        logger.finest("Found exact match for " + curi.toString());

                        // If we found a hit, no need to look at other hits.
                        return doc;
                    }
                }
            }
            if (getTryEquivalent()) {
                // No exact hits. Let's try lenient matching.
                String normalizedURL = DigestIndexer.stripURL(curi.toString());
                query = queryField(DigestIndexer.FIELD_URL_NORMALIZED, normalizedURL);
                collectAllCollector.reset(); // reset collector
                indexSearcher.search(query, collectAllCollector);
                hits = collectAllCollector.getHits();

                for (ScoreDoc hit : hits) {
                    // int i=0 ; i < hits.length ; i++){

                    int docId = hit.doc;
                    Document doc1 = indexSearcher.doc(docId);
                    String indexDigest = doc1.get(DigestIndexer.FIELD_DIGEST);
                    if (indexDigest.equals(currentDigest)) {
                        // Make note in log
                        String equivURL = doc1.get(DigestIndexer.FIELD_URL);
                        curi.getAnnotations().add("equivalentURL:\"" + equivURL + "\"");
                        // Increment statistics counters
                        stats.equivalentURLDuplicates++;
                        if (statsPerHost) {
                            currHostStats.equivalentURLDuplicates++;
                        }
                        logger.finest("Found equivalent match for " + curi.toString() + ". Normalized: "
                                + normalizedURL + ". Equivalent to: " + equivURL);

                        // If we found a hit, no need to look at more.
                        return doc1;
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error accessing index.", e);
        }
        // If we make it here then this is not a duplicate.
        return null;
    }
    
    /**
     * Process a CrawlURI looking up in the index by content digest
     *
     * @param curi The CrawlURI to process
     * @param currHostStats A statistics object for the current host. If per host statistics tracking is enabled this
     * must be non null and the method will increment appropriate counters on it.
     * @return The result of the lookup (a Lucene document). If a duplicate is not found null is returned.
     */
    protected Document lookupByDigest(CrawlURI curi, Statistics currHostStats) {
        Document duplicate = null;
        String currentDigest = null;
        Object digest = curi.getContentDigest();
        if (digest != null) {
            currentDigest = Base32.encode((byte[]) digest);
        } else {
            logger.warning("Digest received from CrawlURI is null. Null Document returned");
            return null;
        }

        Query query = queryField(DigestIndexer.FIELD_DIGEST, currentDigest);
        try {
            AllDocsCollector collectAllCollector = new AllDocsCollector();
            indexSearcher.search(query, collectAllCollector);

            List<ScoreDoc> hits = collectAllCollector.getHits();

            StringBuffer mirrors = new StringBuffer();
            mirrors.append("mirrors: ");
            if (hits != null && hits.size() > 0) {
                // Can definitely be more then one
                // Note: We may find an equivalent match before we find an
                // (existing) exact match.
                // TODO: Ensure that an exact match is recorded if it exists.
                Iterator<ScoreDoc> hitsIterator = hits.iterator();
                while (hitsIterator.hasNext() && duplicate == null) {
                    ScoreDoc hit = hitsIterator.next();
                    int docId = hit.doc;
                    Document doc = indexSearcher.doc(docId);
                    String indexURL = doc.get(DigestIndexer.FIELD_URL);
                    // See if the current hit is an exact match.
                    if (curi.toString().equals(indexURL)) {
                        duplicate = doc;
                        stats.exactURLDuplicates++;
                        if (statsPerHost) {
                            currHostStats.exactURLDuplicates++;
                        }
                        logger.finest("Found exact match for " + curi.toString());
                    }

                    // If not, then check if it is an equivalent match (if
                    // equivalent matches are allowed).
                    if (duplicate == null && getTryEquivalent()) {
                        String normalURL = DigestIndexer.stripURL(curi.toString());
                        String indexNormalURL = doc.get(DigestIndexer.FIELD_URL_NORMALIZED);
                        if (normalURL.equals(indexNormalURL)) {
                            duplicate = doc;
                            stats.equivalentURLDuplicates++;
                            if (statsPerHost) {
                                currHostStats.equivalentURLDuplicates++;
                            }
                            curi.getAnnotations().add("equivalentURL:\"" + indexURL + "\"");
                            logger.finest("Found equivalent match for " + curi.toString() + ". Normalized: "
                                    + normalURL + ". Equivalent to: " + indexURL);
                        }
                    }

                    if (duplicate == null) {
                        // Will only be used if no exact (or equivalent) match
                        // is found.
                        mirrors.append(indexURL + " ");
                    }
                }
                if (duplicate == null) {
                    stats.mirrorNumber++;
                    if (statsPerHost) {
                        currHostStats.mirrorNumber++;
                    }
                    logger.log(Level.FINEST, "Found mirror URLs for " + curi.toString() + ". " + mirrors);
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error accessing index.", e);
        }
        return duplicate;
    }

    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: is.hi.bok.digest.DeDuplicator\n");
        ret.append("  Function:          Abort processing of duplicate records\n");
        if (!getEnabled()) {
            ret.append("Processor is disabled by configuration");
            ret.append("\n");
            return ret.toString();
        }
        ret.append("                     - Lookup by " + 
        		(lookupByURL?"url":"digest") + " in use\n");
        ret.append("  Total handled:     " + stats.handledNumber + "\n");
        ret.append("  Duplicates found:  " + stats.duplicateNumber + " " + 
        		getPercentage(stats.duplicateNumber,stats.handledNumber) + "\n");
        ret.append("  Bytes total:       " + stats.totalAmount + " (" + 
        		ArchiveUtils.formatBytesForDisplay(stats.totalAmount) + ")\n");
        ret.append("  Bytes discarded:   " + stats.duplicateAmount + " (" + 
        		ArchiveUtils.formatBytesForDisplay(stats.duplicateAmount) + ") " + 
        		getPercentage(stats.duplicateAmount, stats.totalAmount) + "\n");
        
    	ret.append("  New (no hits):     " + (stats.handledNumber-
    			(stats.mirrorNumber+stats.exactURLDuplicates+stats.equivalentURLDuplicates)) + "\n");
    	ret.append("  Exact hits:        " + stats.exactURLDuplicates + "\n");
    	ret.append("  Equivalent hits:   " + stats.equivalentURLDuplicates + "\n");
        if(lookupByURL==false){
        	ret.append("  Mirror hits:       " + stats.mirrorNumber + "\n");
        }
        
        if(getAnalyzeTimestamp()){
        	ret.append("  Timestamp predicts: (Where exact URL existed in the index)\n");
        	ret.append("  Change correctly:  " + stats.timestampChangeCorrect + "\n");
        	ret.append("  Change falsely:     " + stats.timestampChangeFalse + "\n");
        	ret.append("  Non-change correct:" + stats.timestampNoChangeCorrect + "\n");
        	ret.append("  Non-change falsely: " + stats.timestampNoChangeFalse + "\n");
        	ret.append("  Missing timpestamp:" + stats.timestampMissing + "\n");
        	
        }
        
        if(statsPerHost){
            ret.append("  [Host] [total] [duplicates] [bytes] " +
                    "[bytes discarded] [new] [exact] [equiv]");
            if(lookupByURL==false){
                ret.append(" [mirror]");
            }
            if(getAnalyzeTimestamp()){
                ret.append(" [change correct] [change falsely]");
                ret.append(" [non-change correct] [non-change falsely]");
                ret.append(" [no timestamp]");
            }
            ret.append("\n");
            synchronized (perHostStats) {
                Iterator<String> it = perHostStats.keySet().iterator();
                while(it.hasNext()){
                    String key = it.next();
                    Statistics curr = perHostStats.get(key);
                    ret.append("  " +key);
                    ret.append(" ");
                    ret.append(curr.handledNumber);
                    ret.append(" ");
                    ret.append(curr.duplicateNumber);
                    ret.append(" ");
                    ret.append(curr.totalAmount);
                    ret.append(" ");
                    ret.append(curr.duplicateAmount);
                    ret.append(" ");
                    ret.append(curr.handledNumber-
                            (curr.mirrorNumber+
                             curr.exactURLDuplicates+
                             curr.equivalentURLDuplicates));
                    ret.append(" ");
                    ret.append(curr.exactURLDuplicates);
                    ret.append(" ");
                    ret.append(curr.equivalentURLDuplicates);

                    if(lookupByURL==false){
                        ret.append(" ");
                        ret.append(curr.mirrorNumber);
                    }    
                    if(getAnalyzeTimestamp()){
                        ret.append(" ");
                        ret.append(curr.timestampChangeCorrect);
                        ret.append(" ");
                        ret.append(curr.timestampChangeFalse);
                        ret.append(" ");
                        ret.append(curr.timestampNoChangeCorrect);
                        ret.append(" ");
                        ret.append(curr.timestampNoChangeFalse);
                        ret.append(" ");
                        ret.append(curr.timestampMissing);
                    }
                    ret.append("\n");
                }
            }
        }
        
        ret.append("\n");
        return ret.toString();
	}
	
	protected static String getPercentage(double portion, double total){
		double value = portion / total;
		value = value*100;
		String ret = Double.toString(value);
		int dot = ret.indexOf('.');
		if(dot+3<ret.length()){
			ret = ret.substring(0,dot+3);
		}
		return ret + "%";
	}
	
	private static String getDigestAsString(CrawlURI curi){
        // The CrawlURI now has a method for this. For backwards
		// compatibility with older Heritrix versions that is not used.
		Object digest = curi.getContentDigest();
        if (digest != null) {
            return Base32.encode((byte[])digest);
        }
        return null;
	}
	

	protected void doAnalysis(CrawlURI curi, Statistics currHostStats,
            boolean isDuplicate) {
		try{
    		Query query = queryField(DigestIndexer.FIELD_URL, curi.toString());
            AllDocsCollector collectAllCollector = new AllDocsCollector();
			indexSearcher.search(query, collectAllCollector);
            List<ScoreDoc> hits = collectAllCollector.getHits();

            Document doc = null;
    	
            if(hits != null && hits.size() > 0){
                // If there are multiple hits, use the one with the most
                // recent date.
                Document docToEval = null;
                for (ScoreDoc hit : hits) {
                    int docId = hit.doc;
                    doc = indexSearcher.doc(docId);
                    // The format of the timestamp ("yyyyMMddHHmmssSSS") allows
                    // us to do a greater then (later) or lesser than (earlier)
                    // comparison of the strings.
                    String timestamp = doc.get(DigestIndexer.FIELD_TIMESTAMP);
                    if (docToEval == null || docToEval.get(DigestIndexer.FIELD_TIMESTAMP).compareTo(timestamp) > 0) {
                        // Found a more recent hit.
                        docToEval = doc;
                    }
                }
                doTimestampAnalysis(curi,docToEval, currHostStats, isDuplicate);
    		}
        } catch(IOException e){
            logger.log(Level.SEVERE,"Error accessing index.",e);
        }
	}
	
		
	protected void doTimestampAnalysis(CrawlURI curi, Document urlHit, 
            Statistics currHostStats, boolean isDuplicate){
        
        //HttpMethod method = curi.getHttpMethod();

        // Compare datestamps (last-modified versus the indexed date)
        Date lastModified = null;
        if (curi.getHttpResponseHeader("last-modified") != null) {
            SimpleDateFormat sdf = 
            	new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", 
                        Locale.ENGLISH);
            try {
				lastModified = sdf.parse(
						curi.getHttpResponseHeader("last-modified"));			// .getValue()
			} catch (ParseException e) {
				logger.log(Level.INFO,"Exception parsing last modified of " + 
						curi.toString(),e);
				return;
			}
        } else {
            stats.timestampMissing++;
            if (statsPerHost) {
                currHostStats.timestampMissing++;
                logger.finest("Missing timestamp on " + curi.toString());
            }
        	return;
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        Date lastFetch = null;
        try {
			lastFetch = sdf.parse(
					urlHit.get(DigestIndexer.FIELD_TIMESTAMP));
		} catch (ParseException e) {
			logger.log(Level.WARNING,"Exception parsing indexed date for " + 
					urlHit.get(DigestIndexer.FIELD_URL),e);
			return;
		}

		if(lastModified.after(lastFetch)){
			// Header predicts change
			if(isDuplicate){
				// But the DeDuplicator did not notice a change.
                stats.timestampChangeFalse++;
                if (statsPerHost){
                    currHostStats.timestampChangeFalse++;
                }
                logger.finest("Last-modified falsly predicts change on " + 
                        curi.toString());
			} else {
                stats.timestampChangeCorrect++;
                if (statsPerHost){
                    currHostStats.timestampChangeCorrect++;
                }
                logger.finest("Last-modified correctly predicts change on " + 
                        curi.toString());
			}
		} else {
			// Header does not predict change.
			if(isDuplicate){
				// And the DeDuplicator verifies that no change had occurred
                stats.timestampNoChangeCorrect++;
                if (statsPerHost){
                    currHostStats.timestampNoChangeCorrect++;
                }
                logger.finest("Last-modified correctly predicts no-change on " + 
                        curi.toString());
			} else {
				// As this is particularly bad we'll log the URL at INFO level
				logger.log(Level.INFO,"Last-modified incorrectly indicated " +
						"no-change on " + curi.toString() + " " + 
						curi.getContentType() + ". last-modified: " + 
                        lastModified + ". Last fetched: " + lastFetch);
                stats.timestampNoChangeFalse++;
                if (statsPerHost){
                    currHostStats.timestampNoChangeFalse++;
                }
			}
		}
        
	}

    /** Run a simple Lucene query for a single term in a single field.
     *
     * @param fieldName name of the field to look in.
     * @param value The value to query for
     * @return A Query for the given value in the given field.
     */
	protected Query queryField(String fieldName, String value) {
		Query query = null;

		/** alternate solution. */
		BytesRef valueRef = new BytesRef(value.getBytes());
		query = new ConstantScoreQuery(new TermRangeFilter(fieldName, valueRef, valueRef, true, true));

		/** The most clean solution, but it seems also memory demanding */
		// query = new ConstantScoreQuery(new FieldCacheTermsFilter(fieldName,
		// value));
		return query; 	
	}
	
}

class Statistics{
    // General statistics
    
    /** Number of URIs that make it through the processors exclusion rules
     *  and are processed by it.
     */
    long handledNumber = 0;
    
    /** Number of URIs that are deemed duplicates and further processing is
     *  aborted
     */
    long duplicateNumber = 0;
    
    /** Then number of URIs that turned out to have exact URL and content 
     *  digest matches.
     */
    long exactURLDuplicates = 0;
    
    /** The number of URIs that turned out to have equivalent URL and content
     *  digest matches.
     */
    long equivalentURLDuplicates = 0;
    
    /** The number of URIs that, while having no exact or equivalent matches,  
     *  do have exact content digest matches against non-equivalent URIs.
     */
    long mirrorNumber = 0;
    
    /** The total amount of data represented by the documents who were deemed
     *  duplicates and excluded from further processing.
     */
    long duplicateAmount = 0;
    
    /** The total amount of data represented by all the documents processed **/
    long totalAmount = 0;
    
    // Timestamp analysis
    
    long timestampChangeCorrect = 0;
    long timestampChangeFalse = 0;
    long timestampNoChangeCorrect = 0;
    long timestampNoChangeFalse = 0;
    long timestampMissing = 0;

    // ETag analysis;
    
    long ETagChangeCorrect = 0;
    long ETagChangeFalse = 0;
    long ETagNoChangeCorrect = 0;
    long ETagNoChangeFalse = 0;
    long ETagMissingIndex = 0;
    long ETagMissingCURI = 0;
}

