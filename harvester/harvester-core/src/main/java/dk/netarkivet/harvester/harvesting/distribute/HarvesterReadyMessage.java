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
package dk.netarkivet.harvester.harvesting.distribute;

import java.io.Serializable;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.utils.SystemUtils;
import dk.netarkivet.harvester.distribute.HarvesterChannels;
import dk.netarkivet.harvester.distribute.HarvesterMessage;
import dk.netarkivet.harvester.distribute.HarvesterMessageVisitor;

/**
 * The HarvestControllerServer periodically sends {@link HarvesterReadyMessage}s to the JobDispatcher to
 * notify it whether it is available for processing a job or already processing one.
 */
@SuppressWarnings({"serial"})
public class HarvesterReadyMessage extends HarvesterMessage implements Serializable {

    /** The name of the channel of jobs crawled by the sender. */
    private final String harvestChannelName;

    /** The sender's application instance ID. */
    private final String applicationInstanceId;

    /** The host of the sender. */
    private final String hostName;
    
    /**
     * Builds a new message.
     *
     * @param harvestChannelName the channel of jobs crawled by the sender.
     * @param applicationInstanceId the sender's application instance ID.
     */
    public HarvesterReadyMessage(String applicationInstanceId, String harvestChannelName) {
        super(HarvesterChannels.getHarvesterStatusChannel(), Channels.getError());
        this.applicationInstanceId = applicationInstanceId;
        this.harvestChannelName = harvestChannelName;
        this.hostName = SystemUtils.getLocalHostName();
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

    /**
     * @return the hostname of the sender. 
     */
    public String getHostName() {
    	return hostName;
    }
    
    
    
}
