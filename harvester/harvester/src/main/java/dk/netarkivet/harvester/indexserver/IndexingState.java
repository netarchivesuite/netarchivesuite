package dk.netarkivet.harvester.indexserver;

import java.util.concurrent.Future;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/** Stores the state of a indexing task. */
public class IndexingState {
    
    /** The Id of the job being indexed. */
    private final Long jobIdentifier;
    /**  The full path to the index. */
    private final String index;
    /** The result object for the indexing task. */
    private final Future<Boolean> resultObject;

    /**
     * Constructor for an IndexingState object.
     * @param jobId The ID of the Job being indexing.
     * @param indexingpath The full path to the index.
     * @param result The result object for the indexing task
     */
    public IndexingState(Long jobId, String indexingpath, 
            Future<Boolean> result) {
        ArgumentNotValid.checkNotNull(jobId, "Long jobId");
        ArgumentNotValid.checkNotNullOrEmpty(
                indexingpath, "String indexingpath");
        ArgumentNotValid.checkNotNull(result, "Future<Boolean> result");
        this.jobIdentifier = jobId;
        this.index = indexingpath;
        this.resultObject = result;
    }
    
    /**
     * 
     * @return the Id of the job being indexed.
     */
    public Long getJobIdentifier() {
        return jobIdentifier;
    }
    
    /**
     * 
     * @return the full path to the index generated.
     */
    public String getIndex() {
        return index;
    }
    
    /**
     * 
     * @return the result of the indexing process.
     */
    public Future<Boolean> getResultObject() {
        return resultObject;
    }
    
    public String toString() {
        return "IndexingState for JobID #" + jobIdentifier 
                + ": (index = " + index + ", IndexingDone = " 
                + resultObject.isDone() + ")";
    }
}
