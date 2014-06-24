package dk.netarkivet.harvester.harvesting;

public abstract class ArchiveFilenameParser {
    
    /** @return the harvestID. */
    public abstract String getHarvestID();
    
    /** 
     * Get the job ID.
     * @return the Job ID.
     */
    public abstract String getJobID();
    
    /** 
     * Get the timestamp.
     * @return the timestamp.
     */
    public abstract String getTimeStamp();
    
    /** 
     * Get the serial number.
     * @return the serial number.
     */
    public abstract String getSerialNo();
    
    /** 
     * Get the filename.
     * @return the filename.
     */
    public abstract String getFilename();
}
