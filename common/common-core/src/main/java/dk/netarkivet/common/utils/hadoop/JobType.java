package dk.netarkivet.common.utils.hadoop;

/**
 * Enum specifying which type of Hadoop job is being run - mostly for logging purposes.
 */
public enum JobType {
    STANDARD_CDX,
    METADATA_CDX,
    CRAWL_LOG_EXTRACTION;

    public String toString() {
        switch(this) {
        case STANDARD_CDX: return "standard CDX";
        case METADATA_CDX: return "metadata CDX";
        case CRAWL_LOG_EXTRACTION: return "crawl log extraction";
        default: return ""; // TODO what do?
        }
    }
}