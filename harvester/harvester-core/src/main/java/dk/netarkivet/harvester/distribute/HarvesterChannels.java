/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package dk.netarkivet.harvester.distribute;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.harvester.datamodel.HarvestChannel;

/**
 * This singleton class is in charge of giving out the correct channels.
 */

public class HarvesterChannels {

    private static final String THESCHED_CHANNEL_PREFIX = "THE_SCHED";
    /**
     * Prefix for the channel used to send CrawlProgressMessages.
     */
    private static final String HARVEST_MONITOR_CHANNEL_PREFIX = "HARVESTMON";

    /**
     * Prefix for the channel used to sending HarvesterReadyMessages.
     */
    private static final String HARVESTER_STATUS_CHANNEL_PREFIX = "HARVESTER_STATUS";
    /**
     * The one existing instance of the Channels object. Not accessible from the outside at all.
     */
    private static HarvesterChannels instance;

    /**
     * Accessor for singleton internally.
     *
     * @return the <code>Channels</code> object for this singleton.
     */
    private static HarvesterChannels getInstance() {
        if (instance == null) {
            instance = new HarvesterChannels();
        }
        return instance;
    }

    /**
     * @return the <code>ChannelID</code> for the queue on which HarvestControllers send harvest status messages
     */
    public static ChannelID getTheSched() {
        return getInstance().THE_SCHED;
    }

    public final ChannelID THE_SCHED = new ChannelID(THESCHED_CHANNEL_PREFIX, ChannelID.COMMON, ChannelID.NO_IP,
            ChannelID.NO_APPLINST_ID, ChannelID.QUEUE);

    /**
     * Return the queue for the harvest monitor registry.
     *
     * @return the <code>ChannelID</code> object for the queue.
     */
    public static ChannelID getHarvestMonitorChannel() {
        return getInstance().HARVEST_MONITOR;
    }

    private final ChannelID HARVEST_MONITOR = new ChannelID(HARVEST_MONITOR_CHANNEL_PREFIX, ChannelID.COMMON,
            ChannelID.NO_IP, ChannelID.NO_APPLINST_ID, ChannelID.QUEUE);

    /**
     * @return the <code>ChannelID</code> object for the topic used by the harvesters to call in ready for new jobs.
     */
    public static ChannelID getHarvesterStatusChannel() {
        return getInstance().HARVESTER_STATUS;
    }

    private final ChannelID HARVESTER_STATUS = new ChannelID(HARVESTER_STATUS_CHANNEL_PREFIX, ChannelID.COMMON,
            ChannelID.NO_IP, ChannelID.NO_APPLINST_ID, ChannelID.TOPIC);

    /**
     * Prefix for the channels used to send
     * {@link dk.netarkivet.harvester.harvesting.distribute.HarvesterRegistrationRequest}s and
     * {@link dk.netarkivet.harvester.harvesting.distribute.HarvesterRegistrationResponse}s
     */
    private static final String HARVEST_CHANNEL_VALIDITY_PREFIX = "HCHAN_VAL_";

    private final ChannelID HARVEST_CHANNEL_VALIDITY_REQUEST = new ChannelID(HARVEST_CHANNEL_VALIDITY_PREFIX + "REQ",
            ChannelID.COMMON, ChannelID.NO_IP, ChannelID.NO_APPLINST_ID, ChannelID.QUEUE);

    /**
     * Return the queue for sending {@link dk.netarkivet.harvester.harvesting.distribute.HarvesterRegistrationRequest}s.
     *
     * @return the <code>ChannelID</code> object for the queue.
     */
    public static ChannelID getHarvesterRegistrationRequestChannel() {
        return getInstance().HARVEST_CHANNEL_VALIDITY_REQUEST;
    }

    /**
     * Prefix for channels related to harvest channel validity messages.
     */
    private final ChannelID HARVEST_CHANNEL_VALIDITY_RESPONSE = new ChannelID(HARVEST_CHANNEL_VALIDITY_PREFIX + "RESP",
            ChannelID.COMMON, ChannelID.NO_IP, ChannelID.NO_APPLINST_ID, ChannelID.QUEUE);

    /**
     * Return the queue for sending {@link dk.netarkivet.harvester.harvesting.distribute.HarvesterRegistrationResponse}
     * s.
     *
     * @return the <code>ChannelID</code> object for the queue.
     */
    public static ChannelID getHarvesterRegistrationResponseChannel() {
        return getInstance().HARVEST_CHANNEL_VALIDITY_RESPONSE;
    }

    /**
     * The prefix for channels handling snapshot harvest jobs.
     */
    private static final String JOB_SNAPSHOT_CHANNEL_PREFIX = "JOB_SNAPSHOT";

    /**
     * The prefix for channels handling focused harvest jobs.
     */
    private static final String JOB_PARTIAL_CHANNEL_PREFIX = "JOB_PARTIAL";

    /**
     * Returns the queue which is used by the scheduler to send doOneCrawl to Harvest Controllers listening on the given
     * harvest channel.
     * @param harvestChannel
     * 
     * @return That channel (queue)
     */
    public static ChannelID getHarvestJobChannelId(HarvestChannel harvestChannel) {
        return getHarvestJobChannelId(harvestChannel.getName(),harvestChannel.isSnapshot());
    }

    /**
     * Returns the queue which is used by the scheduler to send doOneCrawl to Harvest Controllers listening on the given
     * harvest channel.
     *
     * @param harvestChannelName
     * @param isSnapshot
     *
     * @return That channel (queue)
     */
    public static ChannelID getHarvestJobChannelId(String harvestChannelName, boolean isSnapshot) {
        String prefix = (isSnapshot ? JOB_SNAPSHOT_CHANNEL_PREFIX : JOB_PARTIAL_CHANNEL_PREFIX) + "_"
                + harvestChannelName.toUpperCase();
        return new ChannelID(prefix, ChannelID.COMMON, ChannelID.NO_IP, ChannelID.NO_APPLINST_ID, ChannelID.QUEUE);
    }
    
}
