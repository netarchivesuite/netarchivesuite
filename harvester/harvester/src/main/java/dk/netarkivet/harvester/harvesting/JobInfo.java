package dk.netarkivet.harvester.harvesting;

/**
 * Interface for selecting partial job information necessary for constructing
 * HeritrixFiles. 
 */
public interface JobInfo {
   
    /**
     * Get the job ID belonging to a job.
     * @return The job ID belonging to a job.
     */
    Long getJobID();
    
    /**
     * Get the harvest ID belonging to a job.
     * @return The harvest ID belonging to a job.
     */
    Long getOrigHarvestDefinitionID();
   
    /**
     * Get the harvestFilename prefix.
     * @return the harvestFilename prefix.
     */
     String getHarvestFilenamePrefix();
}
