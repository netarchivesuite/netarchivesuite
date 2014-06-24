package dk.netarkivet.harvester.harvesting.distribute;

import java.io.Serializable;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.harvester.distribute.HarvesterChannels;
import dk.netarkivet.harvester.distribute.HarvesterMessage;
import dk.netarkivet.harvester.distribute.HarvesterMessageVisitor;
import dk.netarkivet.harvester.scheduler.JobDispatcher;

/**
 * The {@link HarvestControllerServer} periodically sends 
 * {@link HarvesterReadyMessage}s to the {@link JobDispatcher} to notify
 * it whether it is available for processing a job or already processing one.
 */
@SuppressWarnings({ "serial"})
public class HarvesterReadyMessage
        extends HarvesterMessage
        implements Serializable {

    /**
     * The name of the channel of jobs crawled by the sender.
     */
    private final String harvestChannelName;

    /**
     * The sender's application instance ID.
     */
    private final String applicationInstanceId;


    /**
     * Builds a new message.
     * @param harvestChannelName the channel of jobs crawled by the sender.
     * @param applicationInstanceId the sender's application instance ID.
     */
    public HarvesterReadyMessage(
            String applicationInstanceId,
            String harvestChannelName) {
        super(HarvesterChannels.getHarvesterStatusChannel(), Channels.getError());
        this.applicationInstanceId = applicationInstanceId;
        this.harvestChannelName = harvestChannelName;
    }

    @Override
    public void accept(HarvesterMessageVisitor v) {
        v.visit(this);
    }

    /**
     * @return the associated harvest channel name
     */
    public String getHarvestChannelName() {
        return harvestChannelName;
    }

    /**
     * @return the application instance ID.
     */
    public String getApplicationInstanceId() {
        return applicationInstanceId;
    }
}
