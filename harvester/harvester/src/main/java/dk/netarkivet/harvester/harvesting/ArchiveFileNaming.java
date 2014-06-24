package dk.netarkivet.harvester.harvesting;

import dk.netarkivet.harvester.datamodel.Job;

/**
 * Interface for a class that implement archiveFileNaming. This is only used to override
 * the 'prefix' property of the ARC/WARCWriterprocessors. 
 */
public interface ArchiveFileNaming {

    /**
     * Make a prefix to be used by Heritrix. It is optional what information 
     * from the harvestjob if any is going to be part of the prefix.
     * 
     * @param job the harvestJob
     * @return a prefix to be used by Heritrix
     */
    String getPrefix(Job job);    
}
