package is.hi.bok.deduplicator;

/**
 * Lifted from H1 AdaptiveRevisitAttributeConstants and limited to what DeDuplicator was using.
 * 
 *
 */
public interface DedupAttributeConstants {
    
    
    /** No knowledge of URI content. Possibly not fetched yet, unable
     *  to check if different or an error occurred on last fetch attempt. */
    public static final int CONTENT_UNKNOWN = -1;
    
    /** URI content has not changed between the two latest, successfully
     *  completed fetches. */
    public static final int CONTENT_UNCHANGED = 0;
    
    /** URI content had changed between the two latest, successfully completed
     *  fetches. By definition, content has changed if there has only been one
     *  successful fetch made. */
    public static final int CONTENT_CHANGED = 1;

    /**
     * Key to use getting state of crawluri from the CrawlURI data.
     */
    public static final String A_CONTENT_STATE_KEY = "revisit-state";
}
