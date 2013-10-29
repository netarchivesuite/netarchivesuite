/* File:       $Id: HarvestChannelValidityResponse.java 2251 2012-02-08 13:03:03Z mss $
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
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.distribute.HarvesterMessage;
import dk.netarkivet.harvester.distribute.HarvesterMessageVisitor;
import dk.netarkivet.harvester.scheduler.HarvesterStatusReceiver;

/**
 * Message sent by the {@link HarvesterStatusReceiver} after processing a
 * {@link HarvestChannelValidityRequest} message. It notifies crawlers
 * whether a given harvest channel effectively matches a {@link HarvestChannel}
 * defined in the harvest database.
 *
 */
public class HarvestChannelValidityResponse extends HarvesterMessage {
	
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
	 */
	public HarvestChannelValidityResponse(
			final String harvestChannelName, 
			final boolean isValid, 
			final boolean isSnapshot) {
		super(Channels.getHarvestChannelValidityResponseChannel(), Channels.getError());
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
