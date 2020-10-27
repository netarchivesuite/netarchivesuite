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

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.utils.SystemUtils;
import dk.netarkivet.harvester.distribute.HarvesterChannels;
import dk.netarkivet.harvester.distribute.HarvesterMessage;
import dk.netarkivet.harvester.distribute.HarvesterMessageVisitor;
import dk.netarkivet.harvester.harvesting.monitor.HarvestMonitor;

/**
 * Message sent by a HarvestController at startup, to check if the channel name it has been assigned is valid
 * (e.g. registered in the harvest database).
 * <p>
 * The message is sent on a dedicated queue, and processed by the {@link HarvestMonitor}, which checks if the channel
 * name matches a channel defined in the harvest database.
 * <p>
 * In reply a {@link HarvesterRegistrationResponse} is sent back.
 *
 * @author ngiraud
 */
@SuppressWarnings({"serial"})
public class HarvesterRegistrationRequest extends HarvesterMessage {

    /** The harvest channel name to check. */
    private final String harvestChannelName;

    private final String instanceId;
    
    private final String hostname;

    public HarvesterRegistrationRequest(final String harvestChannelName, final String instanceId) {
        super(HarvesterChannels.getHarvesterRegistrationRequestChannel(), Channels.getError());
        this.harvestChannelName = harvestChannelName;
        this.instanceId = instanceId;
        this.hostname = SystemUtils.getLocalHostName();
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

    /**
     * @return the hostname of the sender
     */
    public final String getHostname() {
        return hostname;
    }
    
}
