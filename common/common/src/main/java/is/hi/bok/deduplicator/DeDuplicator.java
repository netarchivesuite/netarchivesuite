/* DeDuplicator
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.FieldCacheTermsFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.frontier.AdaptiveRevisitAttributeConstants;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;
import org.archive.util.ArchiveUtils;
import org.archive.util.Base32;

/**
 * Heritrix compatible processor.
 * <p>
 * Will abort the processing (skip to post processor chain) of CrawlURIs that 
 * are deemed <i>duplicates</i>.
 * <p>
 * Duplicate detection can only be performed <i>after</i> the fetch processors
 * have run.
 * 
 * @author Kristinn Sigur&eth;sson
 */
public class DeDuplicator extends Processor 
implements AdaptiveRevisitAttributeConstants{

    private static final long serialVersionUID =
        ArchiveUtils.classnameBasedUID(DeDuplicator.class,1);

    private static Logger logger =
        Logger.getLogger(DeDuplicator.class.getName());
	
    protected IndexSearcher index = null;
    protected IndexReader indexReader = null;
    protected boolean lookupByURL = true;
    protected boolean equivalent = DEFAULT_EQUIVALENT.booleanValue();
    protected String mimefilter = DEFAULT_MIME_FILTER;
    protected boolean blacklist = true;
    protected boolean doTimestampAnalysis = false;
    protected boolean doETagAnalysis = false;
    protected boolean statsPerHost = DEFAULT_STATS_PER_HOST.booleanValue();
    protected boolean changeContentSize = 
        DEFAULT_CHANGE_CONTENT_SIZE.booleanValue();
    protected boolean useOrigin = false;
    protected boolean useOriginFromIndex = false;
    protected boolean useSparseRangeFilter = DEFAULT_USE_SPARSE_RANGE_FILTER;

    protected Statistics stats = null;
    protected HashMap<String, Statistics> perHostStats = null;
    protected boolean skipWriting = DEFAULT_SKIP_WRITE.booleanValue();

    /* Configurable parameters
     * - Index location
     * - Matching mode (By URL (default) or By Content Digest)
     * - Try equivalent matches
     * - Mime filter
     * - Filter mode (blacklist (default) or whitelist)
     * - Analysis (None (default), Timestamp only or Timestamp and ETag)
     * - Log level
     * - Track per host stats
     * - Origin
     * - Skip writing
     */
    /** Location of Lucene Index to use for lookups */
    public final static String ATTR_INDEX_LOCATION = "index-location";
    public final static String DEFAULT_INDEX_LOCATION = "";

    /** The matching method in use (by url or content digest) */
    public final static String ATTR_MATCHING_METHOD = "matching-method";
    public final static String[] AVAILABLE_MATCHING_METHODS = {
    	"By URL",
    	"By content digest"
    };
    public final static String DEFAULT_MATCHING_METHOD = 
        AVAILABLE_MATCHING_METHODS[0];
    
    /** If an exact match is not made, should the processor try 
     *  to find an equivalent match?  
     **/
    public final static String ATTR_EQUIVALENT = "try-equivalent";
    public final static Boolean DEFAULT_EQUIVALENT = new Boolean(false);

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
    public final static String DEFAULT_FILTER_MODE = 
        AVAILABLE_FILTER_MODES[0];
    
    /** Set analysis mode. */
    public final static String ATTR_ANALYSIS_MODE = "analysis-mode";
    public final static String[] AVAILABLE_ANALYSIS_MODES = {
        "None",
        "Timestamp",
        "Timestamp and ETag"
    };
    public final static String DEFAULT_ANALYSIS_MODE = 
        AVAILABLE_ANALYSIS_MODES[0];
    
    /** Should the content size information be set to zero when a duplicate is 
     *  found?
     */
    public final static String ATTR_CHANGE_CONTENT_SIZE = "change-content-size";
    public final static Boolean DEFAULT_CHANGE_CONTENT_SIZE = 
        new Boolean(true);

    /** What to write to a log file */
    public final static String ATTR_LOG_LEVEL = "log-level";
    public final static String[] AVAILABLE_LOG_LEVELS = {
        Level.SEVERE.toString(),
        Level.INFO.toString(),
        Level.FINEST.toString()
    };
    public final static String DEFAULT_LOG_LEVEL = AVAILABLE_LOG_LEVELS[0];

    /** Should statistics be tracked per host? **/
    public final static String ATTR_STATS_PER_HOST = "stats-per-host";
    public final static Boolean DEFAULT_STATS_PER_HOST = new Boolean(false);
    
    /** How should 'origin' be handled **/
    public final static String ATTR_ORIGIN_HANDLING = "origin-handling";
    public final static String ORIGIN_HANDLING_NONE = 
        "No origin information";
    public final static String ORIGIN_HANDLING_PROCESSOR = 
        "Use processor setting";
    public final static String ORIGIN_HANDLING_INDEX = 
        "Use index information";
    public final static String[] AVAILABLE_ORIGIN_HANDLING = {
        ORIGIN_HANDLING_NONE,
        ORIGIN_HANDLING_PROCESSOR,
        ORIGIN_HANDLING_INDEX
    };
    public final static String DEFAULT_ORIGIN_HANDLING = 
        ORIGIN_HANDLING_NONE;
    
    /** Origin of duplicate URLs **/
    public final static String ATTR_ORIGIN = "origin";
    public final static String DEFAULT_ORIGIN = "";

    /** Should the writer processor chain be skipped? **/
    public final static String ATTR_SKIP_WRITE = "skip-writing";
    public final static Boolean DEFAULT_SKIP_WRITE = new Boolean(true);
    
    /** Should we use sparse queries (uses less memory at a cost to performance? **/
    public final static String ATTR_USE_SPARSE_RANGE_FILTER = "use-sparse-range-filter";
    public final static Boolean DEFAULT_USE_SPARSE_RANGE_FILTER = new Boolean(false);
    
	public DeDuplicator(String name) {
		super(name, "Aborts the processing of URIs (skips to post processing " +
                "chain) if a duplicate is found in the specified index. " +
                "Note that any changes made to this processors configuration " +
                "at run time will be ignored unless otherwise stated.");
		Type t = new SimpleType(
				ATTR_INDEX_LOCATION,
				"Location of index (full path). Can not be changed at run " +
				"time.",
				DEFAULT_INDEX_LOCATION);
		t.setOverrideable(false);
		addElementToDefinition(t);
		t = new SimpleType(
				ATTR_MATCHING_METHOD,
				"Select if we should lookup by URL " +
				"or by content digest (counts mirror matches).",
				DEFAULT_MATCHING_METHOD,AVAILABLE_MATCHING_METHODS);
		t.setOverrideable(false);
		addElementToDefinition(t);
        t = new SimpleType(
                ATTR_EQUIVALENT,
                "If an exact match of URI and content digest is not found " +
                "then an equivalent URI (i.e. one with any www[0-9]*, " +
                "trailing slashes and parameters removed) can be checked. " +
                "If an equivalent URI has an identical content digest then " +
                "enabling this feature will cause the processor to consider " +
                "this a duplicate. Equivalent matches are noted in the " +
                "crawl log and their number is tracked seperately.",
                DEFAULT_EQUIVALENT);
        t.setOverrideable(false);
        addElementToDefinition(t);
        t = new SimpleType(
				ATTR_MIME_FILTER,
				"A regular expression that the mimetype of all documents " +
                "will be compared against. \nIf the attribute filter-mode is " +
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
                ATTR_ANALYSIS_MODE,
                "If enabled, the processor can analyse the timestamp (last-" +
                "modified) and ETag info of the HTTP headers and compare " +
                "their predictions as to whether or not the document had " +
                "changed against the result of the index lookup. This is " +
                "ONLY for the purpose of gathering statistics about the " +
                "usefulness and accuracy of the HTTP header information in " +
                "question and has no effect on the processing of documents. " +
                "Analysis is only possible if " +
                "the relevant data was included in the index.",
                DEFAULT_ANALYSIS_MODE,AVAILABLE_ANALYSIS_MODES);
        t.setOverrideable(false);
        t.setExpertSetting(true);
        addElementToDefinition(t);
        
        t = new SimpleType(
                ATTR_LOG_LEVEL,
                "Adjust the verbosity of the processor. By default, it only " +
                "reports serious (Java runtime) errors. " +
                "By setting the log level " +
                "higher, various additional data can be logged. " +
                "* Serious - Default logging level, only serious errors. " +
                "Note that it is possible that a more permissive default " +
                "logging level has been set via the heritrix.properties " +
                "file. This setting (severe) will not affect that.\n" +
                "* Info - Records some anomalies. Such as the information " +
                "on URIs that the HTTP header info falsely predicts " +
                "no-change on.\n" +
                "* Finest - Full logging of all URIs processed. For " +
                "debugging purposes only!",
                DEFAULT_LOG_LEVEL,AVAILABLE_LOG_LEVELS);
        t.setOverrideable(false);
        t.setExpertSetting(true);
        addElementToDefinition(t);
        t = new SimpleType(
                ATTR_STATS_PER_HOST,
                "If enabled the processor will keep track of the number of " +
                "processed uris, duplicates found etc. per host. The listing " +
                "will be added to the processor report (not the host-report).",
                DEFAULT_STATS_PER_HOST);
        t.setOverrideable(false);
        t.setExpertSetting(true);
        addElementToDefinition(t);
        t = new SimpleType(
                ATTR_CHANGE_CONTENT_SIZE,
                "If set to true then the processor will set the content size " +
                "of the CrawlURI to zero when a duplicate is discovered. ",
                DEFAULT_CHANGE_CONTENT_SIZE);
        t.setOverrideable(false);
        addElementToDefinition(t);
        
        t = new SimpleType(
                ATTR_ORIGIN_HANDLING,
                "The origin of duplicate URLs can be handled a few different " +
                "ways. It is important to note that the 'origin' information " +
                "is malleable and may be anything from a ARC name and offset " +
                "to a simple ID of a particular crawl. It is entirely at the " +
                "operators discretion.\n " +
                ORIGIN_HANDLING_NONE + " - No origin information is " +
                "associated with the URLs.\n " +
                ORIGIN_HANDLING_PROCESSOR + " - Duplicate URLs are all given " +
                "the same origin, specified by the 'origin' setting of this " +
                "processor.\n " +
                ORIGIN_HANDLING_INDEX + " - The origin of each duplicate URL " +
                "is read from the index. If the index does not contain any " +
                "origin information for an URL, the processor setting is " +
                "used as a fallback!",
                DEFAULT_ORIGIN_HANDLING,AVAILABLE_ORIGIN_HANDLING);
        t.setOverrideable(false);
        addElementToDefinition(t);
        
        t = new SimpleType(
                ATTR_ORIGIN,
                "The origin of duplicate URLs.",
                DEFAULT_ORIGIN);
        addElementToDefinition(t);

        t = new SimpleType(
                ATTR_SKIP_WRITE,
                "If set to true, then processing of duplicate URIs will be " +
                "skipped directly to the post processing chain. If false, " +
                "processing of duplicates will skip directly to the writer " +
                "chain that precedes the post processing chain.",
                DEFAULT_SKIP_WRITE);
        t.setOverrideable(true);
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
	
    /*
     *  (non-Javadoc)
     * @see org.archive.crawler.framework.Processor#initialTasks()
     */
    protected void initialTasks() {
        // Read settings and set appropriate class variables.
        
        // Index location
        String indexLocation = (String)readAttribute(ATTR_INDEX_LOCATION,"");
        try {
            Directory indexDir = new MMapDirectory(new File(indexLocation));
            IndexReader reader = IndexReader.open(indexDir);
            index = new IndexSearcher(reader);
        } catch (Exception e) {
            logger.log(Level.SEVERE,"Unable to find/open index.",e);
        } 
        
        // Matching method
        String matchingMethod = (String)readAttribute(
                ATTR_MATCHING_METHOD,DEFAULT_MATCHING_METHOD);
        lookupByURL = matchingMethod.equals(DEFAULT_MATCHING_METHOD);

        // Try equivalent matches
        equivalent = ((Boolean)readAttribute(
                ATTR_EQUIVALENT,DEFAULT_EQUIVALENT)).booleanValue();
        
        // Mime filter
        mimefilter = (String)readAttribute(
                ATTR_MIME_FILTER,DEFAULT_MIME_FILTER);
        
        // Filter mode (blacklist (default) or whitelist)
        blacklist = ((String)readAttribute(
                ATTR_FILTER_MODE,DEFAULT_FILTER_MODE)).equals(
                        DEFAULT_FILTER_MODE);

        // Analysis (None (default), Timestamp only or Timestamp and ETag)
        String analysisMode = (String)readAttribute(
                ATTR_ANALYSIS_MODE, DEFAULT_ANALYSIS_MODE);
        if (analysisMode.equals(AVAILABLE_ANALYSIS_MODES[1])){
            // Timestamp only
            doTimestampAnalysis = true;
        } else if (analysisMode.equals(AVAILABLE_ANALYSIS_MODES[2])){
            // Both timestamp and ETag
            doTimestampAnalysis = true;
            doETagAnalysis = true;
        }
        
        // Log file/level
        String lev = (String)readAttribute(ATTR_LOG_LEVEL,DEFAULT_LOG_LEVEL);
        if(lev.equals(Level.FINEST.toString())){
            logger.setLevel(Level.FINEST);
        } else if(lev.equals(Level.INFO.toString())){
            logger.setLevel(Level.INFO);
        } // Severe effectively means default level.  

        
        // Track per host stats
        statsPerHost = ((Boolean)readAttribute(
                ATTR_STATS_PER_HOST,DEFAULT_STATS_PER_HOST)).booleanValue();
        
        // Change content size
        changeContentSize = ((Boolean)readAttribute(
                ATTR_CHANGE_CONTENT_SIZE,
                DEFAULT_CHANGE_CONTENT_SIZE)).booleanValue();
        
        // Origin handling.
        String originHandling = (String)readAttribute(
                ATTR_ORIGIN_HANDLING,DEFAULT_ORIGIN_HANDLING);
        if(originHandling.equals(ORIGIN_HANDLING_NONE)==false){
            useOrigin = true;
            if(originHandling.equals(ORIGIN_HANDLING_INDEX)){
                useOriginFromIndex = true;
            }
        }
        
        // Range Filter type
        useSparseRangeFilter = ((Boolean)readAttribute(
                ATTR_USE_SPARSE_RANGE_FILTER,
                DEFAULT_USE_SPARSE_RANGE_FILTER)).booleanValue();
        
        // Initialize some internal variables:
        stats = new Statistics();
        if (statsPerHost) {
            perHostStats = new HashMap<String, Statistics>();
        }
    }
    
    /**
     * A utility method for reading attributes. If not found, an error is logged
     * and the defaultValue is returned.
     * @param name The name of the attribute
     * @param defaultValue A default value to return if an error occurs
     * @return The value of the attribute or the default value if an error
     *         occurs
     */
    protected Object readAttribute(String name, Object defaultValue){
        try {
            return getAttribute(name);
        } catch (Exception e) {
            logger.log(Level.SEVERE,"Unable read " + name + 
                    " attribute",e);
            return defaultValue;
        } 
    }

	protected void innerProcess(CrawlURI curi) throws InterruptedException {
        if (curi.isSuccess() == false) {
            // Early return. No point in doing comparison on failed downloads.
            logger.finest("Not handling " + curi.toString()
                    + ", did not succeed.");
            return;
        }
        if (curi.isPrerequisite()) {
            // Early return. Prerequisites are exempt from checking.
            logger.finest("Not handling " + curi.toString()
                    + ", prerequisite.");
            return;
        }
        if (curi.isSuccess() == false 
                || curi.isPrerequisite() 
                || curi.toString().startsWith("http")==false) {
            // Early return. Non-http documents are not handled at present
            logger.finest("Not handling " + curi.toString()
                        + ", non-http.");
            return;
        }
        if(curi.getContentType() == null){
            // No content type means we can not handle it.
            logger.finest("Not handling " + curi.toString()
                    + ", missing content (mime) type");
            return;
        }
        if(curi.getContentType().matches(mimefilter) == blacklist){
            // Early return. Does not pass the mime filter
            logger.finest("Not handling " + curi.toString()
                    + ", excluded by mimefilter (" + 
                    curi.getContentType() + ").");
            return;
        }
        if(curi.containsKey(A_CONTENT_STATE_KEY) && 
                curi.getInt(A_CONTENT_STATE_KEY)==CONTENT_UNCHANGED){
            // Early return. A previous processor or filter has judged this
            // CrawlURI as having unchanged content.
            logger.finest("Not handling " + curi.toString()
                    + ", already flagged as unchanged.");
            return;
        }
        logger.finest("Processing " + curi.toString() + "(" + 
                curi.getContentType() + ")");

        stats.handledNumber++;
        stats.totalAmount += curi.getContentSize();
        Statistics currHostStats = null;
        if(statsPerHost){
            synchronized (perHostStats) {
                String host = getController().getServerCache()
                            .getHostFor(curi).getHostName();
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
            // Increment statistics counters
            stats.duplicateAmount += curi.getContentSize();
            stats.duplicateNumber++;
            if(statsPerHost){ 
                currHostStats.duplicateAmount+=curi.getContentSize();
                currHostStats.duplicateNumber++;
            }
            // Duplicate. Abort further processing of URI.
            if(((Boolean)readAttribute(
                    ATTR_SKIP_WRITE,
                    DEFAULT_SKIP_WRITE)).booleanValue()){
                // Skip writing, go directly to post processing chain
                curi.skipToProcessorChain(
                        getController().getPostprocessorChain());
            } else {
                // Do not skip writing, go to writer processors
                curi.skipToProcessorChain(
                        getController().getProcessorChainList()
                        .getProcessorChain(CrawlOrder.ATTR_WRITE_PROCESSORS));
            }
            
            // Record origin?
            String annotation = "duplicate";
            if(useOrigin){
                // TODO: Save origin in the CrawlURI so that other processors
                //       can make use of it. (Future: WARC)
                if(useOriginFromIndex && 
                    duplicate.get(DigestIndexer.FIELD_ORIGIN)!=null){
                    // Index contains origin, use it.
                    annotation += ":\"" + duplicate.get(
                            DigestIndexer.FIELD_ORIGIN) + "\""; 
                } else {
                    String tmp = (String)getUncheckedAttribute(curi,ATTR_ORIGIN);
                    // Check if an origin value is actually available
                    if(tmp != null || tmp.trim().length() > 0){
                        // It is available, add it to the log line.
                        annotation += ":\"" + tmp + "\""; 
                    }
                }
            } 
            // Make note in log
            curi.addAnnotation(annotation);

            if(changeContentSize){
                // Set content size to zero, we are not planning to 
                // 'write it to disk'
                // TODO: Reconsider this
                curi.setContentSize(0);
            }
            // Mark as duplicate for other processors
            curi.putInt(A_CONTENT_STATE_KEY, CONTENT_UNCHANGED);
        }
        
        if(doTimestampAnalysis){
            doAnalysis(curi,currHostStats, duplicate!=null);
        }
	}

    /** 
     * Process a CrawlURI looking up in the index by URL
     * @param curi The CrawlURI to process
     * @param currHostStats A statistics object for the current host.
     *                      If per host statistics tracking is enabled this
     *                      must be non null and the method will increment
     *                      appropriate counters on it.
     * @return The result of the lookup (a Lucene document). If a duplicate is
     *         not found null is returned.
     */
    protected Document lookupByURL(CrawlURI curi, Statistics currHostStats){
        // Look the CrawlURI's URL up in the index.
        try {
            Query query = queryField(DigestIndexer.FIELD_URL,
                curi.toString());
            TopDocs topdocs = index.search(query, Integer.MAX_VALUE);
            ScoreDoc[] hits = topdocs.scoreDocs;
            Document doc = null;
            String currentDigest = getDigestAsString(curi);
            if(hits != null && hits.length > 0){
                // Typically there should only be one it, but we'll allow for
                // multiple hits.
                for(int i=0 ; i < hits.length ; i++){
                    // Multiple hits on same exact URL should be rare
                    // See if any have matching content digests
                    int docId = hits[i].doc;
                    doc = index.doc(docId);
                    String oldDigest = doc.get(DigestIndexer.FIELD_DIGEST);

                    if(oldDigest.equalsIgnoreCase(currentDigest)){
                        stats.exactURLDuplicates++;
                        if(statsPerHost){ 
                            currHostStats.exactURLDuplicates++;
                        }
                            
                        logger.finest("Found exact match for " + 
                                curi.toString());
                        
                        // If we found a hit, no need to look at other hits.
                        return doc;
                    }
                }
            } 
            if(equivalent) {
                // No exact hits. Let's try lenient matching.
                String normalizedURL = DigestIndexer.stripURL(curi.toString());
                query = queryField(DigestIndexer.FIELD_URL_NORMALIZED,
                                normalizedURL);
                topdocs = index.search(query, Integer.MAX_VALUE);
                hits = topdocs.scoreDocs;
                for(int i=0 ; i < hits.length ; i++){
                    int docId = hits[i].doc;
                    Document doc1 = index.doc(docId);
                    String indexDigest = doc1.get(DigestIndexer.FIELD_DIGEST);
                    if(indexDigest.equals(currentDigest)){
                        // Make note in log
                        String equivURL = doc1.get(
                                DigestIndexer.FIELD_URL);
                        curi.addAnnotation("equivalent to " + equivURL);
                        // Increment statistics counters
                        stats.equivalentURLDuplicates++;
                        if(statsPerHost){ 
                            currHostStats.equivalentURLDuplicates++;
                        }
                        logger.finest("Found equivalent match for " + 
                                curi.toString() + ". Normalized: " + 
                                normalizedURL + ". Equivalent to: " + equivURL);

                        //If we found a hit, no need to look at more.
                        return doc1;
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE,"Error accessing index.",e);
        }
        // If we make it here then this is not a duplicate.
        return null;
    }
    
    /** 
     * Process a CrawlURI looking up in the index by content digest
     * @param curi The CrawlURI to process
     * @param currHostStats A statistics object for the current host.
     *                      If per host statistics tracking is enabled this
     *                      must be non null and the method will increment
     *                      appropriate counters on it.
     * @return The result of the lookup (a Lucene document). If a duplicate is
     *         not found null is returned.
     */
    protected Document lookupByDigest(CrawlURI curi, Statistics currHostStats) {
        Document duplicate = null; 
        String currentDigest = null;
        Object digest = curi.getContentDigest();
        if (digest != null) {
            currentDigest = Base32.encode((byte[])digest);
        }
        Query query = queryField(DigestIndexer.FIELD_DIGEST, currentDigest);
        try {
            TopDocs topdocs = index.search(query, Integer.MAX_VALUE);
            ScoreDoc[] hits = topdocs.scoreDocs;
            
            StringBuffer mirrors = new StringBuffer();
            mirrors.append("mirrors: ");
            if(hits != null && hits.length > 0){
                // Can definitely be more then one
                // Note: We may find an equivalent match before we find an
                //       (existing) exact match. 
                // TODO: Ensure that an exact match is recorded if it exists.
                for(int i=0 ; i < hits.length && duplicate==null ; i++){
                    int docId = hits[i].doc;
                    Document doc = index.doc(docId);
                    String indexURL = doc.get(DigestIndexer.FIELD_URL);
                    // See if the current hit is an exact match.
                    if(curi.toString().equals(indexURL)){
                        duplicate = doc;
                        stats.exactURLDuplicates++;
                        if(statsPerHost){
                            currHostStats.exactURLDuplicates++;
                        }
                        logger.finest("Found exact match for " + 
                                curi.toString());
                    }
                    
                    // If not, then check if it is an equivalent match (if
                    // equivalent matches are allowed).
                    if(duplicate == null && equivalent){
                        String normalURL = 
                            DigestIndexer.stripURL(curi.toString());
                        String indexNormalURL = 
                            doc.get(
                                    DigestIndexer.FIELD_URL_NORMALIZED);
                        if(normalURL.equals(indexNormalURL)){
                            duplicate = doc;
                            stats.equivalentURLDuplicates++;
                            if(statsPerHost){
                                currHostStats.equivalentURLDuplicates++;
                            }
                            curi.addAnnotation("equivalent to " + indexURL);
                            logger.finest("Found equivalent match for " + 
                                    curi.toString() + ". Normalized: " + 
                                    normalURL + ". Equivalent to: " + indexURL);
                        }
                    }
                    
                    if(duplicate == null){
                        // Will only be used if no exact (or equivalent) match
                        // is found.
                        mirrors.append(indexURL + " ");
                    }
                }
                if(duplicate == null){
                    stats.mirrorNumber++;
                    if (statsPerHost) {
                        currHostStats.mirrorNumber++;
                    }
                    logger.log(Level.FINEST,"Found mirror URLs for " + 
                            curi.toString() + ". " + mirrors);
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE,"Error accessing index.",e);
        }
        return duplicate;
    }
    
	public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: is.hi.bok.digest.DeDuplicator\n");
        ret.append("  Function:          Abort processing of duplicate records\n");
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
        
        if(doTimestampAnalysis){
        	ret.append("  Timestamp predicts: (Where exact URL existed in the index)\n");
        	ret.append("  Change correctly:  " + stats.timestampChangeCorrect + "\n");
        	ret.append("  Change falsly:     " + stats.timestampChangeFalse + "\n");
        	ret.append("  Non-change correct:" + stats.timestampNoChangeCorrect + "\n");
        	ret.append("  Non-change falsly: " + stats.timestampNoChangeFalse + "\n");
        	ret.append("  Missing timpestamp:" + stats.timestampMissing + "\n");
        	
        }
        
        if(statsPerHost){
            ret.append("  [Host] [total] [duplicates] [bytes] " +
                    "[bytes discarded] [new] [exact] [equiv]");
            if(lookupByURL==false){
                ret.append(" [mirror]");
            }
            if(doTimestampAnalysis){
                ret.append(" [change correct] [change falsly]");
                ret.append(" [non-change correct] [non-change falsly]");
                ret.append(" [no timestamp]\n");
            }
            synchronized (perHostStats) {
                Iterator<String> it = perHostStats.keySet().iterator();
                while(it.hasNext()){
                    String key = (String)it.next();
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
                    if(doTimestampAnalysis){
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
	        Query query = queryField(DigestIndexer.FIELD_URL,
	                curi.toString());
	        TopDocs topdocs = index.search(query, Integer.MAX_VALUE);
	        ScoreDoc[] hits = topdocs.scoreDocs;
	        Document doc = null;
	        if(hits != null && hits.length > 0){
	            // If there are multiple hits, use the one with the most
	            // recent date.
	            Document docToEval = null;
	            for(int i=0 ; i < hits.length ; i++){
	                int docId = hits[i].doc;
	                doc = index.doc(docId);
	                // The format of the timestamp ("yyyyMMddHHmmssSSS") allows
	                // us to do a greater then (later) or lesser than (earlier)
	                // comparison of the strings.
	                String timestamp = doc.get(DigestIndexer.FIELD_TIMESTAMP);
	                if(docToEval == null 
	                        || docToEval.get(DigestIndexer.FIELD_TIMESTAMP)
	                        .compareTo(timestamp)>0){
	                    // Found a more recent hit.
	                    docToEval = doc;
	                }
	            }
	            doTimestampAnalysis(curi,docToEval, currHostStats, isDuplicate);
	            if(doETagAnalysis){
	                // TODO: Do etag analysis
	            }
	        }
	    } catch(IOException e){
	        logger.log(Level.SEVERE,"Error accessing index.",e);
	    }
	}
	
	protected void doTimestampAnalysis(CrawlURI curi, Document urlHit, 
            Statistics currHostStats, boolean isDuplicate){
        
        HttpMethod method = (HttpMethod)curi.getObject(
                CoreAttributeConstants.A_HTTP_TRANSACTION);

        // Compare datestamps (last-modified versus the indexed date)
        Date lastModified = null;
        if (method.getResponseHeader("last-modified") != null) {
            SimpleDateFormat sdf = 
                    new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", 
                            Locale.ENGLISH);
            try {
                lastModified = sdf.parse(
                        method.getResponseHeader("last-modified").getValue());
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
     * @returns A Query for the given value in the given field.
     */
    protected Query queryField(String fieldName, String value) {
        Query query = null;
//        if(useSparseRangeFilter){
//        	query = new ConstantScoreQuery(
//                new SparseRangeFilter(fieldName, value, value, true, true));
//        } else {
//        	query = new ConstantScoreQuery(
//                new RangeFilter(fieldName, value, value, true, true);//);
//        }
        query = new ConstantScoreQuery(new FieldCacheTermsFilter(fieldName,
                value));
        return query;
    }

	
    protected void finalTasks() {
        try {
            if (index != null) {
                index.close();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE,"Error closing index",e);
        }
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
