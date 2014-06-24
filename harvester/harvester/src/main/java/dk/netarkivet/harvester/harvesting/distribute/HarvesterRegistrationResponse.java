package dk.netarkivet.harvester.harvesting.distribute;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.distribute.HarvesterChannels;
import dk.netarkivet.harvester.distribute.HarvesterMessage;
import dk.netarkivet.harvester.distribute.HarvesterMessageVisitor;
import dk.netarkivet.harvester.scheduler.HarvesterStatusReceiver;

/**
 * Message sent by the {@link HarvesterStatusReceiver} after processing a
 * {@link HarvesterRegistrationRequest} message. It notifies crawlers
 * whether a given harvest channel effectively matches a {@link HarvestChannel}
 * defined in the harvest database.
 *
 */
@SuppressWarnings({ "serial"})
public class HarvesterRegistrationResponse extends HarvesterMessage {

    /**
     * The harvest channel name.
     */
    private final String harvestChannelName;

    /**
     * If true, the name matches an existing {@link HarvestChannel}.
     */
    private final boolean isValid;

    /**
     * Whether the matching {@link HarvestChannel} handles snapshot or focused harvests.
     * Meaningless if {@link #isValid} is false.
     */
    private final boolean isSnapshot;

    /**
     * Constructor from fields.
     * @param harvestChannelName the harvest channel name
     * @param isValid whether the given name denotes an existing channel
     * @param isSnapshot true if the channel accepts snapshot harvest, false for partial.
     *
     */
    public HarvesterRegistrationResponse(
            final String harvestChannelName,
            final boolean isValid,
            final boolean isSnapshot) {
        super(HarvesterChannels.getHarvesterRegistrationResponseChannel(), Channels.getError());
        this.harvestChannelName = harvestChannelName;
        this.isValid = isValid;
        this.isSnapshot = isSnapshot;
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
     * @return the isValid
     */
    public final boolean isValid() {
        return isValid;
    }

    /**
     * @return the isSnapshot
     */
    public final boolean isSnapshot() {
        return isSnapshot;
    }

}
