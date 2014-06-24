package dk.netarkivet.systemtest;

public interface HarvestingEventListener {
    void harvestStarted();
    void harvestFinished();
    void harvestFailed();
}
