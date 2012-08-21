package dk.netarkivet.harvester.harvesting;

public class JobInfoTestImpl implements JobInfo {

    private Long jobId;
    private Long harvestId;

    public JobInfoTestImpl(Long jobId, Long harvestId) {
        this.jobId = jobId;
        this.harvestId = harvestId;
    }
    
    @Override
    public Long getJobID() {
        return jobId;
    }

    @Override
    public Long getOrigHarvestDefinitionID() {
        return this.harvestId;
    }

    @Override
    public String getHarvestNamePrefix() {
        return jobId + "-" + harvestId;
    }
    
}
