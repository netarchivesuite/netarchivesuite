package is.hi.bok.deduplicator;

public class DedupStatistics { 

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

