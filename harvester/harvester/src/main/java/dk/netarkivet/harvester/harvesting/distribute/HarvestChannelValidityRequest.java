/* File:       $Id: HarvestChannelValidityRequest.java 2251 2012-02-08 13:03:03Z mss $
 * Revision:   $Revision: 2251 $
 * Author:     $Author: mss $
 * Date:       $Date: 2012-02-08 14:03:03 +0100 (Wed, 08 Feb 2012) $
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
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
 * In reply a {@link HarvestChannelValidityResponse} is sent back.
 *
 * @author ngiraud
 *
 */
public class HarvestChannelValidityRequest extends HarvesterMessage {

    /**
     * The harvest channel name to check.
     */
    private final String harvestChannelName;
    
    private final String instanceId;

    public HarvestChannelValidityRequest(
    		final String harvestChannelName,
    		final String instanceId) {
        super(HarvesterChannels.getHarvestChannelValidityRequestChannel(), Channels.getError());
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
