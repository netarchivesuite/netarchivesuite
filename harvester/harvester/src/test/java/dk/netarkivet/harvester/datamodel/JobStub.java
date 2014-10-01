package dk.netarkivet.harvester.datamodel;

/**
 * Provides functionality for creating a empty job for test purposes. Also provides missing setter methods
 * or overrides more complicated ones.
 */
public class JobStub extends Job {

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public void setOrigHarvestDefinitionID(Long origHarvestDefinitionID) {
        this.origHarvestDefinitionID = origHarvestDefinitionID;
    }
}
