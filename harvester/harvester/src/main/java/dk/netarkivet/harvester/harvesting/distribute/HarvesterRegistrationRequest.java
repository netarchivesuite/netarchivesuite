package dk.netarkivet.harvester.harvesting.distribute;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.harvester.distribute.HarvesterChannels;
import dk.netarkivet.harvester.distribute.HarvesterMessage;
import dk.netarkivet.harvester.distribute.HarvesterMessageVisitor;
import dk.netarkivet.harvester.harvesting.HarvestController;
import dk.netarkivet.harvester.harvesting.monitor.HarvestMonitor;

/**
 * Message sent by a {@link HarvestController} at startup, to check if the channel name
 * it has been assigned is valid (e.g. registered in the harvest database).
 *
 * The message is sent on a dedicated queue, and processed by 
 * the {@link HarvestMonitor}, which checks if the channel name matches a channel defined in 
 * the harvest database.
 *
 * In reply a {@link HarvesterRegistrationResponse} is sent back.
 *
 * @author ngiraud
 *
 */
@SuppressWarnings({ "serial"})
public class HarvesterRegistrationRequest extends HarvesterMessage {

    /**
     * The harvest channel name to check.
     */
    private final String harvestChannelName;
    
    private final String instanceId;

    public HarvesterRegistrationRequest(
    		final String harvestChannelName,
    		final String instanceId) {
        super(HarvesterChannels.getHarvesterRegistrationRequestChannel(), Channels.getError());
        this.harvestChannelName = harvestChannelName;
        this.instanceId = instanceId;
    }

    @Override
    public void accept(HarvesterMessageVisitor v) {
        v.visit(this);
    }

    /**
     * @return the harvestChannelName
     */
    public final String getHarvestChannelName() {
        return harvestChannelName;
    }

	/**
	 * @return the instanceId
	 */
	public final String getInstanceId() {
		return instanceId;
	}

}
