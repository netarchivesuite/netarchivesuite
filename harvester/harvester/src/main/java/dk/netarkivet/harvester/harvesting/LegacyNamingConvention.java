package dk.netarkivet.harvester.harvesting;

import dk.netarkivet.harvester.datamodel.Job;

/** 
 * Implements the standard way of prefixing archive files in Netarchivesuite. 
 * I.e. jobid-harvestid
 */
public class LegacyNamingConvention implements ArchiveFileNaming {
    
    public LegacyNamingConvention() {
    }

    @Override
    public String getPrefix(Job theJob) {
        return theJob.getJobID() + "-" + theJob.getOrigHarvestDefinitionID();
    }
    
}
